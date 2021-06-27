package moe.nemesiss.keygenerator.service

import io.grpc.ManagedChannelBuilder
import moe.nemesiss.keygenerator.grpc.model.JoinRequest
import moe.nemesiss.keygenerator.grpc.model.LoadBalancerGrpcKt
import moe.nemesiss.keygenerator.grpc.model.PingRequest
import java.util.concurrent.TimeUnit

class LoadBalancerClientKt(nodeConfig: NodeConfig) {

    private val stub = LoadBalancerGrpcKt.LoadBalancerCoroutineStub(
        ManagedChannelBuilder.forAddress(
            nodeConfig.loadbalancerHost,
            nodeConfig.loadBalancerPort
        ).usePlaintext().build()
    )

    suspend fun join(joinRequest: JoinRequest) = stub
                                                            .withDeadlineAfter(30, TimeUnit.SECONDS)
                                                            .join(joinRequest)

    suspend fun ping(pingRequest: PingRequest) = stub
                                                             .withDeadlineAfter(200, TimeUnit.MILLISECONDS)
                                                             .ping(pingRequest)
}