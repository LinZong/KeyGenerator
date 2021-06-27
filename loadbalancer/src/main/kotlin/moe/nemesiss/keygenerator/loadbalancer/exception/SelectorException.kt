package moe.nemesiss.keygenerator.loadbalancer.exception

class SafeUpperBoundRoundRobinSelectorException : KeyGeneratorGroupException {
    constructor() : super()
    constructor(message: String?) : super(message)
}