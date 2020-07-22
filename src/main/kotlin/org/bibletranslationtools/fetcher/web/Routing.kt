package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.http.Parameters
import io.ktor.request.acceptLanguage
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import java.util.Locale
import java.util.ResourceBundle
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchLanguageViewData
import org.bibletranslationtools.fetcher.usecase.FetchProductViewData

fun Routing.root(resolver: DependencyResolver) {
    // language locale could be changed by user selected gl
    var contentLanguage: MutableList<Locale.LanguageRange> = mutableListOf()
    route("/") {
        get {
            // landing page
            contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
            call.respond(
                ThymeleafContent(
                    template = "index",
                    model = mapOf(),
                    locale = getPreferredLocale(contentLanguage, "index")
                )
            )
        }
        route("gl") {
            get {
                // languages page
                val path = call.request.path()
                call.respond(gatewayLanguagesView(path, resolver, contentLanguage))
            }
            route("{languageCode}") {
                get {
                    // products page
                    val path = call.request.path()
                    call.respond(productsView(path, resolver, contentLanguage))
                }
                route("{productSlug}") {
                    get {
                        // books page
                        val languageCode = call.parameters["languageCode"]
                        val path = call.request.path()
                        call.respond(booksView(languageCode, path, resolver, contentLanguage))
                    }
                    route("{bookSlug}") {
                        get {
                            call.respond(chaptersView(call.parameters, resolver, contentLanguage))
                        }
                    }
                }
            }
        }
    }
}

private fun getPreferredLocale(languageRanges: List<Locale.LanguageRange>, templateName: String): Locale {
    val noFallbackController = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)

    for (languageRange in languageRanges) {
        val locale = Locale.Builder().setLanguageTag(languageRange.range).build()
        try {
            ResourceBundle.getBundle("templates/$templateName", locale, noFallbackController)
            return locale
        } catch (ex: UnsupportedOperationException) {
            ex.printStackTrace()
        }
    }

    return Locale.getDefault()
}

private fun gatewayLanguagesView(
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val model = FetchLanguageViewData(resolver.languageRepository)
    return ThymeleafContent(
        template = "",
        model = mapOf(
            "languageList" to model.getListViewData(path)
        ),
        locale = getPreferredLocale(contentLanguage, "")
    )
}

private fun productsView(
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val model = FetchProductViewData(resolver.productCatalog)

    return ThymeleafContent(
        template = "",
        model = mapOf(
            "productList" to model.getListViewData(path)
        ),
        locale = getPreferredLocale(contentLanguage, "")
    )
}

private fun booksView(
    languageCode: String?,
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    if (languageCode.isNullOrBlank()) {
        // invalid route parameter
        return ThymeleafContent(
            template = "error",
            model = mapOf()
        )
    }
    val bookModel = FetchBookViewData(resolver.bookRepository, languageCode)

    return ThymeleafContent(
        template = "",
        model = mapOf(
            "bookList" to bookModel.getListViewData(path)
        ),
        locale = getPreferredLocale(contentLanguage, "")
    )
}

private fun chaptersView(
    parameters: Parameters,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val languageCode = parameters["languageCode"]
    val bookSlug = parameters["bookSlug"]
    val productSlug = parameters["productSlug"]

    if (
        languageCode.isNullOrBlank() ||
        bookSlug.isNullOrBlank() ||
        productSlug.isNullOrBlank()
    ) {
        // invalid route parameters
        return ThymeleafContent(template = "error", model = mapOf())
    }

    val book = FetchBookViewData(resolver.bookRepository, languageCode)
    return ThymeleafContent(
        template = "",
        model = mapOf("book" to book),
        locale = getPreferredLocale(contentLanguage, "")
    )
}
