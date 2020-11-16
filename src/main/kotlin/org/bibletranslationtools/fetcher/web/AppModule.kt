package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.Thymeleaf
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.acceptLanguage
import io.ktor.request.uri
import io.ktor.routing.Routing
import io.ktor.routing.routing
import java.util.Locale
import kotlin.concurrent.thread
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.web.controllers.bookController
import org.bibletranslationtools.fetcher.web.controllers.chapterController
import org.bibletranslationtools.fetcher.web.controllers.homeController
import org.bibletranslationtools.fetcher.web.controllers.languageController
import org.bibletranslationtools.fetcher.web.controllers.productController
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

const val CACHE_REFRESH_RATE_PER_HOUR = 3600000

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
    install(Routing) {
        val resolver = DependencyResolver
        scheduleCacheUpdate()
        routing {
            // Static contents declared here
            static("static") {
                resources("css")
                resources("js")
                static("fonts") {
                    resources("fonts")
                }
                static("img") {
                    resources("img")
                }
            }
            intercept(ApplicationCallPipeline.Setup) {
                if (!call.request.uri.startsWith("/static")) {
                    contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
                }
            }
            // Application Routes - Controllers
            homeController()
            languageController(resolver)
            productController(resolver)
            bookController(resolver)
            chapterController(resolver)
        }
    }
}

private fun scheduleCacheUpdate() {
    thread(start = true, isDaemon = true) {
        val hours = System.getenv("CACHE_REFRESH_TIME_HRS").toLong()
        while (true) {
            Thread.sleep(CACHE_REFRESH_RATE_PER_HOUR * hours)
            DependencyResolver.contentCache.update()
        }
    }
}
