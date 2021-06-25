package moe.nemesiss.keygenerator.service

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.default
import java.io.File

class KeyGenerator {

    private class BootArgs(parser: ArgParser) {
        val dataDir: String by parser.storing("working dir path").default("./key-generator-data")
        val host: String by parser.storing("communication endpoint host").default("localhost")
        val port: Int by parser.storing("communication endpoint port") { toInt() }.default(12346)
        val loadBalancerHost: String by parser.storing("load balancer communication host").default("localhost")
        val loadBalancerPort: Int by parser.storing("load balancer communication port") { toInt() }.default(12345)

        val nodeConfig get() = NodeConfig(File(dataDir), host, port, loadBalancerHost, loadBalancerPort)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val nodeConfig = ArgParser(args)
                    .parseInto(::BootArgs)
                    .nodeConfig
                    .apply { validate() }

            } catch (help: ShowHelpException) {
                // no op, just exit program.
                help.printAndExit()
            }
        }
    }
}