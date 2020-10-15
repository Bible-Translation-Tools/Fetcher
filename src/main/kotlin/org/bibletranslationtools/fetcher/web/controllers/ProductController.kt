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
import org.bibletranslationtools.fetcher.usecase.FetchProductViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.*
import java.util.Locale

fun Routing.productController(resolver: DependencyResolver) {
    route("/$GL_ROUTE/{${ParamKeys.languageParamKey}}") {
        var contentLanguage = listOf<Locale.LanguageRange>()
        // execute before any sub-routes
        intercept(ApplicationCallPipeline.Call) {
            if (!call.request.uri.startsWith("/static")) {
                contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
            }
        }
        get {
            // products page
            val path = normalizeUrl(call.request.path())
            val params = Params(
                lc = call.parameters[ParamKeys.languageParamKey]
            )
            call.respond(
                productsView(params, path, resolver, contentLanguage)
            )
        }
    }
}

private fun productsView(
    params: Params,
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val validator =
        RoutingValidator(resolver)

    if (!validator.isLanguageCodeValid(params.languageCode)) {
        return errorPage(
            "invalid_route_parameter",
            "invalid_route_parameter_message",
            HttpStatusCode.NotFound,
            contentLanguage
        )
    }

    val model = FetchProductViewData(resolver.productCatalog)
    val languageName = getLanguageName(params.languageCode, resolver)

    return ThymeleafContent(
        template = "products",
        model = mapOf(
            "productList" to model.getListViewData(path),
            "languagesNavTitle" to languageName,
            "languagesNavUrl" to "/$GL_ROUTE",
            "fileTypesNavUrl" to "#"
        ),
        locale = getPreferredLocale(contentLanguage,"products")
    )
}
