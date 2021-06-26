package moe.nemesiss.keygenerator.service.state.join

import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.service.constant.Join
import moe.nemesiss.keygenerator.service.state.State
import moe.nemesiss.keygenerator.service.state.StateProvider
import moe.nemesiss.keygenerator.service.util.Observer
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WaitJoinResultState(private val waitTicket: String) : State<JoinStateContext> {
    companion object {
        private val log = KotlinLogging.logger("WaitJoinResultState")
    }

    override fun handle(provider: StateProvider<JoinStateContext>, ctx: JoinStateContext) {
        val result = CompletableFuture<JoinResult>()

        ctx.joinResultObservable += object : Observer<JoinResult> {
            override fun onNext(value: JoinResult) {
                // this method will be run in grpc thread pool.
                if (value.ticket == waitTicket) {
                    result.complete(value)
                }
            }
        }

        try {
            val joinResult = result.get(30, TimeUnit.SECONDS)
            // 保证了ticket
            when (joinResult.code) {
                Join.JOIN_OK -> provider.provide(JoinSuccessState(joinResult))
                else -> provider.provide(JoinFailedState(joinResult))
            }
        } catch (timeout: TimeoutException) {
            log.error { "Join timeout! Try again!" }
            provider.provide(RequestJoinState())
        }
    }
}