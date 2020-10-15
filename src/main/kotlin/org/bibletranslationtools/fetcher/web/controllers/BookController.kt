package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.acceptLanguage
import io.ktor.request.path
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.*
import java.util.Locale

fun Routing.bookController(resolver: DependencyResolver){
    route("/$GL_ROUTE/{${ParamKeys.languageParamKey}}/{${ParamKeys.productParamKey}}") {
        var contentLanguage = listOf<Locale.LanguageRange>()
        // execute before any sub-routes
        intercept(ApplicationCallPipeline.Call) {
            if (!call.request.uri.startsWith("/static")) {
                contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
            }
        }
        get {
            // books page
            val path = normalizeUrl(call.request.path())
            val params = Params(
                lc = call.parameters[ParamKeys.languageParamKey],
                ps = call.parameters[ParamKeys.productParamKey]
            )
            call.respond(
                booksView(params, path, resolver, contentLanguage)
            )
        }
    }
}

private fun booksView(
    params: Params,
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val validator =
        RoutingValidator(resolver)

    if (
        !validator.isLanguageCodeValid(params.languageCode) ||
        !validator.isProductSlugValid(params.productSlug)
    ) {
        return errorPage(
            "invalid_route_parameter",
            "invalid_route_parameter_message",
            HttpStatusCode.NotFound,
            contentLanguage
        )
    }

    val languageName = getLanguageName(params.languageCode, resolver)
    val productTitle = getProductTitleKey(params.productSlug, resolver)
    val bookViewData = FetchBookViewData(
        resolver.bookRepository,
        resolver.storageAccess,
        params.languageCode,
        params.productSlug
    ).getViewDataList(path)

    return ThymeleafContent(
        template = "books",
        model = mapOf(
            "bookList" to bookViewData,
            "languagesNavTitle" to languageName,
            "languagesNavUrl" to "/$GL_ROUTE",
            "fileTypesNavTitle" to productTitle,
            "fileTypesNavUrl" to "/$GL_ROUTE/${params.languageCode}",
            "booksNavUrl" to "#"
        ),
        locale = getPreferredLocale(contentLanguage,"books")
    )
}
