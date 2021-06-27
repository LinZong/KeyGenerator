package moe.nemesiss.keygenerator.loadbalancer.repo

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.TypeReference
import moe.nemesiss.keygenerator.loadbalancer.Epoch
import moe.nemesiss.keygenerator.loadbalancer.KeyGeneratorGroup
import moe.nemesiss.keygenerator.loadbalancer.KeyGeneratorGroupMeta
import moe.nemesiss.keygenerator.loadbalancer.util.SafeUpperRoundRobinClientSelector
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.moveTo

private val GroupMetaTypeRef = object : TypeReference<Map<String, KeyGeneratorGroupMeta>>() {}

class GroupMetaRepository(val dataDir: File) {
    private val tmpEpochMetaFile = File(dataDir, "epoch.meta.tmp")

    private val epochMetaFile = File(dataDir, "epoch.meta")

    private val tmpSelectorMetaFile = File(dataDir, "rr.meta.tmp")

    private val selectorMetaFile = File(dataDir, "rr.meta")

    fun ensureDataDirCreated() {
        if (!(dataDir.exists() || dataDir.mkdirs())) {
            throw IllegalStateException("data dir is not created!")
        }
    }

    @Synchronized
    fun loadEpoch(initGroup: Boolean): Epoch {
        if (initGroup) {
            val epoch = Epoch(0, 0, 1)
            saveEpoch(epoch)
            return epoch
        }
        if (tmpEpochMetaFile.exists()) {
            // 说明有中断的重平衡，此时应回滚到重平衡前的状态。
            tmpEpochMetaFile.delete()
        }
        if (!epochMetaFile.exists()) {
            throw IllegalStateException("epoch meta file NOT FOUND!")
        }
        return JSON.parseObject(epochMetaFile.readText(), Epoch::class.java)
    }

    @Synchronized
    fun saveEpoch(epoch: Epoch) {
        tmpEpochMetaFile.writeText(JSON.toJSONString(epoch))
        tmpEpochMetaFile.toPath().moveTo(epochMetaFile.toPath(), true)
    }

    @Synchronized
    fun saveSelector(rr: SafeUpperRoundRobinClientSelector) {
        val json = JSONObject()
        json["round"] = rr.round
        json["maxRound"] = rr.maxRound
        json["maxRoundDelta"] = rr.maxRoundDelta
        tmpSelectorMetaFile.writeText(JSON.toJSONString(json))
        tmpSelectorMetaFile.toPath().moveTo(selectorMetaFile.toPath(), true)
    }

    @Synchronized
    fun loadSelector(initGroup: Boolean): SafeUpperRoundRobinClientSelector {

        if (initGroup) {
            // 删掉tmpFile和file
            (tmpSelectorMetaFile.exists() && tmpSelectorMetaFile.delete())
            (selectorMetaFile.exists() && selectorMetaFile.delete())

            val instance = SafeUpperRoundRobinClientSelector(
                this,
                emptyList(),
                0,
                0,
                3
            )

            saveSelector(instance) // 先写盘
            return instance
        }

        // 1. 先看有没有来不及rename的数据
        if (tmpSelectorMetaFile.exists()) {
            // 存在未完成的next round动作, 尝试读取
            val tmpJson = tryReadTmpMetaFile()

            if (tmpJson != null) {
                // 成功读取，开始恢复数据
                val round = tmpJson.getIntValue("round")
                val maxRound = tmpJson.getIntValue("maxRound")
                val maxRoundDelta = tmpJson.getIntValue("maxRoundDelta")
                tmpSelectorMetaFile.toPath().moveTo(selectorMetaFile.toPath(), true)
                return SafeUpperRoundRobinClientSelector(this, emptyList(), round, maxRound, maxRoundDelta)
            }

            // 读取失败，但是现在已经进行到round递增阶段
            // 有两种可能, 1 现在在进行重平衡的重设RR阶段, 但是重平衡的重设RR一定在递增epoch之前，
            // 那直接读老数据，假装重平衡没有发生过就好了。不过读老数据面临的问题是你不知道当前round是多少。
            // 2 没有重平衡，只是递增round，触顶了delta，需要写盘新的round和maxround而已。但是不写新的round下去也不知道当前round是多少。
            // 解决此问题的办法是将当前round 设置等于 maxround，这是一个绝对安全的取值方法。因为真正的round距离maxround一定有一个delta量的间隔。
            // 所以下一次重平衡的时候一定能够给出一个绝对安全的起始值，可以大于所有已分配的值。
        }

        if (selectorMetaFile.exists()) {
            // 读老数据
            val json = JSON.parseObject(selectorMetaFile.readText())
            // val round = json.getIntValue("round") no used.
            val maxRound = json.getIntValue("maxRound")
            val maxRoundDelta = json.getIntValue("maxRoundDelta")
            return SafeUpperRoundRobinClientSelector(
                this,
                emptyList(),
                maxRound,
                /** ↑ use max round instead of round **/
                maxRound,
                maxRoundDelta
            )
        }

        // 老数据读不到，抛异常
        throw IllegalStateException("metadata for round robin selector NOT FOUND!")
    }

    private fun tryReadTmpMetaFile(): JSONObject? {
        return try {
            JSON.parseObject(tmpSelectorMetaFile.readText())
        } catch (e: Throwable) {
            null
        }
    }
}

class MetaRepository(val dataDir: File) {

    private val log = KotlinLogging.logger("MetaRepository")

    private val tmpCoordinatorMetaFile = File(dataDir, "group.meta.tmp")

    private val coordinatorMetaFile = File(dataDir, "group.meta")

    fun getGroupRepository(namespace: String): GroupMetaRepository {
        val workDir = File(dataDir, namespace)
        return GroupMetaRepository(workDir)
    }

    @Synchronized
    fun loadGroups(): ConcurrentHashMap<String, KeyGeneratorGroup> {
        if (tmpCoordinatorMetaFile.exists()) {
            try {
                val result = parseGroupMeta(tmpCoordinatorMetaFile.readText())
                // 文件是好的，可以Rename。
                tmpCoordinatorMetaFile.toPath().moveTo(coordinatorMetaFile.toPath(), true)
                return result
            } catch (e: Throwable) {
                // tmp file is corrupted.
            }
        }
        // 读正常文件
        if (!coordinatorMetaFile.exists()) {
            log.warn { "Coordinator Meta File NOT FOUND! Load an empty one." }
            return ConcurrentHashMap<String, KeyGeneratorGroup>()
        }
        return parseGroupMeta(coordinatorMetaFile.readText())
    }

    private fun parseGroupMeta(meta: String): ConcurrentHashMap<String, KeyGeneratorGroup> {
        val json = JSON.parseObject(meta, GroupMetaTypeRef)
        return ConcurrentHashMap(json.mapValues { e ->
            val workRepo = getGroupRepository(e.key)
            if (!workRepo.dataDir.exists()) {
                throw IllegalStateException("workdir for namespace ${e.key} doesn't exists!")
            }
            KeyGeneratorGroup(workRepo, e.value.namespace, e.value.groupId)
        })
    }

    @Synchronized
    fun saveGroups(groups: Map<String, KeyGeneratorGroup>) {
        val groupMetas = groups.mapValues { e -> e.value.meta }
        tmpCoordinatorMetaFile.writeText(JSON.toJSONString(groupMetas))
        tmpCoordinatorMetaFile.toPath().moveTo(coordinatorMetaFile.toPath(), true)
    }
}