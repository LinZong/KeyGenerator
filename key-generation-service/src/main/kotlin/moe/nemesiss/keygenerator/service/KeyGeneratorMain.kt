package moe.nemesiss.keygenerator.service

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.default
import io.grpc.ServerBuilder
import moe.nemesiss.keygenerator.grpc.model.*
import moe.nemesiss.keygenerator.service.repo.ClusterInfoRepository
import moe.nemesiss.keygenerator.service.repo.KeyRepository
import moe.nemesiss.keygenerator.service.state.StateProvider
import moe.nemesiss.keygenerator.service.state.StateRunner
import moe.nemesiss.keygenerator.service.state.join.JoinStateContext
import moe.nemesiss.keygenerator.service.state.join.RequestJoinState
import moe.nemesiss.keygenerator.service.timertask.HeartbeatJobCallback
import moe.nemesiss.keygenerator.service.util.StickyObservable
import mu.KotlinLogging
import org.quartz.impl.StdSchedulerFactory
import java.io.Closeable
import java.io.File

private class BootArgs(parser: ArgParser) {
    val namespace: String by parser.storing("join namespace")
    val dataDir: String by parser.storing("working dir path").default("./key-generator-data")
    val host: String by parser.storing("communication endpoint host").default("localhost")
    val port: Int by parser.storing("communication endpoint port") { toInt() }.default(12346)
    val loadBalancerHost: String by parser.storing("load balancer communication host").default("localhost")
    val loadBalancerPort: Int by parser.storing("load balancer communication port") { toInt() }.default(12345)
    val name: String by parser.storing("instance name")
    val nodeConfig
        get() = NodeConfig(
            namespace,
            name,
            File(dataDir, namespace),
            host,
            port,
            loadBalancerHost,
            loadBalancerPort
        )
}


class KeyGeneratorMain(private val nodeConfig: NodeConfig) : KeyGeneratorNodeGrpcKt.KeyGeneratorNodeCoroutineImplBase(),
    HeartbeatJobCallback, Closeable {


    private val clusterInfoRepository = ClusterInfoRepository(nodeConfig.dataDir)
    private val clusterInfo = clusterInfoRepository.load()

    private val loadBalancerClient = LoadBalancerClientKt(nodeConfig)
    private val keyRepository = KeyRepository(nodeConfig)
    private val keyGenerator = keyRepository.load()
    private val server = ServerBuilder.forPort(nodeConfig.port).addService(this).build()
    private val scheduler = StdSchedulerFactory().scheduler

    private val joinResultObservable = StickyObservable<JoinResult>()

    init {
        log.info { nodeConfig }
    }

    fun start() {
        // 走初始化流程
        joinCluster()
    }

    private fun joinCluster() {
        val runner = StateRunner(StateProvider(RequestJoinState()))
        val context = JoinStateContext(
            nodeConfig,
            clusterInfo,
            loadBalancerClient,
            scheduler,
            this,
            joinResultObservable
        )
        runner.run(context)

    }

    override fun close() {
        log.info { "Shutting down..." }
        clusterInfoRepository.save(clusterInfo)
    }

    override suspend fun getKey(request: GetKeyRequest): GetKeyResult {
        return super.getKey(request)
    }

    override suspend fun getAndIncreaseKey(request: GetKeyRequest): GetKeyResult {
        return super.getAndIncreaseKey(request)
    }

    override suspend fun sendAsyncJoinResult(request: JoinResult): Empty {
        joinResultObservable.publish(request)
        return empty {}
    }

    companion object {
        private val log = KotlinLogging.logger("KeyGenerator")

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val config = ArgParser(args).parseInto(::BootArgs).nodeConfig
                val kg = KeyGeneratorMain(config).apply { start() }
                Runtime.getRuntime().addShutdownHook(Thread { kg.close() })
                log.warn { "KeyGenerator instance is running!" }
                readLine()
            } catch (help: ShowHelpException) {
                // no op, just exit program.
                help.printAndExit()
            }
        }
    }

    override fun onHeartbeatSuccess() {

    }

    override fun onHeartbeatFailed(code: Int, e: Throwable?) {

    }
}