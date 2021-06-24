package moe.nemesiss.keygenerator.service.keysequence.io.impl

import com.alibaba.fastjson.JSON
import moe.nemesiss.keygenerator.service.Configuration
import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.codec.KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceLoader
import mu.KotlinLogging
import java.io.File


class FileKeySequenceLoader : KeySequenceLoader {

    companion object {
        private val log = KotlinLogging.logger("KeySequenceLoader")
    }

    fun loadAllKeySequences(): List<KeySequence<Number>> {
        val keySequenceMetadataFiles =
            Configuration.KeyPath.listFiles { file -> file.isFile && file.extension == "meta" } ?: return emptyList()
        val result = arrayListOf<KeySequence<Number>>()
        for (file in keySequenceMetadataFiles) {
            val namespace = file.nameWithoutExtension
            try {
                val metadata = loadMetadata(file)
                val keySequence = loadKeySequence(namespace, metadata)
                result += keySequence
            } catch (e: Throwable) {
                log.error(e) { "Cannot load key with namespace: $namespace!" }
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun loadKeySequence(namespace: String, metadata: KeySequenceMetadata): KeySequence<Number> {
        val keyFile = File(Configuration.KeyPath, "${namespace}.key")
        if (!keyFile.exists()) {
            throw IllegalArgumentException("key file: $namespace is not exist!")
        }
        val keyBytes = loadKeySequenceBytes(keyFile)
        val codec = Class.forName(metadata.codecQualifyName).newInstance() as KeySequenceCodec<Number>
        return codec.decode(keyBytes)
    }

    override fun loadMetadata(metaFile: File): KeySequenceMetadata {
        return JSON.parseObject(metaFile.readText(), KeySequenceMetadata::class.java)
    }

    override fun loadKeySequenceBytes(keySequenceFile: File): ByteArray {
        return keySequenceFile.readBytes()
    }
}