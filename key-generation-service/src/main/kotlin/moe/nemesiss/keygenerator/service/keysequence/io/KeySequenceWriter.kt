package moe.nemesiss.keygenerator.service.keysequence.io

import moe.nemesiss.keygenerator.service.keysequence.KeySequence

interface KeySequenceWriter<T, R : Number> {

    object Extensions {
        const val LongKey = "longkey"
    }

    fun writeFully(keySequence: KeySequence<T, R>)

    fun writeIncremental(keySequence: KeySequence<T, R>)
}