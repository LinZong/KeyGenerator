package moe.nemesiss.keygenerator.service.keysequence.io

import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.codec.KeySequenceCodec

interface KeySequenceWriter<T : Number> {

    /**
     * write key sequence metadata to file.
     */
    fun writeMetadata(metadata: KeySequenceMetadata)

    /**
     * write bound key to file.
     */
    fun writeKey(keySequence: KeySequence<T>)

    /**
     * get key sequence codec.
     */
    fun getCodec(): KeySequenceCodec<T>

    /**
     * set key sequence codec.
     */
    fun setCodec(codec: KeySequenceCodec<T>)
}