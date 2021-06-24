package moe.nemesiss.keygenerator.service.keysequence.io

import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import java.io.File

interface KeySequenceLoader {

    /**
     * load metadata from file
     */
    fun loadMetadata(metaFile: File): KeySequenceMetadata

    /**
     * load key sequence bytes from file
     */
    fun loadKeySequenceBytes(keySequenceFile: File): ByteArray
}