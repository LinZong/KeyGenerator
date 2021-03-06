package moe.nemesiss.keygenerator.loadbalancer

import io.grpc.StatusException
import moe.nemesiss.keygenerator.base.constant.GetKey
import moe.nemesiss.keygenerator.base.constant.Join
import moe.nemesiss.keygenerator.base.constant.KeyGeneratorGroupMode
import moe.nemesiss.keygenerator.base.constant.Pong
import moe.nemesiss.keygenerator.grpc.model.*
import moe.nemesiss.keygenerator.loadbalancer.exception.KeyGeneratorGroupException
import moe.nemesiss.keygenerator.loadbalancer.exception.KeyGeneratorGroupRebalancingException
import moe.nemesiss.keygenerator.loadbalancer.exception.SafeUpperBoundRoundRobinSelectorException
import moe.nemesiss.keygenerator.loadbalancer.repo.GroupMetaRepository
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private const val ClientErrorThreshold = 3
private const val WaitForRunningStateMaxSpinCount = 32

data class KeyGeneratorGroupMeta(val namespace: String, val groupId: String)

class KeyGeneratorGroup(
    private val repo: GroupMetaRepository,
    val namespace: String,
    val groupId: String,
    initGroup: Boolean = false
) {

    private val log = KotlinLogging.logger("KeyGeneratorGroup-$namespace-$groupId")

    private val mode = AtomicInteger(KeyGeneratorGroupMode.RUNNING)

    private val runningRequests = AtomicInteger(0)

    private val rrSelector = repo.loadSelector(initGroup)

    @Volatile
    private var epoch = repo.loadEpoch(initGroup)

    private val rebalanceThreadGroup = ThreadGroup("Rebalance-$namespace-$groupId")

    private val cleanerThreadGroup = ThreadGroup("Cleaner-$namespace-$groupId")


    val meta get() = KeyGeneratorGroupMeta(namespace, groupId)

    init {
        Thread(cleanerThreadGroup) {
            log.info { "Error client cleaner start!" }
            while (true) {
                Thread.sleep(10 * 1000L) // 10s
                val snapshot = rrSelector.elementSnapshot
                val errorClients = snapshot.filter { c -> c.errorCount >= ClientErrorThreshold }
                if (errorClients.isEmpty()) {
                    continue
                }
                runningRequests.incrementAndGet()
                try {
                    if (mode.get() != KeyGeneratorGroupMode.RUNNING) {
                        continue
                    }
                    rrSelector.removeElements(errorClients)
                    log.info { "clean ${errorClients.size} client(s)." }
                } finally {
                    runningRequests.decrementAndGet()
                }
            }
        }.start()
    }

    fun ping(pingRequest: PingRequest): PongResult {
        if (mode.get() != KeyGeneratorGroupMode.RUNNING) {
            return pongResult { code = Pong.NOT_IN_RUNNING_STATE }
        }
        if (pingRequest.epoch != epoch.epoch) {
            return pongResult { code = Pong.EPOCH_OUT_OF_SYNC }
        }
        return pongResult { code = Pong.OK }
    }

    @Throws(KeyGeneratorGroupException::class, KeyGeneratorGroupRebalancingException::class)
    fun getAndIncreaseKey(): Long {
        try {
            // ??????????????????
            runningRequests.incrementAndGet()

            for (v in 0 until WaitForRunningStateMaxSpinCount) {
                // ?????????????????????????????? ????????????RUNNING
                if (mode.get() != KeyGeneratorGroupMode.RUNNING) {
                    continue
                }

                // just take a snapshot.
                val maxRetry = rrSelector.elements.size
                for (w in 0 until maxRetry) {
                    // ????????????????????????
                    if (rrSelector.elements.isEmpty()) {
                        break
                    }
                    val client = rrSelector.next()
                    try {
                        val result = client.getAndIncreaseKey()
                        when (result.code) {
                            GetKey.OK -> return result.key
                            GetKey.NOT_IN_RUNNING_STATE -> {
                                log.warn { "group is in running state, but client: $client reports not." }
                                client.recordFailed()
                            }
                            GetKey.INTERNAL_ERROR -> {
                                log.warn { "client: $client has internal error." }
                                client.recordFailed()
                            }
                        }
                    } catch (se: StatusException) {
                        log.error(se) { "" }
                        client.recordFailed()
                    }
                }
                // ???????????????client, ???????????????
                throw KeyGeneratorGroupException("no available client!")
            }
            // ????????????????????? 32 ?????????????????????????????????REBALANCE????????????????????????
            throw KeyGeneratorGroupRebalancingException()
        } catch (se: SafeUpperBoundRoundRobinSelectorException) {
            // no client .
            throw KeyGeneratorGroupException("no available client!")
        } finally {
            runningRequests.decrementAndGet()
        }
    }

    fun join(joinRequest: JoinRequest): JoinResult {
        log.info { "Join request arrived: $joinRequest" }
        // ????????????????????????rebalance
        advanceMode(KeyGeneratorGroupMode.RUNNING, KeyGeneratorGroupMode.REBALANCING)
        // ??????rebalance????????????epoch

        while (runningRequests.get() > 0) {
            // wait until no running requests.
        }

        log.info { "Join request: $joinRequest begin." }

        if (joinRequest.epoch == epoch.epoch &&
            joinRequest.namespace == namespace &&
            joinRequest.groupId == groupId
        ) {
            log.info { "$joinRequest same epoch, namespace and groupId, go fastpath." }
            // epoch??????????????????????????????????????????
            synchronized(this) {
                rrSelector.addElement(KeyGeneratorClient(joinRequest.name, joinRequest.host, joinRequest.port))
            }
            advanceMode(KeyGeneratorGroupMode.REBALANCING, KeyGeneratorGroupMode.RUNNING)
            log.info { "$joinRequest fastpath end." }
            return joinResult {
                code = Join.JOIN_OK
                epoch = this@KeyGeneratorGroup.epoch.epoch
                key = 0
                step = this@KeyGeneratorGroup.epoch.machineCount
                namespace = this@KeyGeneratorGroup.namespace
                groupId = this@KeyGeneratorGroup.groupId
                ticket = joinRequest.ticket
            }
        } else {
            log.info { "$joinRequest go slow path." }
            Thread(ThreadGroup(rebalanceThreadGroup, joinRequest.name)) {
                log.info { "Begin rebalance for client: ${joinRequest.name}" }

                rebalanceLocked(joinRequest)

                advanceMode(KeyGeneratorGroupMode.REBALANCING, KeyGeneratorGroupMode.RUNNING)
                log.info { "Rebalance for client: ${joinRequest.name} is finished." }
            }.start()
            return joinResult {
                code = Join.WAITING_REBALANCED
                ticket = joinRequest.ticket
            }
        }
    }

    private fun rebalanceLocked(joinRequest: JoinRequest) {

        val rebalanceAffectedClients = rrSelector
            .elements
            .filter { c ->
                c.name != joinRequest.name
            }


        // ??????epoch?????????
        val nextMachineCount = rebalanceAffectedClients.size + 1

        // ??????epoch????????????
        val nextBase = epoch.maxBaseValue + rrSelector.round * epoch.machineCount

        // ??????epoch???????????????
        val nextMaxBase = nextBase + nextMachineCount - 1

        // ???????????????????????????epoch
        val nextEpoch = Epoch(epoch.epoch + 1, nextMachineCount, nextMaxBase)

        // ???????????????????????????clients
        val nextClients =
            (rebalanceAffectedClients + KeyGeneratorClient(
                joinRequest.name,
                joinRequest.host,
                joinRequest.port
            )).toMutableList()


        // ????????????
        repo.saveEpoch(nextEpoch)
        epoch = nextEpoch

        // ?????????????????????
        val clientBaseValues = (nextBase..nextMaxBase).toList()
        val broadcastFailedClients = arrayListOf<KeyGeneratorClient>()

        for ((index, client) in nextClients.withIndex()) {
            val base = clientBaseValues[index]
            val code = if (client.name == joinRequest.name) Join.JOIN_OK else Join.REBALANCE_NOTIFY
            val ticket = if (client.name == joinRequest.name) joinRequest.ticket else ""
            try {
                client.sendAsyncJoinResult(joinResult {
                    this.code = code
                    this.epoch = nextEpoch.epoch
                    this.key = base
                    this.step = nextMachineCount
                    this.namespace = this@KeyGeneratorGroup.namespace
                    this.groupId = this@KeyGeneratorGroup.groupId
                    this.ticket = ticket
                })
            } catch (se: StatusException) {
                log.error(se) { "broadcast to client $client failed!" }
                broadcastFailedClients += client
            }
        }

        // ??????broadcast??????????????????
        nextClients.removeAll(broadcastFailedClients)
        // ?????????clients, ?????????????????????round???maxround
        rrSelector.elements = nextClients
    }

    private fun advanceMode(curr: Int, next: Int) {
        while (!mode.compareAndSet(curr, next));
    }
}