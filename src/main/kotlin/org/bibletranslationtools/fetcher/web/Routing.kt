package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.client.features.ClientRequestException
import io.ktor.http.Parameters
import io.ktor.request.acceptLanguage
import io.ktor.request.path
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import java.lang.IllegalArgumentException
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.FetchLanguageViewData
import org.bibletranslationtools.fetcher.usecase.FetchProductViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData
import org.slf4j.LoggerFactory

private const val GL_ROUTE = "gl"

private object ParamKeys {
    const val languageParamKey = "languageCode"
    const val productParamKey = "productSlug"
    const val bookParamKey = "bookSlug"
}

fun Routing.root(resolver: DependencyResolver) {
    route("/") {
        var contentLanguage = listOf<Locale.LanguageRange>()
        // execute before any sub-routes
        intercept(ApplicationCallPipeline.Call) {
            if (!call.request.uri.startsWith("/static")) {
                contentLanguage = Locale.LanguageRange.parse(call.request.acceptLanguage())
            }
        }
        get {
            // landing page

            call.respond(
                ThymeleafContent(
                    template = "landing",
                    model = mapOf(
                        "glRoute" to "/$GL_ROUTE"
                    ),
                    locale = getPreferredLocale(contentLanguage, "landing")
                )
            )
        }
        route(GL_ROUTE) {
            get {
                // languages page
                val path = normalizeUrl(call.request.path())
                call.respond(gatewayLanguagesView(path, resolver, contentLanguage))
            }
            route("{${ParamKeys.languageParamKey}}") {
                get {
                    // products page
                    val path = normalizeUrl(call.request.path())
                    call.respond(productsView(call.parameters, path, resolver, contentLanguage))
                }
                route("{${ParamKeys.productParamKey}}") {
                    get {
                        // books page
                        val path = normalizeUrl(call.request.path())
                        call.respond(booksView(call.parameters, path, resolver, contentLanguage))
                    }
                    route("{${ParamKeys.bookParamKey}}") {
                        get {
                            // chapters page
                            call.respond(chaptersView(call.parameters, resolver, contentLanguage))
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
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val model = FetchLanguageViewData(resolver.languageRepository)
    return ThymeleafContent(
        template = "languages",
        model = mapOf(
            "languageList" to model.getListViewData(path),
            "languageNavTitle" to "",
            "languagesNavUrl" to "#"
        ),
        locale = getPreferredLocale(contentLanguage, "languages")
    )
}

private fun productsView(
    parameters: Parameters,
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val validator = RoutingValidator(resolver)
    if(!validator.isLanguageCodeValid(parameters[ParamKeys.languageParamKey])) {
        return errorPage("Language Code ${parameters["languageCode"]} is invalid.")
    }

    val model = FetchProductViewData(resolver.productCatalog)
    val languageCode = parameters[ParamKeys.languageParamKey] ?: ""
    if (languageCode.isNullOrEmpty()) return errorPage("Invalid Language Code")

    val languageName = resolver.languageCatalog.getLanguage(languageCode)?.localizedName ?: ""

    return ThymeleafContent(
        template = "products",
        model = mapOf(
            "productList" to model.getListViewData(path),
            "languageNavTitle" to languageName,
            "languagesNavUrl" to "/$GL_ROUTE",
            "toolsNavUrl" to "#"
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
    val validator = RoutingValidator(resolver)
    val languageCode = parameters[ParamKeys.languageParamKey]

    if (!validator.isLanguageCodeValid(languageCode)) return errorPage("Invalid Language Code")
    if(!validator.isProductSlugValid(parameters[ParamKeys.productParamKey])) return errorPage("Invalid Product Slug")

    val languageName = resolver.languageCatalog.getLanguage(languageCode)?.localizedName ?: ""
    val bookViewData = FetchBookViewData(
        resolver.bookRepository,
        resolver.storageAccess,
        languageCode!!
    ).getViewDataList(path)

    return ThymeleafContent(
        template = "books",
        model = mapOf(
            "bookList" to bookViewData,
            "languageNavTitle" to languageName,
            "languagesNavUrl" to "/$GL_ROUTE",
            "toolsNavUrl" to "/$GL_ROUTE/$languageCode",
            "booksNavUrl" to "#"
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

    val languageCode = parameters["languageCode"]
    val productSlug = parameters["productSlug"]
    val language = resolver.languageCatalog.getLanguage(languageCode ?: "")
    val languageName = language?.localizedName ?: ""

    return when {
        chapterViewDataList == null -> errorPage("Invalid Parameters")
        bookViewData == null -> errorPage("Could not find the content with the specified url")
        else -> ThymeleafContent(
            template = "chapters",
            model = mapOf(
                "book" to bookViewData,
                "chapterList" to chapterViewDataList,
                "languageNavTitle" to languageName,
                "languagesNavUrl" to "/$GL_ROUTE",
                "toolsNavUrl" to "/$GL_ROUTE/$languageCode",
                "booksNavUrl" to "/$GL_ROUTE/$languageCode/$productSlug"
            ),
            locale = getPreferredLocale(contentLanguage, "chapters")
        )
    }
}

private fun getBookViewData(parameters: Parameters, resolver: DependencyResolver): BookViewData? {
    val validator = RoutingValidator(resolver)
    val languageCode = parameters[ParamKeys.languageParamKey]
    val bookSlug = parameters[ParamKeys.bookParamKey]
    val productSlug = parameters[ParamKeys.productParamKey]

    return if (
        validator.isLanguageCodeValid(languageCode) &&
        validator.isBookSlugValid(languageCode, bookSlug) &&
        validator.isProductSlugValid(productSlug)
    ) {
        FetchBookViewData(
            resolver.bookRepository,
            resolver.storageAccess,
            languageCode!!
        ).getViewData(bookSlug!!, productSlug!!)
    } else {
        null
    }
}

@Throws(ClientRequestException::class)
private fun getChapterViewDataList(parameters: Parameters, resolver: DependencyResolver): List<ChapterViewData>? {
    val validator = RoutingValidator(resolver)
    val languageCode = parameters[ParamKeys.languageParamKey]
    val productSlug = parameters[ParamKeys.productParamKey]
    val bookSlug = parameters[ParamKeys.bookParamKey]

    return if (
        validator.isLanguageCodeValid(languageCode) &&
        validator.isBookSlugValid(languageCode, bookSlug) &&
        validator.isProductSlugValid(productSlug)
    ) {
        FetchChapterViewData(
            chapterCatalog = resolver.chapterCatalog,
            storage = resolver.storageAccess,
            languageCode = languageCode!!,
            productSlug = productSlug!!,
            bookSlug = bookSlug!!
        ).getViewDataList()
    } else {
        null
    }
}

private fun getPreferredLocale(languageRanges: List<Locale.LanguageRange>, templateName: String): Locale {
    val noFallbackController = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)
    val logger = LoggerFactory.getLogger("GetLocale")

    for (languageRange in languageRanges) {
        val locale = Locale.Builder().setLanguageTag(languageRange.range).build()
        try {
            ResourceBundle.getBundle("templates/$templateName", locale, noFallbackController)
            return locale
        } catch (ex: MissingResourceException) {
            logger.warn("Locale for ${locale.language} not supported")
        } catch (ex: IllegalArgumentException) {
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
