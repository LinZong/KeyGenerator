package moe.nemesiss.keygenerator.service.state.join

import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.service.ClusterInfo
import moe.nemesiss.keygenerator.service.KeyGenerator
import moe.nemesiss.keygenerator.service.LoadBalancerClientKt
import moe.nemesiss.keygenerator.service.NodeConfig
import moe.nemesiss.keygenerator.service.state.StateContext
import moe.nemesiss.keygenerator.service.timertask.HeartbeatJobCallback
import moe.nemesiss.keygenerator.service.util.Observable
import org.quartz.Scheduler

class JoinStateContext(
    val nodeConfig: NodeConfig,
    val clusterInfo: ClusterInfo,
    val loadBalancerClient: LoadBalancerClientKt,
    val scheduler: Scheduler,
    val heartbeatJobCallback: HeartbeatJobCallback,
    val joinResultObservable: Observable<JoinResult>,
    val keyGenerator: KeyGenerator
) : StateContext()