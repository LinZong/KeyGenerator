package moe.nemesiss.keygenerator.loadbalancer

import moe.nemesiss.keygenerator.base.constant.Join
import moe.nemesiss.keygenerator.base.constant.Pong
import moe.nemesiss.keygenerator.grpc.model.*
import moe.nemesiss.keygenerator.loadbalancer.exception.KeyGeneratorGroupNotFoundException
import moe.nemesiss.keygenerator.loadbalancer.repo.MetaRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class KeyGeneratorGroupCoordinatorServer(private val coordinator: KeyGeneratorGroupCoordinator) :
    LoadBalancerGrpcKt.LoadBalancerCoroutineImplBase() {

    private val log = KotlinLogging.logger("KeyGeneratorGroupCoordinatorServer")

    init {
        log.info { "Grpc server started!" }
    }

    override suspend fun join(request: JoinRequest): JoinResult {
        return coordinator.joinGroup(request)
    }

    override suspend fun ping(request: PingRequest): PongResult {
        return coordinator.pingGroup(request)
    }
}

@Component
class KeyGeneratorGroupCoordinator(private val repo: MetaRepository) {

    private val groups: ConcurrentHashMap<String, KeyGeneratorGroup> = repo.loadGroups()

    fun joinGroup(request: JoinRequest): JoinResult {
        val group = groups[request.namespace] ?: return joinResult { code = Join.NO_NAMESPACE }
        if (request.groupId.isNotEmpty() && group.groupId != request.groupId) {
            return joinResult {
                code = Join.INVALID_GROUP_ID
            }
        }
        return group.join(request)
    }

    fun pingGroup(pingRequest: PingRequest): PongResult {
        val group = groups[pingRequest.namespace] ?: return pongResult { code = Pong.NOT_JOINED }
        if (group.groupId != pingRequest.groupId) {
            return pongResult { code = Pong.NOT_JOINED }
        }
        return group.ping(pingRequest)
    }

    fun createGroup(namespace: String) {
        if (groups.containsKey(namespace)) {
            throw IllegalArgumentException("$namespace already exists.")
        }

        val group = KeyGeneratorGroup(
            repo.getGroupRepository(namespace).apply { ensureDataDirCreated() },
            namespace,
            UUID.randomUUID().toString(),
            true
        )
        groups[namespace] = group
        repo.saveGroups(groups)
        // return ok
    }

    fun nextKey(namespace: String): Long {
        if (!groups.containsKey(namespace)) {
            throw KeyGeneratorGroupNotFoundException("no namespace $namespace.")
        }
        return groups[namespace]!!.getAndIncreaseKey()
    }
}