package moe.nemesiss.keygenerator.service

import java.io.File
import java.net.Inet4Address


class NodeConfig(
    val namespace: String,
    val name: String,
    val dataDir: File,
    val host: String,
    val port: Int,
    val loadbalancerHost: String,
    val loadBalancerPort: Int
) {
    fun validate() {
        validateDataDir()
        validateHostAndPort()
        validateInstanceName()
    }

    private fun validateInstanceName() {
        if (name.isBlank()) {
            throw IllegalArgumentException("instance name should not be blank!")
        }
    }

    private fun validateDataDir() {
        dataDir.mkdirs()
        if (!(dataDir.exists() && dataDir.isDirectory))
            throw IllegalArgumentException("data dir is illegal. must be a directory and r/w available!")
    }

    private fun validateHostAndPort() {
        Inet4Address.getByName(host)
        Inet4Address.getByName(loadbalancerHost)
        if (port !in 0..65535) {
            throw IllegalArgumentException("port of self address is illegal, should be in [0, 65535].")
        }
        if (loadBalancerPort !in 0..65535) {
            throw IllegalArgumentException("port of load balancer is illegal, should be in [0, 65535].")
        }
    }

    override fun toString() = """
        namespace: [$namespace]
        data dir: [${dataDir.absolutePath}]
        node endpoint: [$host:$port]
        load balancer endpoint: [$loadbalancerHost:$loadBalancerPort]
    """
}