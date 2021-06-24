package moe.nemesiss.keygenerator.service.keysequence.io

import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata

interface KeySequenceWriter {

    fun writeMetadata(metadata: KeySequenceMetadata)

    fun writeKey(bytes: ByteArray)
}