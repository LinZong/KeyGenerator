package moe.nemesiss.keygenerator.service.repo

import moe.nemesiss.keygenerator.service.KeyGenerator
import moe.nemesiss.keygenerator.service.NodeConfig
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

class KeyRepository(nodeConfig: NodeConfig) : Closeable {

    private val file = File(nodeConfig.dataDir, "keysequence")

    private val saver = RandomAccessFile(file, "rwd")

    @Volatile
    private var closed = false

    @Synchronized
    fun save(key: Long) {
        saver.seek(0)
        saver.writeLong(key)
    }

    @Synchronized
    fun load(): KeyGenerator {
        synchronized(KeyRepository::class.java) {
            saver.seek(0)
            if (saver.length() < 8) {
                save(0)
                return KeyGenerator(this, 0)
            }
            val key = saver.readLong()
            return KeyGenerator(this, key)
        }
    }

    @Synchronized
    override fun close() {
        if (!closed) {
            saver.close()
            closed = true
        }
    }
}