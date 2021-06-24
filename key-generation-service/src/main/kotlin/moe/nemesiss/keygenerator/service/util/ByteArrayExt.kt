package moe.nemesiss.keygenerator.service.util

fun ByteArray.join(vararg elements: ByteArray): ByteArray {
    val size = this.size + elements.sumOf { e -> e.size }
    assert(0 <= size && size <= Int.MAX_VALUE)
    val result = ByteArray(size)
    val candidates = listOf(this) + listOf(*elements)
    var copied = 0
    for (candidate in candidates) {
        candidate.copyInto(result, copied)
        copied += candidate.size
    }
    assert(copied == size)
    return result
}