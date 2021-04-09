package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.usecase.FetchProductViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.LANGUAGE_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.errorPage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.normalizeUrl
import org.bibletranslationtools.fetcher.web.controllers.utils.validator
import org.koin.java.KoinJavaComponent.get

fun Routing.productController() {
    route("/$GL_ROUTE/{$LANGUAGE_PARAM_KEY}") {
//        val productCatalog by inject<ProductCatalog>()
        get {
            // products page
            val path = normalizeUrl(call.request.path())
            val params = UrlParameters(
                languageCode = call.parameters[LANGUAGE_PARAM_KEY]
            )
            call.respond(
                productsView(params.languageCode, path)
            )
        }
    }
}

private fun productsView(
    languageCode: String,
    path: String
): ThymeleafContent {
    if (!validator.isLanguageCodeValid(languageCode)) {
        return errorPage(
            "invalid_route_parameter",
            "invalid_route_parameter_message",
            HttpStatusCode.NotFound
        )
    }

    val language = get(LanguageRepository::class.java).getLanguage(languageCode)!!
    val contentCache = get(ContentCacheAccessor::class.java)
    val productList = FetchProductViewData(
        get(ProductCatalog::class.java),
        languageCode
    ).getListViewData(path, contentCache, language.isGateway)

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
