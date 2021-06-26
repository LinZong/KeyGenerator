package moe.nemesiss.keygenerator.service.util

import java.util.concurrent.locks.Lock

fun <T> Lock.guard(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}