package moe.nemesiss.keygenerator.service.keysequence.io.impl

import com.alibaba.fastjson.JSON
import moe.nemesiss.keygenerator.service.Configuration
import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.codec.KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter
import java.io.*

class FileKeySequenceWriter<T : Number>(
    private val namespace: String,
    private var codec: KeySequenceCodec<T>
) : KeySequenceWriter<T> {

    override fun writeMetadata(metadata: KeySequenceMetadata) {
        val stringifyMetadata = JSON.toJSONString(metadata)
        // directory is created on boot, but also worth double check.
        Configuration.ensureKeyPathCreated()
        BufferedWriter(FileWriter(File(Configuration.KeyPath, "${namespace}.meta")))
            .use { writer ->
                writer.write(stringifyMetadata)
            }
    }

    override fun writeKey(keySequence: KeySequence<T>) {
        Configuration.ensureKeyPathCreated()
        val keyBytes = codec.encode(keySequence)
        BufferedOutputStream(FileOutputStream(File(Configuration.KeyPath, "${keySequence.getNamespace()}.key")))
            .use { writer ->
                writer.write(keyBytes)
            }
    }

    override fun getCodec(): KeySequenceCodec<T> {
        return codec
    }

    override fun setCodec(codec: KeySequenceCodec<T>) {
        this.codec = codec
    }
}