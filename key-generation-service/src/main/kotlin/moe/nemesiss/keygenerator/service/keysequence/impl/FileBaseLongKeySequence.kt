package moe.nemesiss.keygenerator.service.keysequence.impl

import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.codec.KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter
import moe.nemesiss.keygenerator.service.keysequence.io.impl.FileKeySequenceWriter

class FileBaseLongKeySequence(
    private val namespace: String,
    private var value: Long,
    private val codec: KeySequenceCodec<Long>
) : KeySequence<Long> {

    var writer: KeySequenceWriter<Long> = FileKeySequenceWriter(namespace, codec)
        set(value) {
            field = value.apply { setCodec(codec) }
            writer.writeMetadata(KeySequenceMetadata(codec::class.java.name))
        }

    init {
        writer.writeMetadata(KeySequenceMetadata(codec::class.java.name))
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
            writer.writeKey(this)
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
}