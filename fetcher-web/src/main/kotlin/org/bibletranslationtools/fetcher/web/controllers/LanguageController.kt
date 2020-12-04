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
                    resolver
                )
            )
        }
    }
    route("$GL_ROUTE/filter") {
        // Async request from client script
        get {
            val path = "/$GL_ROUTE"
            val searchQuery = call.request.queryParameters["keyword"]

            if (!searchQuery.isNullOrEmpty()) {
                call.respond(filterLanguages(searchQuery, path, resolver))
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
    route("$GL_ROUTE/load-more") {
        // Async request from the client script
        get {
            val path = "/$GL_ROUTE"
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
    resolver: DependencyResolver
): ThymeleafContent {
    val languageList = FetchLanguageViewData(
        resolver.languageRepository
    ).getViewDataList(path, resolver.contentCache)

    return ThymeleafContent(
        template = "languages",
        model = mapOf(
            "languageList" to languageList,
            "languagesNavUrl" to "#",
            "gatewayCount" to languageList.size
        ),
        locale = getPreferredLocale(contentLanguage, "languages")
    )
}

private fun filterLanguages(
    query: String,
    currentPath: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val resultLanguages = FetchLanguageViewData(resolver.languageRepository)
        .filterLanguages(query, currentPath, resolver.storageAccess)

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
        .loadMoreLanguages(path, resolver.storageAccess, currentIndex)

    return ThymeleafContent(
        template = "fragments/language_list",
        model = mapOf(
            "languageList" to moreLanguages
        )
    )
}