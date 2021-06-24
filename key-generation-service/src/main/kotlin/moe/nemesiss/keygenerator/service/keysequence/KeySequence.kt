package moe.nemesiss.keygenerator.service.keysequence

interface KeySequence<T : Number> {

    /**
     * get current key on sequence.
     */
    fun getKey(): T

    /**
     * set a new initial key to sequence.
     */
    fun setKey(value: T)

    /**
     * get current key on sequence and increase it before return.
     * @return sequence key before increase.
     */
    fun getAndIncrease(step: T): T

    /**
     * get namespace for key sequence.
     */
    fun getNamespace(): String
}