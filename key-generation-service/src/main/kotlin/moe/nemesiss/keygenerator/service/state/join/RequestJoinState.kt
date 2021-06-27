package moe.nemesiss.keygenerator.service.state.join

import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import moe.nemesiss.keygenerator.base.constant.Join
import moe.nemesiss.keygenerator.base.constant.NodeState
import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.grpc.model.joinRequest
import moe.nemesiss.keygenerator.service.state.State
import moe.nemesiss.keygenerator.service.state.StateProvider
import moe.nemesiss.keygenerator.service.timertask.HeartbeatJob
import moe.nemesiss.keygenerator.service.util.Observer
import moe.nemesiss.keygenerator.service.util.RetryException
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.CompletableFuture

class RequestJoinState : State<JoinStateContext> {

    companion object {
        private val log = KotlinLogging.logger("RequestJoinState")
        private const val RetryForTimeoutLimit = 5
    }

    override fun handle(provider: StateProvider<JoinStateContext>, ctx: JoinStateContext) {
        if (ctx.clusterInfo.state == NodeState.JOINING) {
            log.warn { "Can only run one join request at the same time." }
            provider.terminate()
        }

        // 清除心跳定时任务。
        ctx.scheduler.deleteJob(HeartbeatJob.Key)
        // 设置自身状态为JOINING
        ctx.clusterInfo.state = NodeState.JOINING
        // 发起加入请求

        try {
            val joinTicket = UUID.randomUUID().toString()

            val rebalanceFuture = CompletableFuture<JoinResult>()

            val rebalanceObserver = object : Observer<JoinResult> {
                override fun onNext(value: JoinResult) {
                    if (value.ticket == joinTicket) {
                        rebalanceFuture.complete(value)
                        // 解除订阅
                        ctx.joinResultObservable -= this
                    }
                }
            }

            ctx.joinResultObservable += rebalanceObserver

            val result =
                runBlocking {
                    try {
                        ctx.loadBalancerClient.join(
                            joinRequest {
                                namespace = ctx.nodeConfig.namespace
                                groupId = ctx.clusterInfo.groupId
                                epoch = ctx.clusterInfo.epoch
                                name = ctx.nodeConfig.name
                                ticket = joinTicket
                                host = ctx.nodeConfig.host
                                port = ctx.nodeConfig.port
                            }
                        )
                    } catch (se: StatusException) {
                        if (se.status == Status.DEADLINE_EXCEEDED)
                            throw RetryException()
                        else throw se
                    } catch (e: Throwable) {
                        log.error(e) { "eee" }
                        throw e
                    }
                }
            log.info { "get join result: $result" }

            when (result.code) {
                Join.JOIN_OK -> {
                    provider.provide(JoinSuccessState(result, fastPath = true))
                }
                Join.WAITING_REBALANCED -> {
                    // 监听服务器在一分钟内回传的ClusterInfo
                    provider.provide(WaitJoinResultState(rebalanceFuture, rebalanceObserver))
                }
                else -> {
                    provider.provide(JoinFailedState(result))
                }
            }

        } catch (se: StatusException) {
            log.error(se) { "request join failed. internal error occurred in rpc communication." }
            ctx.clusterInfo.state = NodeState.FATAL
            provider.terminate()
        }
    }
}