package moe.nemesiss.keygenerator.loadbalancer.controller

import moe.nemesiss.keygenerator.loadbalancer.KeyGeneratorGroupCoordinator
import moe.nemesiss.keygenerator.loadbalancer.exception.KeyGeneratorGroupNotFoundException
import moe.nemesiss.keygenerator.loadbalancer.exception.KeyGeneratorGroupRebalancingException
import moe.nemesiss.keygenerator.loadbalancer.model.CreateGroupRequest
import moe.nemesiss.keygenerator.loadbalancer.model.CreateGroupResponse
import moe.nemesiss.keygenerator.loadbalancer.model.DataResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/key")
class KeyGenerationController {

    private val log = KotlinLogging.logger("KeyGenerationController")

    @Autowired
    lateinit var coordinator: KeyGeneratorGroupCoordinator

    @PostMapping("/createGroup")
    fun createGroup(@RequestBody request: CreateGroupRequest): CreateGroupResponse {
        return try {
            coordinator.createGroup(request.namespace)
            CreateGroupResponse(0)
        } catch (e: IllegalArgumentException) {
            CreateGroupResponse(-1, "namespace already exists.")
        } catch (e: Throwable) {
            log.error(e) { "" }
            CreateGroupResponse(-2, "unknown exception.")
        }
    }

    @GetMapping("/{namespace}")
    fun nextKey(@PathVariable("namespace") namespace: String): DataResponse<Long> {
        return try {
            DataResponse(0, null, coordinator.nextKey(namespace))
        } catch (e: KeyGeneratorGroupNotFoundException) {
            DataResponse(-1, "no namespace: $namespace")
        } catch (e: KeyGeneratorGroupRebalancingException) {
            DataResponse(-2, "rebalancing.")
        } catch (e: Throwable) {
            log.error(e) { "error!!!" }
            DataResponse(-3, "unknown exception.")
        }
    }
}