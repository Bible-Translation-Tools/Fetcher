package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
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
import java.lang.NumberFormatException

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
        // async request from client
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
    route("$HL_ROUTE/filter") {
        // async request from client
        get {
            val path = "/$HL_ROUTE"
            val searchQuery = call.request.queryParameters["search"]

            if (!searchQuery.isNullOrEmpty()) {
                call.respond(filterHeartLanguages(searchQuery, path, resolver))
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
    route("$HL_ROUTE/load-more") {
        // async request from the client
        get {
            val path = "/$HL_ROUTE"
            val index = try {
                call.request.queryParameters["index"]?.toInt()
            } catch (ex: NumberFormatException) {
                null
            }

            if (index == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                call.respond(loadMore(index, path, resolver))
            }
        }
    }
}

private fun languagesView(
    path: String,
    isGateway: Boolean,
    resolver: DependencyResolver
): ThymeleafContent {
    val model = FetchLanguageViewData(resolver.languageRepository)
    val languageList = if (isGateway) {
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

private fun filterHeartLanguages(
    query: String,
    currentPath: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val resultLanguages = FetchLanguageViewData(resolver.languageRepository)
        .filterHeartLanguages(query, currentPath, resolver.storageAccess)

    return ThymeleafContent(
        template = "fragments/language_list",
        model = mapOf(
            "languageList" to resultLanguages
        )
    )
}

private fun loadMore(
    currentIndex: Int,
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val moreLanguages = FetchLanguageViewData(resolver.languageRepository)
        .getHLViewDataList(path, resolver.storageAccess, currentIndex)

    return ThymeleafContent(
        template = "fragments/language_list",
        model = mapOf(
            "languageList" to moreLanguages
        )
    )
}