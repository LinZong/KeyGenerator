package moe.nemesiss.keygenerator.service

import moe.nemesiss.keygenerator.service.constant.NodeState
import moe.nemesiss.keygenerator.service.constant.NodeState.*
import moe.nemesiss.keygenerator.service.repo.ClusterInfoRepository
import moe.nemesiss.keygenerator.service.util.guard
import java.util.concurrent.locks.ReentrantReadWriteLock

class ClusterInfo(private val repository: ClusterInfoRepository, val enablePersistence: Boolean = true) {

    // locker
    private val locker = ReentrantReadWriteLock()
    private val readLock = locker.readLock()
    private val writeLock = locker.writeLock()

    @Volatile
    var epoch = 0
        get() = readLock.guard { field }
        set(value) = writeLock.guard { field = value; writeDisk() }

    @Volatile
    var state = STARTING
        get() = readLock.guard { field }
        set(value) = writeLock.guard { stateChecker(field, value); field = value; writeDisk() }

    @Volatile
    var step = 1
        get() = readLock.guard { field }
        set(value) = writeLock.guard { field = value; writeDisk() }

    @Volatile
    var groupId = ""
        get() = readLock.guard { field }
        set(value) = writeLock.guard { field = value; writeDisk() }

    private fun writeDisk() {
        if (enablePersistence)
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