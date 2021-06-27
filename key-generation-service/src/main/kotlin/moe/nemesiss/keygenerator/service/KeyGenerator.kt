package moe.nemesiss.keygenerator.service

import moe.nemesiss.keygenerator.service.repo.KeyRepository
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class KeyGenerator(private val keyRepository: KeyRepository, initValue: Long) {

    private val locker = ReentrantReadWriteLock()

    private val rl = locker.readLock()

    private val wl = locker.writeLock()

    @Volatile
    var key: Long = initValue
        get() = rl.withLock { field }
        set(value) = wl.withLock { field = value }

    fun getAndIncrease(step: Int) = wl.withLock {
        val oldKey = key
        key += step
        keyRepository.save(key)
        return@withLock oldKey
    }
}