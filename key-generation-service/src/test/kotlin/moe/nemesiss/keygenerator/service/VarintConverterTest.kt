package moe.nemesiss.keygenerator.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import moe.nemesiss.keygenerator.service.util.Varint

class VarintConverterTest : FunSpec({
    test("encode and decode int") {
        for (i in -1048576..1048577) {
            Varint.decodeInt(Varint.encodeInt(i)) shouldBeExactly i
        }

        Varint.decodeInt(Varint.encodeInt(Int.MAX_VALUE)) shouldBeExactly Int.MAX_VALUE
        Varint.decodeInt(Varint.encodeInt(Int.MIN_VALUE)) shouldBeExactly Int.MIN_VALUE
    }

    test("encode and decode long") {
        Varint.decodeLong(Varint.encodeLong(Long.MAX_VALUE)) shouldBeExactly Long.MAX_VALUE
        Varint.decodeLong(Varint.encodeLong(Long.MIN_VALUE)) shouldBeExactly Long.MIN_VALUE
    }

    test("should decode value with unknown bytes") {
        // create a group of meaningless byte with alternate up/down msb.
        val badSuffix = ByteArray(10) { if (it % 2 == 0) (0x7F).toByte() else (0xFF).toByte() }
        // encode number to byte array.
        val data = Varint.encodeInt(123456)
        val appendData = ByteArray(data.size + 10)
        // append bad suffix to previous encoded content.
        data.copyInto(appendData)
        badSuffix.copyInto(appendData, data.size)
        // test our decoder.
        Varint.decodeInt(appendData) shouldBeExactly 123456
    }
})