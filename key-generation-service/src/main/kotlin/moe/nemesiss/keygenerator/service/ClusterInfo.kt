package moe.nemesiss.keygenerator.service

import moe.nemesiss.keygenerator.base.constant.NodeState
import moe.nemesiss.keygenerator.base.constant.NodeState.*
import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.service.repo.ClusterInfoRepository
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class ClusterInfo(private val repository: ClusterInfoRepository, val enablePersistence: Boolean = true) {

    // locker
    private val locker = ReentrantReadWriteLock()
    private val readLock = locker.readLock()
    private val writeLock = locker.writeLock()

    private var batchMode = false

    @Volatile
    var epoch = 0
        get() = readLock.withLock { field }
        set(value) = writeLock.withLock { field = value; writeDisk() }

    @Volatile
    var state = STARTING
        get() = readLock.withLock { field }
        set(value) = writeLock.withLock { stateChecker(field, value); field = value; writeDisk() }

    @Volatile
    var step = 1
        get() = readLock.withLock { field }
        set(value) = writeLock.withLock { field = value; writeDisk() }

    @Volatile
    var groupId = ""
        get() = readLock.withLock { field }
        set(value) = writeLock.withLock { field = value; writeDisk() }

    fun update(joinResult: JoinResult) {
        synchronized(this) {
            batchMode = true
            epoch = joinResult.epoch
            step = joinResult.step
            groupId = joinResult.groupId
            batchMode = false
            writeDisk()
        }
    }

    private fun writeDisk() {
        if (enablePersistence && !batchMode)
            repository.save(this)
    }

    private fun stateChecker(currState: NodeState, nextState: NodeState) {
        if (nextState !in acceptStates[currState]!!) {
            throw IllegalStateException("$currState cannot move to $nextState!")
        }
    }

    companion object {
        private val acceptStates = mapOf(
            STARTING to setOf(JOINING),
            JOINING to setOf(WAITING_REBALANCED, RUNNING, FATAL),
            WAITING_REBALANCED to setOf(JOINING, RUNNING, FATAL),
            RUNNING to setOf(JOINING),
            FATAL to emptySet()
        )
    }
}