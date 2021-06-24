package moe.nemesiss.keygenerator.service

import com.alibaba.fastjson.JSON
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import moe.nemesiss.keygenerator.service.keysequence.KeySequenceMetadata
import moe.nemesiss.keygenerator.service.keysequence.codec.impl.Varint64KeySequenceCodec
import moe.nemesiss.keygenerator.service.keysequence.impl.LocalLongKeySequence
import moe.nemesiss.keygenerator.service.keysequence.io.impl.FileKeySequenceLoader
import moe.nemesiss.keygenerator.service.keysequence.io.impl.FileKeySequenceWriter
import moe.nemesiss.keygenerator.service.keysequence.io.impl.NoOpKeySequenceWriter
import java.io.File

class KeySequenceTest : FunSpec({

    test("increase correctly") {
        val keySeq = LocalLongKeySequence("test", 0, Varint64KeySequenceCodec(), NoOpKeySequenceWriter())
        keySeq.apply {
            getKey() shouldBeExactly 0
            getAndIncrease(1) shouldBeExactly 0
            getAndIncrease(2) shouldBeExactly 1
            getKey() shouldBeExactly 3
        }

        val keySeqWithNonZeroInitValue =
            LocalLongKeySequence("test2", 10, Varint64KeySequenceCodec(), NoOpKeySequenceWriter())

        keySeqWithNonZeroInitValue.apply {
            getKey() shouldBeExactly 10
            getAndIncrease(1) shouldBeExactly 10
            getAndIncrease(1) shouldBeExactly 11
            getKey() shouldBeExactly 12
        }
    }

    test("persist to disk correctly") {
        val keySeq = LocalLongKeySequence("test", 0, Varint64KeySequenceCodec(), FileKeySequenceWriter("test"))
        keySeq.getAndIncrease(1)

        // check file exists.
        val metaFile = File(Configuration.KeyPath, "test.meta")
        metaFile.exists() shouldBe true
        shouldNotThrow<Throwable> {
            val meta = JSON.parseObject(metaFile.readText(), KeySequenceMetadata::class.java)
            meta.codecQualifyName shouldBe Varint64KeySequenceCodec::class.java.name
            val loadFromDiskKeySeq = FileKeySequenceLoader().loadKeySequence("test", meta)
            // increased before.
            loadFromDiskKeySeq.getNamespace() shouldBe "test"
            loadFromDiskKeySeq.getKey() shouldBe 1
            null
        }
    }

}) {
    override fun beforeTest(testCase: TestCase) {
        Configuration.KeyPath = File("./KeySequenceTest")
        Configuration.ensureKeyPathCreated()
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        Configuration.KeyPath.deleteRecursively()
    }
}