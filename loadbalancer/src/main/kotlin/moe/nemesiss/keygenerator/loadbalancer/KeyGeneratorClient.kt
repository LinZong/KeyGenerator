package moe.nemesiss.keygenerator.loadbalancer

import io.grpc.CallOptions
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import moe.nemesiss.keygenerator.grpc.model.JoinResult
import moe.nemesiss.keygenerator.grpc.model.KeyGeneratorNodeGrpcKt
import moe.nemesiss.keygenerator.grpc.model.getKeyRequest
import java.util.concurrent.TimeUnit

class KeyGeneratorClient(val name: String, host: String, port: Int) {
    private val stub = KeyGeneratorNodeGrpcKt.KeyGeneratorNodeCoroutineStub(
        ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build(),
        CallOptions.DEFAULT
    )

    @Volatile
    var errorCount = 0
        private set

    fun getKey() = runBlocking {
        stub.withDeadlineAfter(100, TimeUnit.MILLISECONDS).getKey(getKeyRequest { })
    }

    fun getAndIncreaseKey() = runBlocking {
        stub.withDeadlineAfter(100, TimeUnit.MILLISECONDS).getAndIncreaseKey(getKeyRequest { })
    }

    fun sendAsyncJoinResult(joinResult: JoinResult) = runBlocking {
        stub.sendAsyncJoinResult(joinResult)
    }

    @Synchronized
    fun recordFailed() = errorCount++

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyGeneratorClient

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "KeyGeneratorClient(name='$name')"
    }
}