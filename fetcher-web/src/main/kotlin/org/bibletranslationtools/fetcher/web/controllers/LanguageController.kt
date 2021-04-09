package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import java.lang.NumberFormatException
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchLanguageViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.normalizeUrl
import org.koin.java.KoinJavaComponent.get

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
        searchFilter(resolver)
    }
    route("$GL_ROUTE/load-more") {
        // Async request from client script
        loadMoreLanguages(resolver)
    }
}

private fun Route.searchFilter(resolver: DependencyResolver) {
    get {
        val path = "/$GL_ROUTE"
        val searchQuery = call.request.queryParameters["keyword"]
        val index = try {
            call.request.queryParameters["index"]?.toInt()
        } catch (ex: NumberFormatException) {
            null
        }

        return@get when {
            !searchQuery.isNullOrEmpty() && index != null -> {
                call.respond(filterLanguages(searchQuery, path, resolver, index))
            }
            !searchQuery.isNullOrEmpty() -> {
                call.respond(filterLanguages(searchQuery, path, resolver))
            }
            else -> {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun Route.loadMoreLanguages(resolver: DependencyResolver) {
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

private fun languagesView(
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val languageList = FetchLanguageViewData(
        get(LanguageRepository::class.java),
        resolver.contentCache,
        get(StorageAccess::class.java)
    ).getViewDataList(path)

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
    resolver: DependencyResolver,
    currentIndex: Int = 0
): ThymeleafContent {
    val resultLanguages = FetchLanguageViewData(
        get(LanguageRepository::class.java),
        resolver.contentCache,
        get(StorageAccess::class.java)
    ).filterLanguages(query, currentPath, currentIndex)

    val isLastResult = resultLanguages.size < FetchLanguageViewData.DISPLAY_ITEMS_LIMIT

    return ThymeleafContent(
        template = "fragments/language_list",
        model = mapOf(
            "languageList" to resultLanguages,
            "isLastResult" to isLastResult
        )
    )
}

private fun loadMore(
    currentIndex: Int,
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val moreLanguages = FetchLanguageViewData(
        get(LanguageRepository::class.java),
        resolver.contentCache,
        get(StorageAccess::class.java)
    ).loadMoreLanguages(path, currentIndex)

    val isLastResult = moreLanguages.size < FetchLanguageViewData.DISPLAY_ITEMS_LIMIT

    return ThymeleafContent(
        template = "fragments/language_list",
        model = mapOf(
            "languageList" to moreLanguages,
            "isLastResult" to isLastResult
        )
    )
}
