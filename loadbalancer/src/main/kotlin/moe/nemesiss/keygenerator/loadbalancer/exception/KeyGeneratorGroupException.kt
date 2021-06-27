package moe.nemesiss.keygenerator.loadbalancer.exception

open class KeyGeneratorGroupException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
}

class KeyGeneratorGroupNotFoundException : KeyGeneratorGroupException {
    constructor() : super()
    constructor(message: String?) : super(message)
}

class KeyGeneratorGroupRebalancingException : KeyGeneratorGroupException {
    constructor() : super()
    constructor(message: String?) : super(message)
}