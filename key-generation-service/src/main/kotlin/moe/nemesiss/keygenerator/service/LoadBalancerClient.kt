package moe.nemesiss.keygenerator.service

import io.grpc.Deadline
import io.grpc.ManagedChannelBuilder
import moe.nemesiss.keygenerator.grpc.model.JoinRequest
import moe.nemesiss.keygenerator.grpc.model.LoadBalancerGrpc
import moe.nemesiss.keygenerator.grpc.model.PingRequest
import moe.nemesiss.keygenerator.service.NodeConfig
import java.util.concurrent.TimeUnit

class LoadBalancerClient(nodeConfig: NodeConfig) {

    private val stub = LoadBalancerGrpc.newFutureStub(
        ManagedChannelBuilder.forAddress(
            nodeConfig.loadbalancerHost,
            nodeConfig.loadBalancerPort
        ).usePlaintext().build()
    ).withDeadline(Deadline.after(200, TimeUnit.MILLISECONDS))

    fun join(joinRequest: JoinRequest) = stub.join(joinRequest)

    fun ping(pingRequest: PingRequest) = stub.ping(pingRequest)
}