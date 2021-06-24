package moe.nemesiss.keygenerator.service.keysequence.impl

import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter
import moe.nemesiss.keygenerator.service.keysequence.io.impl.FileLongKeySequenceWriter

class FileBaseLongKeySequence(
    private val namespace: String,
    private var value: Long,
) : KeySequence<Long, Long> {

    var writer: KeySequenceWriter<Long, Long> = FileLongKeySequenceWriter(namespace)
        set(value) {
            synchronized(this) {
                field.close()
                field = value
            }
        }

    init {
        writer.writeFully(this)
        Runtime.getRuntime()
            .addShutdownHook(Thread {
                // release writer
                writer.close()
            })
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
            writer.writeIncremental(this)
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