package moe.nemesiss.keygenerator.service.state

import mu.KotlinLogging


class StateRunner<R : StateContext>(private val provider: StateProvider<R>) {

    companion object {
        private val log = KotlinLogging.logger("StateRunner")
    }

    lateinit var state: State<R>

    fun run(ctx: R) {
        while (!provider.terminated) {
            state = provider.state
            state.handle(provider, ctx)
            // check if last state is the same as newly provided state after handling.
            if (state == provider.state && !provider.terminated) {
                log.warn { "Will run the same state $state twice! Missing terminate or providing new state before return from handle method?" }
            }
        }
    }
}