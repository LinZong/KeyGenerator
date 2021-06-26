package moe.nemesiss.keygenerator.service.state.join

import io.grpc.StatusException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import moe.nemesiss.keygenerator.grpc.model.joinRequest
import moe.nemesiss.keygenerator.service.constant.Join
import moe.nemesiss.keygenerator.service.constant.NodeState
import moe.nemesiss.keygenerator.service.state.State
import moe.nemesiss.keygenerator.service.state.StateProvider
import moe.nemesiss.keygenerator.service.timertask.HeartbeatJob
import moe.nemesiss.keygenerator.service.util.RetryException
import moe.nemesiss.keygenerator.service.util.retry
import mu.KotlinLogging
import java.util.*

class RequestJoinState : State<JoinStateContext> {

    companion object {
        private val log = KotlinLogging.logger("RequestJoinState")
        private const val RetryForTimeoutLimit = 5
    }

    override fun handle(provider: StateProvider<JoinStateContext>, ctx: JoinStateContext) {
        if (ctx.clusterInfo.state == NodeState.WAITING_REBALANCED) {
            // 超时，先清除所有等待状态
            // 清除心跳定时任务。
            ctx.scheduler.deleteJob(HeartbeatJob.Key)
        }
        // 设置自身状态为JOINING
        ctx.clusterInfo.state = NodeState.JOINING
        // 发起加入请求

        try {
            val joinTicket = UUID.randomUUID().toString()

            val join = retry(RetryForTimeoutLimit) {
                runBlocking {
                    try {
                        withTimeout(30 * 1000L) {
                            ctx.loadBalancerClient.join(
                                joinRequest {
                                    namespace = ctx.nodeConfig.namespace
                                    groupId = ctx.clusterInfo.groupId
                                    epoch = ctx.clusterInfo.epoch
                                    name = ctx.nodeConfig.name
                                    ticket = joinTicket
                                }
                            )
                        }
                    } catch (_: TimeoutCancellationException) {
                        throw RetryException()
                    }
                }
            }

            if (join.isFailure) {
                log.error { "request join failed. all retry were timeout." }
                provider.terminate()
            }
            val result = join.getOrNull()!!

            when (result.code) {
                Join.JOIN_OK -> {
                    provider.provide(JoinSuccessState(result))
                }
                Join.WAITING_REBALANCED -> {
                    // 监听服务器在一分钟内回传的ClusterInfo
                    provider.provide(WaitJoinResultState(joinTicket))
                }
                else -> {
                    provider.provide(JoinFailedState(result))
                }
            }

        } catch (se: StatusException) {
            log.error(se) { "request join failed. internal error occurred in rpc communication." }
            provider.terminate()
        }
    }
}