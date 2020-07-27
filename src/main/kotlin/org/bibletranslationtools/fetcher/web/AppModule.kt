package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.Thymeleaf
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.content.files
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.http.content.staticRootFolder
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

fun Application.appModule() {
    install(DefaultHeaders)
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }
    install(CallLogging)
    intercept(ApplicationCallPipeline.Setup) {
        // display error page here and terminate the pipeline if fatal exception occurs
    }
    install(Routing) {
        val resolver = DependencyResolver
        routing {
            // Static contents declared here
            static("static") {
                resources("css")
                resources("js")
                static("img") {
                    resources("img")
                }
            }
            static("download") {
                staticRootFolder = resolver.directoryProvider.getContentRoot()
                files(".")
            }
            // Application Route
            root(resolver)
        }
    }
}
