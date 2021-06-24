package moe.nemesiss.keygenerator.service.keysequence.io.impl

import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter

class NoOpLongKeySequenceWriter : KeySequenceWriter<Long, Long> {
    override fun writeFully(keySequence: KeySequence<Long, Long>) {

    }

    override fun writeIncremental(keySequence: KeySequence<Long, Long>) {

    }
}