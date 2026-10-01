package org.bibletranslationtools.fetcher.web

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.request.acceptLanguage
import io.ktor.server.request.uri
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.Thymeleaf
import java.util.Locale
import kotlin.concurrent.thread
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.di.appDependencyModule
import org.bibletranslationtools.fetcher.di.ext.CommonKoinExt.get
import org.bibletranslationtools.fetcher.impl.repository.LangType
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.SourceTextAccessor
import org.bibletranslationtools.fetcher.web.controllers.bookController
import org.bibletranslationtools.fetcher.web.controllers.chapterController
import org.bibletranslationtools.fetcher.web.controllers.homeController
import org.bibletranslationtools.fetcher.web.controllers.languageController
import org.bibletranslationtools.fetcher.web.controllers.productController
import org.koin.core.qualifier.named
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.lang.Exception
import java.lang.IllegalArgumentException
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage

const val MILLISECONDS_PER_MINUTE = 60000

private val logger = LoggerFactory.getLogger(Application::appModule::class.java)

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

    install(Koin) {
        modules(appDependencyModule)
    }

    scheduleCacheUpdate()
    scheduleLanguageCatalogUpdate()

    routing {
        // Static contents declared here
        staticResources("/static", "")

        intercept(ApplicationCallPipeline.Plugins) {
            if (!call.request.uri.startsWith("/static")) {
                contentLanguage = try {
                    Locale.LanguageRange.parse(call.request.acceptLanguage() ?: "en")
                } catch (ex: IllegalArgumentException) {
                    logger.warn("Invalid accept language header: ${call.request.acceptLanguage()}")
                    listOf(Locale.LanguageRange("en"))
                }
            }
        }

        // Application Routes - Controllers
        homeController()
        languageController()
        productController()
        bookController()
        chapterController()
    }
}

private fun scheduleCacheUpdate() {
    val envConfig: EnvironmentConfig = get()
    val sourceTextAccessor: SourceTextAccessor = get()

    thread(start = true, isDaemon = true, name = "cache-update") {
        val minutes = envConfig.CACHE_REFRESH_MINUTES.toLong()
        while (true) {
            Thread.sleep(MILLISECONDS_PER_MINUTE * minutes)
            logger.info("Updating cache...")
            try {
                sourceTextAccessor.update()
                logger.info("Cache updated!")
            } catch (e: Exception) {
                logger.error("An error occurred while updating the content cache.", e)
            }
        }
    }
}

private fun scheduleLanguageCatalogUpdate() {
    val envConfig: EnvironmentConfig = get()
    val languageCatalog: LanguageCatalog = get(named(LangType.ALL.name))

    thread(start = true, isDaemon = true, name = "language-catalog-update") {
        val minutes = envConfig.LANGUAGE_REFRESH_MINUTES.toLong()
        while (true) {
            Thread.sleep(MILLISECONDS_PER_MINUTE * minutes)
            logger.info("Updating language catalog...")
            try {
                languageCatalog.refresh()
                logger.info("Language catalog updated!")
            } catch (e: Exception) {
                logger.error("An error occurred while updating the language catalog.", e)
            }
        }
    }
}
