package moe.nemesiss.keygenerator.service.util

class RetryException : Exception()

class RetryReachLimitException : Exception()

fun <T> retry(limit: Int, block: () -> T): Result<T> {
    var time = 0
    while (time < limit) {
        try {
            val ret = block()
            return Result.success(ret)
        } catch (_: RetryException) {
            time++
            continue
        }
    }
    return Result.failure(RetryReachLimitException())
}
