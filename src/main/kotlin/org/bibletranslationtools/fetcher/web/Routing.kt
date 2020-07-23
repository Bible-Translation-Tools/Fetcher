package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.client.features.ClientRequestException
import io.ktor.http.Parameters
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.FetchLanguageViewData
import org.bibletranslationtools.fetcher.usecase.FetchProductViewData

private const val languageParamKey = "languageCode"
private const val productParamKey = "productSlug"
private const val bookParamKey = "bookSlug"

fun Routing.root(resolver: DependencyResolver) {
    route("/") {
        get {
            // landing page
            call.respond(
                ThymeleafContent(
                    template = "",
                    model = mapOf()
                )
            )
        }
        route("gl") {
            get {
                // languages page
                val path = call.request.path()
                call.respond(gatewayLanguagesView(path, resolver))
            }
            route("{$languageParamKey}") {
                get {
                    // products page
                    val path = call.request.path()
                    call.respond(productsView(path, resolver))
                }
                route("{$productParamKey}") {
                    get {
                        // books page
                        val path = call.request.path()
                        call.respond(booksView(call.parameters, path, resolver))
                    }
                    route("{$bookParamKey}") {
                        get {
                            // chapters page
                            call.respond(chaptersView(call.parameters, resolver))
                        }
                    }
                }
            }
        }
    }
}

private fun gatewayLanguagesView(
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val model = FetchLanguageViewData(resolver.languageRepository)
    return ThymeleafContent(
        template = "",
        model = mapOf(
            "languageList" to model.getListViewData(path)
        )
    )
}

private fun productsView(
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val model = FetchProductViewData(resolver.productCatalog)

    return ThymeleafContent(
        template = "",
        model = mapOf(
            "productList" to model.getListViewData(path)
        )
    )
}

private fun booksView(
    parameters: Parameters,
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val languageCode = parameters[languageParamKey]
    if (languageCode.isNullOrEmpty()) return errorPage("Invalid Language Code")

    val booksModel = FetchBookViewData(resolver.bookRepository, languageCode)

    return ThymeleafContent(
        template = "",
        model = mapOf(
            "bookList" to booksModel.getListViewData(path)
        )
    )
}

private fun chaptersView(
    parameters: Parameters,
    resolver: DependencyResolver
): ThymeleafContent {
    val languageCode = parameters[languageParamKey]
    val productSlug = parameters[productParamKey]
    val bookSlug = parameters[bookParamKey]

    if (languageCode.isNullOrEmpty() || productSlug.isNullOrEmpty() || bookSlug.isNullOrEmpty()) {
        return errorPage("Error")
    }

    val book = FetchBookViewData(resolver.bookRepository, languageCode).getBookInfo(bookSlug)
    if (book == null) {
        return errorPage("Could not find the content with the specified url")
    }

    val chapterViewDataList = try {
        FetchChapterViewData(
            chapterCatalog = resolver.chapterCatalog,
            storage = resolver.storageAccess,
            languageCode = languageCode,
            productSlug = productSlug,
            bookSlug = bookSlug
        ).getViewDataList()
    } catch (ex: ClientRequestException) {
        return errorPage("Server network error. Please check back again later.")
    }

    return ThymeleafContent(
        template = "",
        model = mapOf(
            "book" to book,
            "chapterList" to chapterViewDataList
        )
    )
}

private fun errorPage(message: String): ThymeleafContent {
    return ThymeleafContent(
        template = "error",
        model = mapOf("errorMessage" to message)
    )
}
