package moe.nemesiss.keygenerator.service.keysequence.codec.impl

import moe.nemesiss.keygenerator.service.keysequence.KeySequence
import moe.nemesiss.keygenerator.service.keysequence.codec.KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.impl.FileBaseLongKeySequence
import moe.nemesiss.keygenerator.service.util.Varint
import moe.nemesiss.keygenerator.service.util.join
import java.nio.charset.StandardCharsets

/**
 * binary data structure:
 * [namespace byte length(varint32) | namespace | key(varint64) ]
 *
 * a varint value: [varint length(1) | varint byte array]
 */
class Varint64KeySequenceCodec : KeySequenceCodec<Long, Long> {

    class Header(val namespace: String) {
        val bytes: ByteArray by lazy { encodedBytes() }

        private fun encodedBytes(): ByteArray {
            val namespaceByte = namespace.toByteArray()
            val namespaceByteLengthVarint = Varint.encodeInt(namespaceByte.size)
            val headerSize = 1 + namespaceByteLengthVarint.size + namespaceByte.size
            val bytes = ByteArray(headerSize)
            var copied = 0
            bytes[copied++] = namespaceByteLengthVarint.size.toByte()

            namespaceByteLengthVarint.copyInto(bytes, copied)
            copied += namespaceByteLengthVarint.size

            namespaceByte.copyInto(bytes, copied)
            copied += namespaceByte.size
            assert(copied == headerSize)
            return bytes
        }

        companion object {
            val EMPTY = Header("EMPTY")

            fun decodeHeader(bytes: ByteArray): Pair<Header, Int> {
                var read = 0
                val headerLengthVarintLength = bytes[read++].toInt()
                val headerLengthVarint = bytes.copyOfRange(read, read + headerLengthVarintLength)
                read += headerLengthVarintLength
                val headerLength = Varint.decodeInt(headerLengthVarint)
                val headerBytes = bytes.copyOfRange(read, read + headerLength)
                read += headerLength
                return Header(headerBytes.toString(StandardCharsets.UTF_8)) to read
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Header
            if (namespace != other.namespace) return false
            return true
        }

        override fun hashCode(): Int {
            return namespace.hashCode()
        }
    }

    var header = Header.EMPTY
        private set

    override fun encode(key: KeySequence<Long, Long>): ByteArray {
        val currentHeader = Header(key.getNamespace())
        if (currentHeader != header) {
            header = currentHeader
            // enforce generating encoded bytes.
            header.bytes
        }

        val keyVarint = Varint.encodeLong(key.getKey())
        val size = 1 + keyVarint.size

        val keyBytes = ByteArray(size)
        var copied = 0

        keyBytes[copied++] = keyVarint.size.toByte()
        keyVarint.copyInto(keyBytes, copied)
        copied += keyVarint.size

        return header.bytes.join(keyBytes)
    }

    override fun decode(data: ByteArray): KeySequence<Long, Long> {
        var (header, readBytes) = Header.decodeHeader(data)
        // update cached keys.
        this.header = header

        val keyVarintLength = data[readBytes++]
        val keyVarint = data.copyOfRange(readBytes, readBytes + keyVarintLength)
        readBytes += keyVarintLength
        val key = Varint.decodeLong(keyVarint)
        return FileBaseLongKeySequence(header.namespace, key)
    }

}