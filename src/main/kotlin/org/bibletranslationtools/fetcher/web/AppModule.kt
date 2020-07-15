package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.Thymeleaf
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.response.respondText
import io.ktor.routing.Routing
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
    }
}
