package moe.nemesiss.keygenerator.service.keysequence.io.impl

import moe.nemesiss.keygenerator.service.Configuration
import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.codec.impl.Varint64KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter
import mu.KotlinLogging
import java.io.File


class FileLongKeySequenceLoader {

    companion object {
        private val log = KotlinLogging.logger("KeySequenceLoader")
    }

    fun loadAllKeySequences(): List<KeySequence<Long, Long>> {
        val keyFiles =
            Configuration.KeyPath.listFiles { file -> file.isFile && file.extension == KeySequenceWriter.Extensions.LongKey }
                ?: return emptyList()
        val result = arrayListOf<KeySequence<Long, Long>>()
        for (keyFile in keyFiles) {
            val namespace = keyFile.nameWithoutExtension
            try {
                val keySequence = loadKeySequence(keyFile)
                result += keySequence
            } catch (e: Throwable) {
                log.error(e) { "Cannot load key with namespace: $namespace!" }
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun loadKeySequence(keyFile: File): KeySequence<Long, Long> {
        if (!keyFile.exists()) {
            throw IllegalArgumentException("key file: ${keyFile.absolutePath} is not exist!")
        }
        val keyBytes = keyFile.readBytes()
        val codec = Varint64KeySequenceCodec()
        return codec.decode(keyBytes)
    }
}