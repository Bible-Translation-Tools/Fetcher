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
import org.bibletranslationtools.fetcher.usecase.FetchProductViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.LANGUAGE_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.errorPage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.normalizeUrl
import org.bibletranslationtools.fetcher.web.controllers.utils.validator

fun Routing.productController(resolver: DependencyResolver) {
    route("/$GL_ROUTE/{$LANGUAGE_PARAM_KEY}") {
        get {
            // products page
            val path = normalizeUrl(call.request.path())
            val params = UrlParameters(
                languageCode = call.parameters[LANGUAGE_PARAM_KEY]
            )
            call.respond(
                productsView(params.languageCode, path, resolver)
            )
        }
    }
}

private fun productsView(
    languageCode: String,
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    if (!validator.isLanguageCodeValid(languageCode)) {
        return errorPage(
            "invalid_route_parameter",
            "invalid_route_parameter_message",
            HttpStatusCode.NotFound
        )
    }

    val language = resolver.languageRepository.getLanguage(languageCode)!!
    val productList = FetchProductViewData(
        resolver.productCatalog, languageCode
    ).getListViewData(path, resolver.contentCache, language.isGateway)

    return ThymeleafContent(
        template = "products",
        model = mapOf(
            "productList" to productList,
            "languagesNavTitle" to language.localizedName,
            "languagesNavUrl" to "/$GL_ROUTE",
            "fileTypesNavUrl" to "#"
        ),
        locale = getPreferredLocale(contentLanguage, "products")
    )
}
