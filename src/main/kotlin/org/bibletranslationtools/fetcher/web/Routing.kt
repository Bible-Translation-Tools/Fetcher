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
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData

private object ParamKeys {
    const val languageParamKey = "languageCode"
    const val productParamKey = "productSlug"
    const val bookParamKey = "bookSlug"
}

fun Routing.root(resolver: DependencyResolver) {
    route("/") {
        get {
            // landing page
            call.respond(
                ThymeleafContent(
                    template = "landing",
                    model = mapOf()
                )
            )
        }
        route("gl") {
            get {
                // languages page
                val path = normalizeUrl(call.request.path())
                call.respond(gatewayLanguagesView(path, resolver))
            }
            route("{${ParamKeys.languageParamKey}}") {
                get {
                    // products page
                    val path = normalizeUrl(call.request.path())
                    call.respond(productsView(path, resolver))
                }
                route("{${ParamKeys.productParamKey}}") {
                    get {
                        // books page
                        val path = normalizeUrl(call.request.path())
                        call.respond(booksView(call.parameters, path, resolver))
                    }
                    route("{${ParamKeys.bookParamKey}}") {
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

private fun normalizeUrl(path: String): String = java.io.File(path).invariantSeparatorsPath

private fun gatewayLanguagesView(
    path: String,
    resolver: DependencyResolver
): ThymeleafContent {
    val model = FetchLanguageViewData(resolver.languageRepository)
    return ThymeleafContent(
        template = "languages",
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
        template = "products",
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
    val languageCode = parameters[ParamKeys.languageParamKey]
    if (languageCode.isNullOrEmpty()) return errorPage("Invalid Language Code")

    val bookViewData = FetchBookViewData(
        resolver.bookRepository,
        resolver.storageAccess,
        languageCode
    ).getViewDataList(path)

    return ThymeleafContent(
        template = "books",
        model = mapOf(
            "bookList" to bookViewData
        )
    )
}

private fun chaptersView(
    parameters: Parameters,
    resolver: DependencyResolver
): ThymeleafContent {
    val bookViewData: BookViewData? = getBookViewData(parameters, resolver)

    val chapterViewDataList: List<ChapterViewData>? = try {
        getChapterViewDataList(parameters, resolver)
    } catch (ex: ClientRequestException) {
        return errorPage("Server network error. Please check back again later.")
    }

    return when {
        chapterViewDataList == null -> errorPage("Invalid Parameters")
        bookViewData == null -> errorPage("Could not find the content with the specified url")
        else -> ThymeleafContent(
            template = "chapters",
            model = mapOf(
                "book" to bookViewData,
                "chapterList" to chapterViewDataList
            )
        )
    }
}

private fun getBookViewData(parameters: Parameters, resolver: DependencyResolver): BookViewData? {
    val languageCode = parameters[ParamKeys.languageParamKey]
    val bookSlug = parameters[ParamKeys.bookParamKey]
    val productSlug = parameters[ParamKeys.productParamKey]

    return if (!languageCode.isNullOrEmpty() && !bookSlug.isNullOrEmpty() && !productSlug.isNullOrEmpty()) {
        FetchBookViewData(
            resolver.bookRepository,
            resolver.storageAccess,
            languageCode
        ).getViewData(bookSlug, productSlug)
    } else {
        null
    }
}

@Throws(ClientRequestException::class)
private fun getChapterViewDataList(parameters: Parameters, resolver: DependencyResolver): List<ChapterViewData>? {
    val languageCode = parameters[ParamKeys.languageParamKey]
    val productSlug = parameters[ParamKeys.productParamKey]
    val bookSlug = parameters[ParamKeys.bookParamKey]

    return if (!languageCode.isNullOrEmpty() && !bookSlug.isNullOrEmpty() && !productSlug.isNullOrEmpty()) {
        FetchChapterViewData(
            chapterCatalog = resolver.chapterCatalog,
            storage = resolver.storageAccess,
            languageCode = languageCode,
            productSlug = productSlug,
            bookSlug = bookSlug
        ).getViewDataList()
    } else {
        null
    }
}

private fun errorPage(message: String): ThymeleafContent {
    return ThymeleafContent(
        template = "",
        model = mapOf("errorMessage" to message)
    )
}
