package moe.nemesiss.keygenerator.service.repo

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import moe.nemesiss.keygenerator.service.ClusterInfo
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class ClusterInfoRepository(dataDir: File) {

    private val file = File(dataDir, "clusterinfo")
    private val log = KotlinLogging.logger("ClusterInfoRepository")

    @Synchronized
    fun save(info: ClusterInfo) {
        val json = JSONObject()
        json["epoch"] = info.epoch
        json["step"] = info.step
        json["groupId"] = info.groupId
        BufferedWriter(FileWriter(file))
            .use { bw -> bw.write(JSON.toJSONString(json)) }
    }

    @Synchronized
    fun load(): ClusterInfo {
        if (!file.exists()) {
            log.warn { "${file.absolutePath} is not exists. load a fresh ClusterInfo." }
            return ClusterInfo(this)
        }
        try {
            val json = JSON.parseObject(file.readText())
            val info = ClusterInfo(this)
            json.getInteger("epoch")?.let { epoch -> info.epoch = epoch }
            json.getInteger("step")?.let { step -> info.step = step }
            json.getString("groupId")?.let { groupId -> info.groupId = groupId }
            return info
        } catch (e: Throwable) {
            log.warn { "${file.absolutePath} is broken. load a fresh ClusterInfo instead." }
            return ClusterInfo(this)
        }
    }
}