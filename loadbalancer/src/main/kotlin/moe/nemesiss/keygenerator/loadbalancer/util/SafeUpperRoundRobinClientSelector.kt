package moe.nemesiss.keygenerator.loadbalancer.util

import moe.nemesiss.keygenerator.loadbalancer.KeyGeneratorClient
import moe.nemesiss.keygenerator.loadbalancer.exception.SafeUpperBoundRoundRobinSelectorException
import moe.nemesiss.keygenerator.loadbalancer.repo.GroupMetaRepository
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class SafeUpperRoundRobinClientSelector(
    private val repository: GroupMetaRepository,
    initElements: List<KeyGeneratorClient>,
    initRound: Int,
    initMaxRound: Int,
    val maxRoundDelta: Int
) : Selector<KeyGeneratorClient>() {


    private val locker = ReentrantReadWriteLock()
    private val rl = locker.readLock()
    private val wl = locker.writeLock()

    override var elements: MutableList<KeyGeneratorClient> = initElements.toMutableList()
        set(value) = wl.withLock {
            field = value.toMutableList()
            // 重设各类下标
            index = 0
            round = 0
            maxRound = maxRoundDelta
            repository.saveSelector(this)
        }

    @Volatile
    private var index = 0

    @Volatile
    var round = initRound
        private set

    @Volatile
    var maxRound = initMaxRound.coerceAtLeast(initRound + maxRoundDelta)
        private set

    init {
        // check value for safe.
        if (listOf(initRound, initMaxRound, maxRoundDelta).any { v -> v < 0 }) {
            throw SafeUpperBoundRoundRobinSelectorException("all value should not be < 0.")
        }
        if (initRound > initMaxRound) {
            throw SafeUpperBoundRoundRobinSelectorException("init round should be less than init max round!")
        }
    }

    val elementSnapshot get() = rl.withLock { LinkedList(elements) }

    fun addElement(e: KeyGeneratorClient) {
        wl.withLock { elements += e }
    }

    @Synchronized
    override fun next(): KeyGeneratorClient {
        val snapshot = elementSnapshot
        if (snapshot.isEmpty()) {
            throw SafeUpperBoundRoundRobinSelectorException("no element.")
        }
        index = (index + 1) % snapshot.size
        // here is safe because only rebalance will write elements, and here is for get request, which is mutual.
        if (index == 0) {
            handleNextRoundLocked()
        }

        return snapshot[index]
    }


    private fun handleNextRoundLocked() {
        round++
        if (maxRound - round < maxRoundDelta) {
            // 留有一定余量
            maxRound = round + maxRoundDelta * 2
            repository.saveSelector(this)
        }
    }

    override fun removeElement(e: KeyGeneratorClient) {
        wl.withLock { elements.remove(e) }
    }

    override fun removeElements(e: Collection<KeyGeneratorClient>) {
        wl.withLock { elements.removeAll(e) }
    }
}