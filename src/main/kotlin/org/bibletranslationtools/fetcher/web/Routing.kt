package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.client.features.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.acceptLanguage
import io.ktor.request.path
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import java.util.Locale
import java.util.ResourceBundle
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
            val contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
            call.respond(
                ThymeleafContent(
                    template = "landing",
                    model = mapOf(),
                    locale = getPreferredLocale(contentLanguage, "landing")
                )
            )
        }
        route("gl") {
            get {
                // languages page
                val path = normalizeUrl(call.request.path())
                val contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
                call.respond(gatewayLanguagesView(path, resolver, contentLanguage))
            }
            route("{${ParamKeys.languageParamKey}}") {
                get {
                    // products page
                    val path = normalizeUrl(call.request.path())
                    val contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
                    call.respond(productsView(path, resolver, contentLanguage))
                }
                route("{${ParamKeys.productParamKey}}") {
                    get {
                        // books page
                        val path = normalizeUrl(call.request.path())
                        val contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
                        call.respond(booksView(call.parameters, path, resolver, contentLanguage))
                    }
                    route("{${ParamKeys.bookParamKey}}") {
                        get {
                            // chapters page
                            val contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
                            call.respond(chaptersView(call.parameters, resolver, contentLanguage))
                        }
                    }
                }
            }
        }
        route("/download") {
            get("{paths...}") {
                val pathFromRoute = call.parameters.getAll("paths")?.joinToString("/") ?: ""
                val file = resolver.storageAccess.getContentRoot().resolve(pathFromRoute)
                if (!file.isFile) {
                    call.respond(HttpStatusCode.NotFound, "File is no longer available.")
                } else {
                    call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name}\"")
                    call.respondFile(file)
                }
            }
        }
    }
}

private fun normalizeUrl(path: String): String = java.io.File(path).invariantSeparatorsPath

private fun gatewayLanguagesView(
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val model = FetchLanguageViewData(resolver.languageRepository)
    return ThymeleafContent(
        template = "languages",
        model = mapOf(
            "languageList" to model.getListViewData(path)
        ),
        locale = getPreferredLocale(contentLanguage, "languages")
    )
}

private fun productsView(
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val model = FetchProductViewData(resolver.productCatalog)

    return ThymeleafContent(
        template = "products",
        model = mapOf(
            "productList" to model.getListViewData(path)
        ),
        locale = getPreferredLocale(contentLanguage, "products")
    )
}

private fun booksView(
    parameters: Parameters,
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
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
        ),
        locale = getPreferredLocale(contentLanguage, "books")
    )
}

private fun chaptersView(
    parameters: Parameters,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
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
            ),
            locale = getPreferredLocale(contentLanguage, "chapters")
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

private fun getPreferredLocale(languageRanges: List<Locale.LanguageRange>, templateName: String): Locale {
    val noFallbackController = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)

    for (languageRange in languageRanges) {
        val locale = Locale.Builder().setLanguageTag(languageRange.range).build()
        try {
            ResourceBundle.getBundle("templates/$templateName", locale, noFallbackController)
            return locale
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    return Locale.getDefault()
}

private fun errorPage(message: String): ThymeleafContent {
    return ThymeleafContent(
        template = "",
        model = mapOf("errorMessage" to message)
    )
}
