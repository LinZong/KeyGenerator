package moe.nemesiss.keygenerator.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import moe.nemesiss.keygenerator.service.keysequence.impl.FileBaseLongKeySequence
import moe.nemesiss.keygenerator.service.keysequence.io.KeySequenceWriter
import moe.nemesiss.keygenerator.service.keysequence.io.impl.FileLongKeySequenceLoader
import moe.nemesiss.keygenerator.service.keysequence.io.impl.NoOpLongKeySequenceWriter
import java.io.File
import java.io.IOException

class KeySequenceTest : FunSpec({
    test("increase correctly.") {
        val keySeq =
            FileBaseLongKeySequence("test", 0)
                .apply { writer = NoOpLongKeySequenceWriter() }
        keySeq.apply {
            getKey() shouldBeExactly 0
            getAndIncrease(1) shouldBeExactly 0
            getAndIncrease(2) shouldBeExactly 1
            getKey() shouldBeExactly 3
        }

        val keySeqWithNonZeroInitValue =
            FileBaseLongKeySequence("test2", 10)
                .apply { writer = NoOpLongKeySequenceWriter() }

        keySeqWithNonZeroInitValue.apply {
            getKey() shouldBeExactly 10
            getAndIncrease(1) shouldBeExactly 10
            getAndIncrease(1) shouldBeExactly 11
            getKey() shouldBeExactly 12
        }
    }

    test("persist to disk correctly") {
        val keySeq = FileBaseLongKeySequence("test", 0)
        keySeq.getAndIncrease(1) shouldBeExactly 0

        // check file exists.
        val keyFile = File(Configuration.KeyPath, "test.${KeySequenceWriter.Extensions.LongKey}")
        keyFile.exists() shouldBe true
        shouldNotThrow<IOException> {
            val loadFromDiskKeySeq = FileLongKeySequenceLoader().loadKeySequence(keyFile)
            // increased before.
            loadFromDiskKeySeq.getNamespace() shouldBe "test"
            loadFromDiskKeySeq.getKey() shouldBe 1
            loadFromDiskKeySeq.getAndIncrease(10) shouldBe 1
            loadFromDiskKeySeq.getKey() shouldBe 11
            null
        }
        // test globally search.
        FileLongKeySequenceLoader().loadAllKeySequences().any { ks -> ks.getNamespace() == "test" } shouldBe true
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