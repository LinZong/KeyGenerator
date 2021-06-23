package moe.nemesiss.keygenerator.loadbalancer.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
class DashboardController {

    @GetMapping("/greeting")
    fun greeting() = "Hello, KeyGenerator LoadBalancer!"
}