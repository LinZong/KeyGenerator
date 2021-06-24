package moe.nemesiss.keygenerator.service.keysequence.io.impl

import com.alibaba.fastjson.JSON
import moe.nemesiss.keygenerator.service.Configuration
import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter
import java.io.*

class FileKeySequenceWriter(private val namespace: String) : KeySequenceWriter {

    override fun writeMetadata(metadata: KeySequenceMetadata) {
        val stringifyMetadata = JSON.toJSONString(metadata)
        // directory is created on boot, but also worth double check.
        Configuration.ensureKeyPathCreated()
        BufferedWriter(FileWriter(File(Configuration.KeyPath, "${namespace}.meta")))
            .use { writer ->
                writer.write(stringifyMetadata)
            }
    }

    override fun writeKey(keyBytes: ByteArray) {
        Configuration.ensureKeyPathCreated()
        BufferedOutputStream(FileOutputStream(File(Configuration.KeyPath, "${namespace}.key")))
            .use { writer ->
                writer.write(keyBytes)
            }
    }
}