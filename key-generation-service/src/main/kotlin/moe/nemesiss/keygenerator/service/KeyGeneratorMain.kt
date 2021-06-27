package moe.nemesiss.keygenerator.service

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.default
import io.grpc.ServerBuilder
import moe.nemesiss.keygenerator.base.constant.GetKey
import moe.nemesiss.keygenerator.base.constant.Join
import moe.nemesiss.keygenerator.base.constant.NodeState
import moe.nemesiss.keygenerator.base.constant.Pong
import moe.nemesiss.keygenerator.grpc.model.*
import moe.nemesiss.keygenerator.service.repo.ClusterInfoRepository
import moe.nemesiss.keygenerator.service.repo.KeyRepository
import moe.nemesiss.keygenerator.service.state.StateProvider
import moe.nemesiss.keygenerator.service.state.StateRunner
import moe.nemesiss.keygenerator.service.state.join.JoinStateContext
import moe.nemesiss.keygenerator.service.state.join.RequestJoinState
import moe.nemesiss.keygenerator.service.timertask.HeartbeatJobCallback
import moe.nemesiss.keygenerator.service.util.Observer
import moe.nemesiss.keygenerator.service.util.StickyObservable
import mu.KotlinLogging
import org.quartz.impl.StdSchedulerFactory
import java.io.Closeable
import java.io.File
import kotlin.system.exitProcess

private class BootArgs(parser: ArgParser) {
    val namespace: String by parser.storing("join namespace")
    val dataDir: String by parser.storing("working dir path").default("./key-generator-data")
    val host: String by parser.storing("communication endpoint host").default("localhost")
    val port: Int by parser.storing("communication endpoint port") { toInt() }.default(12346)
    val loadBalancerHost: String by parser.storing("load balancer communication host").default("localhost")
    val loadBalancerPort: Int by parser.storing("load balancer communication port") { toInt() }.default(50052)
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

    private inner class KeyGeneratorGroupRebalanceObserver : Observer<JoinResult> {
        override fun onNext(value: JoinResult) {
            if (clusterInfo.state == NodeState.RUNNING && value.code == Join.REBALANCE_NOTIFY) {
                log.info { "Key changed to: ${value.key}, step changed to ${value.step}" }
                clusterInfo.update(value)
                keyGenerator.key = value.key
            }
        }
    }

    // singleton here.

    private val clusterInfoRepository = ClusterInfoRepository(nodeConfig.dataDir)
    private val clusterInfo = clusterInfoRepository.load()

    private val loadBalancerClient = LoadBalancerClientKt(nodeConfig)
    private val keyRepository = KeyRepository(nodeConfig)

    private val keyGenerator = keyRepository.load()
    private val server = ServerBuilder.forPort(nodeConfig.port).addService(this).build()
    private val scheduler = StdSchedulerFactory().scheduler.apply { start() }

    private val joinResultObservable = StickyObservable<JoinResult>()

    init {
        log.info { nodeConfig }
        joinResultObservable += KeyGeneratorGroupRebalanceObserver()
    }

    fun start() {
        // 走初始化流程
        server.start()
        joinCluster()
        log.info { "KeyGenerator is started." }
    }

    private fun joinCluster() {
        val runner = StateRunner(StateProvider(RequestJoinState()))
        val context = JoinStateContext(
            nodeConfig,
            clusterInfo,
            loadBalancerClient,
            scheduler,
            this,
            joinResultObservable,
            keyGenerator
        )
        runner.run(context)

        if (clusterInfo.state == NodeState.FATAL) {
            log.error { "Join to cluster failed. Exit." }
            gracefullyExit(1)
        }
    }

    private fun gracefullyExit(code: Int) {
        close()
        exitProcess(code)
    }

    override fun close() {
        log.info { "Shutting down..." }
        clusterInfoRepository.save(clusterInfo)
        server.shutdown()
        server.awaitTermination()
    }

    override suspend fun getKey(request: GetKeyRequest): GetKeyResult {
        if (clusterInfo.state != NodeState.RUNNING) {
            return getKeyResult {
                code = GetKey.NOT_IN_RUNNING_STATE
            }
        }
        return kotlin.runCatching {
            getKeyResult {
                code = GetKey.OK; key = keyGenerator.key
            }
        }.getOrElse { getKeyResult { code = GetKey.INTERNAL_ERROR } }
    }

    override suspend fun getAndIncreaseKey(request: GetKeyRequest): GetKeyResult {
        if (clusterInfo.state != NodeState.RUNNING) {
            return getKeyResult {
                code = GetKey.NOT_IN_RUNNING_STATE
            }
        }
        return kotlin.runCatching {
            getKeyResult {
                code = GetKey.OK; key = keyGenerator.getAndIncrease(clusterInfo.step)
            }
        }.getOrElse { getKeyResult { code = GetKey.INTERNAL_ERROR } }
    }

    override suspend fun sendAsyncJoinResult(request: JoinResult): Empty {
        joinResultObservable.publish(request)
        return empty {}
    }


    override fun onHeartbeatSuccess() {
        log.info { "Heartbeat ok!" }
    }

    override fun onHeartbeatFailed(code: Int, e: Throwable?) {
        when (code) {
            Pong.EPOCH_OUT_OF_SYNC -> {
                log.error { "epoch is out of state. try rejoin." }
                joinCluster()
            }
            Pong.NOT_JOINED -> {
                log.error { "not joined into cluster. try rejoin." }
                joinCluster()
            }
            Pong.EXCEPTION_THROWN -> {
                log.error(e) { "heartbeat with exception." }
            }
            Pong.TIME_OUT -> {
                log.warn { "heartbeat timeout!" }
            }
            Pong.NOT_IN_RUNNING_STATE -> {
                log.warn { "Omit heartbeat sending in not running state." }
                // TODO should we cancel scheduled heartbeat job?
            }
        }
    }

    companion object {
        private val log = KotlinLogging.logger("KeyGenerator")

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val config = ArgParser(args)
                    .parseInto(::BootArgs)
                    .nodeConfig
                    .apply { validate() }
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
}