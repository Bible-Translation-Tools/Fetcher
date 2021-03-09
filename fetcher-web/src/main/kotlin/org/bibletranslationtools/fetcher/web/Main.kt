package org.bibletranslationtools.fetcher.web

import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

const val PORT = 8080
const val CLASS_LOADER = "Fetcher"

fun main(args: Array<String>) {
    EnvironmentConfigCheck.run()

    embeddedServer(
        factory = Netty,
        port = PORT,
        watchPaths = listOf(CLASS_LOADER),
        module = Application::appModule
    ).start(wait = true)
}
