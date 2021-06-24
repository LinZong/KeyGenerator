package moe.nemesiss.keygenerator.service.keysequence.impl

import moe.nemesiss.keygenerator.service.keysequence.KeySequence

class MemoryBaseLongKeySequence(private val namespace: String, private var value: Long = 0) : KeySequence<Long> {
    override fun getKey(): Long {
        return value
    }

    override fun setKey(value: Long) {
        this.value = value
    }

    override fun getAndIncrease(step: Long): Long {
        val oldValue = value
        value += step
        return oldValue
    }

    override fun getNamespace(): String {
        return namespace
    }
}