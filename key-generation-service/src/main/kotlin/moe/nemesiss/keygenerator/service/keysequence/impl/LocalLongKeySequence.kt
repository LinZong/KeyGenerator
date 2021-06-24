package moe.nemesiss.keygenerator.service.keysequence.impl

import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.codec.KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter
import moe.nemesiss.keygenerator.service.keysequence.io.impl.FileKeySequenceWriter

class LocalLongKeySequence(
    private val namespace: String,
    private var value: Long,
    private val codec: KeySequenceCodec<Long>,
    private val writer: KeySequenceWriter = FileKeySequenceWriter(namespace)
) : KeySequence<Long> {


    init {
        writer.writeMetadata(KeySequenceMetadata(codec.javaClass.name))
    }

    override fun getKey(): Long {
        return value
    }

    override fun setKey(value: Long) {
        // here no need to add lock because service is running with only one thread.
        val oldValue = this.value
        try {
            this.value = value
            // encode
            val bytes = codec.encode(this)
            writeToDisk(bytes)
            // new value is published!
        } catch (e: Throwable) {
            // fallback to old value
            this.value = oldValue
        }
    }

    override fun getAndIncrease(step: Long): Long {
        val v = getKey()
        setKey(v + step)
        return v
    }

    override fun getNamespace(): String {
        return namespace
    }

    private fun writeToDisk(bytes: ByteArray) {
        writer.writeKey(bytes)
    }
}