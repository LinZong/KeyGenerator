package moe.nemesiss.keygenerator.service.state

interface State<T : StateContext> {
    fun handle(provider: StateProvider<T>, ctx: T)
}


class StateProvider<T : StateContext>(initState: State<T>) {

    var state: State<T> = initState
        private set

    var terminated = false
        private set

    fun provide(state: State<T>) {
        if (terminated) {
            throw IllegalStateException("already terminated!")
        }
        this.state = state
    }

    fun terminate() {
        terminated = true
    }
}
