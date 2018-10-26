package com.mhfs.voc.server

import com.mhfs.voc.ManifestVersionProvider
import com.mhfs.voc.VocabularyService
import org.glassfish.grizzly.http.server.CLStaticHttpHandler
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.glassfish.jersey.server.ResourceConfig
import picocli.CommandLine
import java.io.File
import java.lang.NullPointerException
import java.net.URI
import java.sql.DriverManager
import kotlin.concurrent.thread


@CommandLine.Command(name = "vocserv", description = ["A vocabulary server tool."], mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider::class)
class ServerStarter(defaultLocation: String = "voc_db", loadLocation: File? = null,
                    val httpServer: Boolean = false): Runnable {

    @CommandLine.Option(description = ["Points to a file with data to load."], names = ["--load"], paramLabel = "FILE")
    private var dbLocation: String = defaultLocation

    private lateinit var apiProvider: ServerAPIProvider

    override fun run() {
        val connection = DriverManager.getConnection("jdbc:h2:$dbLocation") ?: throw NullPointerException("DB not accessible.")
        apiProvider = ServerAPIProvider(DBAccess("h2", connection), arrayOf(0, 1, 3, 7, 14, 60))

        if (httpServer) {
            val baseURI = "http://localhost:8080/api"

            val config = ResourceConfig()
            config.registerInstances(apiProvider)

            val server = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseURI), config)

            val staticResources = CLStaticHttpHandler(this::class.java.classLoader, "/web/")
            server.serverConfiguration.addHttpHandler(staticResources, "/")

            Runtime.getRuntime().addShutdownHook(thread(start = false) { server.shutdown() })
        }
    }

    fun getService(): VocabularyService = apiProvider
}

fun main(args: Array<String>) {
    CommandLine.run(ServerStarter(httpServer = true), *args)
}