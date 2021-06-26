package moe.nemesiss.keygenerator.service

import moe.nemesiss.keygenerator.service.repo.KeyRepository
import moe.nemesiss.keygenerator.service.util.guard
import java.util.concurrent.locks.ReentrantReadWriteLock

class KeyGenerator(private val keyRepository: KeyRepository, initValue: Long) {

    private val locker = ReentrantReadWriteLock()

    private val rl = locker.readLock()

    private val wl = locker.writeLock()

    @Volatile
    var key: Long = initValue
        get() = rl.guard { field }
        set(value) = wl.guard { field = value }

    fun getAndIncrease(step: Int) = wl.guard {
        val oldKey = key
        key += step
        keyRepository.save(key)
        return@guard oldKey
    }

}