package moe.nemesiss.keygenerator.service.state.join

import moe.nemesiss.keygenerator.base.constant.NodeState
import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.service.state.State
import moe.nemesiss.keygenerator.service.state.StateProvider
import moe.nemesiss.keygenerator.service.timertask.HeartbeatJob
import mu.KotlinLogging

class JoinSuccessState(private val joinResult: JoinResult, private val fastPath: Boolean = false) :
    State<JoinStateContext> {
    companion object {
        private val log = KotlinLogging.logger("JoinSuccessState")
    }

    override fun handle(provider: StateProvider<JoinStateContext>, ctx: JoinStateContext) {
        // 检查先前的groupId
        if (ctx.clusterInfo.groupId.isEmpty()) {
            // 应用新的groupId
            ctx.clusterInfo.groupId = joinResult.groupId
        }
        // change cluster state to running.
        ctx.clusterInfo.state = NodeState.RUNNING
        if (!fastPath) {
            log.info { "apply joinResult $joinResult to cluster info." }
            ctx.clusterInfo.update(joinResult)
            ctx.keyGenerator.key = joinResult.key
        } else {
            log.info { "fastpath, not changed." }
        }

        log.info { "Join to namespace: ${joinResult.namespace} with groupId: ${joinResult.groupId} success!" }

        // 注册心跳
        val (job, trigger) = HeartbeatJob.build(
            ctx.loadBalancerClient,
            ctx.nodeConfig,
            ctx.clusterInfo,
            ctx.heartbeatJobCallback
        )
        ctx.scheduler.scheduleJob(job, trigger)

        log.info { "Heartbeat checker registered!" }
        provider.terminate()
    }
}