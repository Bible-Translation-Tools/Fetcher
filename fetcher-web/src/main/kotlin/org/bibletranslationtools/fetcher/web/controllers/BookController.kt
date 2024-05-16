package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.di.ext.CommonKoinExt.get
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.resourceIdByLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.LANGUAGE_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.PRODUCT_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.errorPage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.normalizeUrl
import org.bibletranslationtools.fetcher.web.controllers.utils.validator

// books page
fun Routing.bookController() {
    route("/$GL_ROUTE/{$LANGUAGE_PARAM_KEY}/{$PRODUCT_PARAM_KEY}") {
        get {
            val path = normalizeUrl(call.request.path())
            val params = UrlParameters(
                languageCode = call.parameters[LANGUAGE_PARAM_KEY] ?: "",
                productSlug = call.parameters[PRODUCT_PARAM_KEY] ?: ""
            )
            if (
                !validator.isLanguageCodeValid(params.languageCode) ||
                !validator.isProductSlugValid(params.productSlug)
            ) {
                call.respond(
                    errorPage(
                        "invalid_route_parameter",
                        "invalid_route_parameter_message",
                        HttpStatusCode.NotFound
                    )
                )
                return@get
            }

            call.respond(
                booksView(params, path)
            )
        }
    }
}

private fun booksView(
    params: UrlParameters,
    path: String
): ThymeleafContent {
    val contentCache = get<ContentCacheAccessor>()
    val language = get<LanguageRepository>().getLanguage(params.languageCode)!!
    val product = get<ProductCatalog>().getProduct(params.productSlug)!!

    val bookViewData = FetchBookViewData(
        get<EnvironmentConfig>(),
        get<BookRepository>(),
        get<StorageAccess>(),
        language,
        product
    ).getViewDataList(path, contentCache, language.isGateway)

    return ThymeleafContent(
        template = "books",
        model = mapOf(
            "bookList" to bookViewData,
            "languagesNavTitle" to language.localizedName,
            "languagesNavUrl" to "/$GL_ROUTE",
            "fileTypesNavTitle" to product.titleKey,
            "fileTypesNavUrl" to "/$GL_ROUTE/${params.languageCode}",
            "booksNavUrl" to ""
        ),
        locale = getPreferredLocale(contentLanguage, "books")
    )
}
