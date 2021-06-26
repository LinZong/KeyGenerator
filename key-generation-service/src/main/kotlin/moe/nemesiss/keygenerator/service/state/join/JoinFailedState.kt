package moe.nemesiss.keygenerator.service.state.join

import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.service.constant.Join
import moe.nemesiss.keygenerator.service.constant.NodeState
import moe.nemesiss.keygenerator.service.state.State
import moe.nemesiss.keygenerator.service.state.StateProvider
import mu.KotlinLogging

class JoinFailedState(private val joinResult: JoinResult) : State<JoinStateContext> {
    companion object {
        private val log = KotlinLogging.logger("JoinFailedState")
    }

    override fun handle(provider: StateProvider<JoinStateContext>, ctx: JoinStateContext) {
        log.error { "Join to namespace ${ctx.nodeConfig.namespace} with groupId: ${ctx.clusterInfo.groupId} failed." }
        when (joinResult.code) {
            Join.NO_NAMESPACE -> {
                log.error { "reason: no namespace." }
                ctx.clusterInfo.state = NodeState.FATAL
            }
            Join.INVALID_GROUP_ID -> {
                log.error { "reason: groupId is invalid! This means groupId in server-side had been probably changed. If you want a fresh join, please do not specify groupId." }
                ctx.clusterInfo.state = NodeState.FATAL
            }
            else -> {
                log.error { "reason: unknown" }
                ctx.clusterInfo.state = NodeState.FATAL
            }
        }
        provider.terminate()
    }
}