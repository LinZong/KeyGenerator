package moe.nemesiss.keygenerator.loadbalancer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KeyGeneratorLoadBalancerApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<KeyGeneratorLoadBalancerApplication>(*args)
        }
    }
}