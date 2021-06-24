package moe.nemesiss.keygenerator.service.keysequence.io.impl

import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter

class NoOpKeySequenceWriter : KeySequenceWriter {
    override fun writeMetadata(metadata: KeySequenceMetadata) {
        // NO OP
    }

    override fun writeKey(bytes: ByteArray) {
        // NO OP
    }
}