package moe.nemesiss.keygenerator.service.state.join

import moe.nemesiss.keygenerator.base.constant.Join
import moe.nemesiss.keygenerator.base.constant.NodeState
import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.service.state.State
import moe.nemesiss.keygenerator.service.state.StateProvider
import moe.nemesiss.keygenerator.service.util.Observer
import mu.KotlinLogging
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WaitJoinResultState(
    private val rebalanceResult: Future<JoinResult>,
    private val rebalanceObserver: Observer<JoinResult>
) : State<JoinStateContext> {
    companion object {
        private val log = KotlinLogging.logger("WaitJoinResultState")
    }

    override fun handle(provider: StateProvider<JoinStateContext>, ctx: JoinStateContext) {

        ctx.clusterInfo.state = NodeState.WAITING_REBALANCED

        try {
            val joinResult = rebalanceResult.get(3000, TimeUnit.SECONDS)
            // 保证了ticket
            when (joinResult.code) {
                Join.JOIN_OK -> provider.provide(JoinSuccessState(joinResult, fastPath = false))
                else -> provider.provide(JoinFailedState(joinResult))
            }
        } catch (timeout: TimeoutException) {
            log.error { "Join timeout! Try again!" }
            ctx.joinResultObservable -= rebalanceObserver
            provider.provide(RequestJoinState())
        }
    }
}