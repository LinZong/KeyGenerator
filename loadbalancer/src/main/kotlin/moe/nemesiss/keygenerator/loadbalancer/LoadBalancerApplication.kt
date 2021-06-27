package moe.nemesiss.keygenerator.loadbalancer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LoadBalancerApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<LoadBalancerApplication>(*args)
        }
    }
}