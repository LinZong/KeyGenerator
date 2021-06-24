package moe.nemesiss.keygenerator.service.keysequence.codec

import moe.nemesiss.keygenerator.service.keysequence.KeySequence

interface KeySequenceCodec<T : Number> {
    /**
     * encode [key] object to byte array.
     */
    fun encode(key: KeySequence<T>): ByteArray

    /**
     * decode binary [data] to key
     */
    fun decode(data: ByteArray): KeySequence<T>
}