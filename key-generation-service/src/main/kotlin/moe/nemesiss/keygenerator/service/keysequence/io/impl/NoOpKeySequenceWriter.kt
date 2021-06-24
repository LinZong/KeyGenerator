package moe.nemesiss.keygenerator.service.keysequence.io.impl

import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.codec.KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter

class NoOpKeySequenceWriter<T : Number> : KeySequenceWriter<T> {

    private lateinit var codec: KeySequenceCodec<T>

    override fun writeMetadata(metadata: KeySequenceMetadata) {

    }

    override fun getCodec(): KeySequenceCodec<T> {
        return codec
    }

    override fun setCodec(codec: KeySequenceCodec<T>) {
        this.codec = codec
    }

    override fun writeKey(keySequence: KeySequence<T>) {

    }
}