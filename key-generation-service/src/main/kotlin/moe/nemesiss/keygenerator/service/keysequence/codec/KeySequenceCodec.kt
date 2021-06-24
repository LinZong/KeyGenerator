package moe.nemesiss.keygenerator.service.keysequence.codec

import moe.nemesiss.keygenerator.service.keysequence.KeySequence

interface KeySequenceCodec<T, R : Number> {
    /**
     * encode [key] object to byte array.
     */
    fun encode(key: KeySequence<T, R>): ByteArray

    /**
     * decode binary [data] to key
     */
    fun decode(data: ByteArray): KeySequence<T, R>
}