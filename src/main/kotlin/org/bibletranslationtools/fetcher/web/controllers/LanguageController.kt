package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import java.util.Locale
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchLanguageViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.normalizeUrl

fun Routing.languageController(resolver: DependencyResolver) {
    route(GL_ROUTE) {
        get {
            // languages page
            val path = normalizeUrl(call.request.path())
            call.respond(
                gatewayLanguagesView(
                    path,
                    resolver,
                    contentLanguage
                )
            )
        }
    }
}

private fun gatewayLanguagesView(
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val model = FetchLanguageViewData(resolver.languageRepository)
    return ThymeleafContent(
        template = "languages",
        model = mapOf(
            "languageList" to model.getListViewData(path),
            "languagesNavUrl" to "#"
        ),
        locale = getPreferredLocale(contentLanguage, "languages")
    )
}
