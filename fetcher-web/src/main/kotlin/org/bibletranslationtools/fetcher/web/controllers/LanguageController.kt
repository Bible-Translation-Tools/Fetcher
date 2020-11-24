package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchLanguageViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.HL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.normalizeUrl

fun Routing.languageController(resolver: DependencyResolver) {
    route(GL_ROUTE) {
        get {
            // languages page
            val path = normalizeUrl(call.request.path())
            call.respond(
                languagesView(
                    path,
                    true,
                    resolver
                )
            )
        }
    }

    route(HL_ROUTE) {
        get {
            val path = normalizeUrl(call.request.path())
            call.respond(
                languagesView(
                    path,
                    false,
                    resolver
                )
            )
        }
    }
}

private fun languagesView(
    path: String,
    isGateway: Boolean,
    resolver: DependencyResolver
): ThymeleafContent {
    val model = FetchLanguageViewData(resolver.languageRepository)
    val languageList = if(isGateway) {
        model.getGLViewDataList(path, resolver.contentCache)
    } else {
        model.getHLViewDataList(path, resolver.storageAccess)
    }

    return ThymeleafContent(
        template = "languages",
        model = mapOf(
            "languageList" to languageList,
            "languagesNavUrl" to "#",
            "isGateway" to isGateway
        ),
        locale = getPreferredLocale(contentLanguage, "languages")
    )
}
