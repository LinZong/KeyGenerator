package moe.nemesiss.keygenerator.service.util

import java.util.*

interface Observer<T> {
    fun onNext(value: T)
}

abstract class Observable<T> {

    protected val subscribers = LinkedList<Observer<T>>()

    open fun publish(value: T) {
        val snapshot = synchronized(this) { LinkedList(subscribers) }
        for (sub in snapshot) {
            sub.onNext(value)
        }
    }

    open fun subscribe(observable: Observer<T>) {
        synchronized(this) {
            subscribers.add(observable)
        }
    }

    open fun unsubscribe(observable: Observer<T>) {
        synchronized(this) {
            subscribers.remove(observable)
        }
    }

    operator fun plusAssign(observable: Observer<T>) {
        subscribe(observable)
    }

    operator fun minusAssign(observable: Observer<T>) {
        unsubscribe(observable)
    }
}


open class StickyObservable<T> : Observable<T>() {

    private val stickyBuffer = LinkedList<T>()

    override fun publish(value: T) {
        if (subscribers.isEmpty()) {
            synchronized(this) {
                if (subscribers.isEmpty()) {
                    stickyBuffer += value
                    // go out
                    return
                }
            }
        }
        super.publish(value)
    }

    override fun subscribe(observable: Observer<T>) {
        synchronized(this) {
            super.subscribe(observable)
            // 现在subscribers一定有
            if (stickyBuffer.isNotEmpty()) {
                publishBufferedChangesLocked()
            }
        }
    }

    private fun publishBufferedChangesLocked() {
        stickyBuffer.forEach { e -> super.publish(e) }
        stickyBuffer.clear()
    }
}