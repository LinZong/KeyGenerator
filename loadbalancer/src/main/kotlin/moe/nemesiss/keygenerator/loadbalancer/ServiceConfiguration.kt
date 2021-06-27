package moe.nemesiss.keygenerator.loadbalancer

import io.grpc.Server
import io.grpc.ServerBuilder
import moe.nemesiss.keygenerator.loadbalancer.repo.MetaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class ServiceConfiguration {

    @Bean
    fun metaRepository(@Value("\${loadbalancer.data-dir}") dataDir: String): MetaRepository {
        val dataDirFile = File(dataDir)
        (dataDirFile.exists() || dataDirFile.mkdirs())
        return MetaRepository(dataDirFile)
    }

    @Bean(destroyMethod = "shutdown")
    fun loadBalancerGrpcServer(@Value("\${loadbalancer.grpc.port}") grpcPort: Int, coordinator: KeyGeneratorGroupCoordinator): Server {
        val server = ServerBuilder.forPort(grpcPort)
            .addService(KeyGeneratorGroupCoordinatorServer(coordinator))
            .build()
        server.start()
        return server
    }
}