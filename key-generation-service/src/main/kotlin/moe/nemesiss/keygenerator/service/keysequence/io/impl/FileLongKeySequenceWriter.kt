package moe.nemesiss.keygenerator.service.keysequence.io.impl

import moe.nemesiss.keygenerator.service.Configuration
import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.codec.impl.Varint64KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class FileLongKeySequenceWriter(namespace: String) : KeySequenceWriter<Long, Long> {
    private val codec = Varint64KeySequenceCodec()

    private val keyFile = File(Configuration.KeyPath, "${namespace}.${KeySequenceWriter.Extensions.LongKey}")

    override fun writeFully(keySequence: KeySequence<Long, Long>) {
        val keyBytes = codec.encode(keySequence)
        BufferedOutputStream(FileOutputStream(keyFile))
            .use { writer ->
                writer.write(keyBytes)
            }
    }

    override fun writeIncremental(keySequence: KeySequence<Long, Long>) {
        val bytes = codec.encode(keySequence)
        val skipBytes = codec.header.bytes.size
        val raf = RandomAccessFile(keyFile, "rws")
        // seek to key pos.
        raf.seek(skipBytes.toLong())
        // write encoded key.
        raf.write(bytes, skipBytes, (bytes.size - skipBytes))
        // reset file size.
        raf.setLength(bytes.size.toLong())
        // finished.
        raf.close()
    }

}