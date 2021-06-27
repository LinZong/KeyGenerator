package moe.nemesiss.keygenerator.service.state.join

import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.base.constant.Join
import moe.nemesiss.keygenerator.base.constant.NodeState
import moe.nemesiss.keygenerator.service.state.State
import moe.nemesiss.keygenerator.service.state.StateProvider
import mu.KotlinLogging

class JoinFailedState(private val joinResult: JoinResult) : State<JoinStateContext> {
    companion object {
        private val log = KotlinLogging.logger("JoinFailedState")
    }

    override fun handle(provider: StateProvider<JoinStateContext>, ctx: JoinStateContext) {
        log.error { "Join to namespace ${ctx.nodeConfig.namespace} with groupId: ${ctx.clusterInfo.groupId} failed." }
        ctx.clusterInfo.state = NodeState.FATAL
        when (joinResult.code) {
            Join.NO_NAMESPACE -> {
                log.error { "reason: no namespace." }
            }
            Join.INVALID_GROUP_ID -> {
                log.error { "reason: groupId is invalid! This means groupId in server-side had been probably changed. If you want a fresh join, please do not specify groupId." }
            }
            else -> {
                log.error { "reason: unknown" }
            }
        }
        provider.terminate()
    }
}