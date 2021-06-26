package moe.nemesiss.keygenerator.service.repo

import moe.nemesiss.keygenerator.service.KeyGenerator
import moe.nemesiss.keygenerator.service.NodeConfig
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

class KeyRepository(nodeConfig: NodeConfig): Closeable {

    val file = File(nodeConfig.dataDir, "keysequence")

    val saver = RandomAccessFile(file, "rwd")

    fun save(key: Long) {
        saver.seek(0)
        saver.writeLong(key)
    }

    fun load(): KeyGenerator {
        saver.seek(0)
        if (saver.length() < 8) {
            save(0)
            return KeyGenerator(this, 0)
        }
        val key = saver.readLong()
        return KeyGenerator(this, key)
    }

    override fun close() {
        saver.close()
    }
}