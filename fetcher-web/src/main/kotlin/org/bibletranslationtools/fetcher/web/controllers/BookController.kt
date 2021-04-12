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
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.LANGUAGE_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.PRODUCT_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.errorPage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.normalizeUrl
import org.bibletranslationtools.fetcher.web.controllers.utils.validator

fun Routing.bookController(resolver: DependencyResolver) {
    route("/$GL_ROUTE/{$LANGUAGE_PARAM_KEY}/{$PRODUCT_PARAM_KEY}") {
        get {
            // books page
            val path = normalizeUrl(call.request.path())
            val params = UrlParameters(
                languageCode = call.parameters[LANGUAGE_PARAM_KEY],
                productSlug = call.parameters[PRODUCT_PARAM_KEY]
            )

            call.respond(
                booksView(params, path, resolver)
            )
        }
    }
}

private fun booksView(
    params: UrlParameters,
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    if (
        !validator.isLanguageCodeValid(params.languageCode) ||
        !validator.isProductSlugValid(params.productSlug)
    ) {
        return errorPage(
            "invalid_route_parameter",
            "invalid_route_parameter_message",
            HttpStatusCode.NotFound
        )
    }
    val language = resolver.languageRepository.getLanguage(params.languageCode)!!
    val product = resolver.productCatalog.getProduct(params.productSlug)!!

    val bookViewData = FetchBookViewData(
        resolver.environmentConfig,
        resolver.bookRepository,
        resolver.storageAccess,
        language,
        product
    ).getViewDataList(path, resolver.contentCache, language.isGateway)

    return ThymeleafContent(
        template = "books",
        model = mapOf(
            "bookList" to bookViewData,
            "languagesNavTitle" to language.localizedName,
            "languagesNavUrl" to "/$GL_ROUTE",
            "fileTypesNavTitle" to product.titleKey,
            "fileTypesNavUrl" to "/$GL_ROUTE/${params.languageCode}",
            "booksNavUrl" to "#"
        ),
        locale = getPreferredLocale(contentLanguage, "books")
    )
}
