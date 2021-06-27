package moe.nemesiss.keygenerator.loadbalancer.util

abstract class Selector<T> {

    abstract var elements: MutableList<T>

    abstract fun removeElement(e: T)

    abstract fun removeElements(e: Collection<T>)

    abstract fun next(): T
}