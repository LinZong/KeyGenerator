package moe.nemesiss.keygenerator.service.timertask

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import moe.nemesiss.keygenerator.grpc.model.pingRequest
import moe.nemesiss.keygenerator.service.ClusterInfo
import moe.nemesiss.keygenerator.service.LoadBalancerClientKt
import moe.nemesiss.keygenerator.service.NodeConfig
import moe.nemesiss.keygenerator.service.constant.NodeState
import moe.nemesiss.keygenerator.service.constant.Pong
import org.quartz.*

interface HeartbeatJobCallback {
    fun onHeartbeatSuccess()
    fun onHeartbeatFailed(code: Int, e: Throwable? = null)
}

class HeartbeatJob : Job {

    companion object {
        val Key = JobKey("heartbeat", "cluster")

        fun build(
            loadBalancerClient: LoadBalancerClientKt,
            nodeConfig: NodeConfig,
            clusterInfo: ClusterInfo,
            heartbeatJobCallback: HeartbeatJobCallback
        ): Pair<JobDetail, Trigger> {
            val dataMap = JobDataMap()
            dataMap.apply {
                this["loadbalancer"] = loadBalancerClient
                this["nodeConfig"] = nodeConfig
                this["clusterInfo"] = clusterInfo
                this["callback"] = heartbeatJobCallback
            }
            val trigger = TriggerBuilder
                .newTrigger()
                .usingJobData(dataMap)
                .withIdentity("heartbeat-trigger", "cluster")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ? *")) // every minutes.
                .build()
            val job = JobBuilder
                .newJob(HeartbeatJob::class.java)
                .withIdentity(Key)
                .build()

            return job to trigger
        }
    }

    override fun execute(context: JobExecutionContext) {
        val client = context.get("loadbalancer") as LoadBalancerClientKt
        val nodeConfig = context.get("nodeConfig") as NodeConfig
        val clusterInfo = context.get("clusterInfo") as ClusterInfo
        val callback = context.get("callback") as HeartbeatJobCallback

        try {
            if (clusterInfo.state != NodeState.RUNNING) {
                // 其他状态不需要心跳
                callback.onHeartbeatFailed(Pong.NOT_IN_RUNNING_STATE)
                return
            }

            val pong = runBlocking {
                withTimeoutOrNull(1 * 1000) {
                    client.ping(pingRequest {
                        namespace = nodeConfig.namespace
                        groupId = clusterInfo.groupId
                        epoch = clusterInfo.epoch
                        name = nodeConfig.name
                    })
                }
            }

            if (pong == null) {
                callback.onHeartbeatFailed(Pong.TIME_OUT)
                return
            }
            // here pong is non-null.

            if (pong.code == Pong.OK) {
                callback.onHeartbeatSuccess()
                return
            }
            // failed code.
            callback.onHeartbeatFailed(pong.code)
        } catch (e: Throwable) {
            callback.onHeartbeatFailed(Pong.EXCEPTION_THROWN, e)
        }
    }
}