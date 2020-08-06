package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.client.features.ClientRequestException
import io.ktor.http.HttpStatusCode
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

private data class Params(
    private val lc: String? = null, // language code
    private val ps: String? = null, // product slug
    private val bs: String? = null // book slug
) {
    val languageCode = lc ?: ""
    val productSlug = ps ?: ""
    val bookSlug = bs ?: ""
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
                    val params = Params(
                        lc = call.parameters[ParamKeys.languageParamKey]
                    )
                    call.respond(productsView(params, path, resolver, contentLanguage))
                }
                route("{${ParamKeys.productParamKey}}") {
                    get {
                        // books page
                        val path = normalizeUrl(call.request.path())
                        val params = Params(
                            lc = call.parameters[ParamKeys.languageParamKey],
                            ps = call.parameters[ParamKeys.productParamKey]
                        )
                        call.respond(booksView(params, path, resolver, contentLanguage))
                    }
                    route("{${ParamKeys.bookParamKey}}") {
                        get {
                            // chapters page
                            val params = Params(
                                lc = call.parameters[ParamKeys.languageParamKey],
                                ps = call.parameters[ParamKeys.productParamKey],
                                bs = call.parameters[ParamKeys.bookParamKey]
                            )
                            call.respond(chaptersView(params, resolver, contentLanguage))
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
    params: Params,
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val validator = RoutingValidator(resolver)

    if (!validator.isLanguageCodeValid(params.languageCode)) {
        return errorPage(
            "Invalid route parameters",
            "Invalid Language.",
            HttpStatusCode.NotFound
        )
    }

    val model = FetchProductViewData(resolver.productCatalog)
    val languageName = getLanguageName(params.languageCode, resolver)

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
    params: Params,
    path: String,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val validator = RoutingValidator(resolver)

    if (
        !validator.isLanguageCodeValid(params.languageCode) ||
        !validator.isProductSlugValid(params.productSlug)
    ) {
        return errorPage(
            "Invalid route parameters",
            "Invalid Language and/or Product.",
            HttpStatusCode.NotFound
        )
    }

    val languageName = getLanguageName(params.languageCode, resolver)
    val bookViewData = FetchBookViewData(
        resolver.bookRepository,
        resolver.storageAccess,
        params.languageCode
    ).getViewDataList(path)

    return ThymeleafContent(
        template = "books",
        model = mapOf(
            "bookList" to bookViewData,
            "languageNavTitle" to languageName,
            "languagesNavUrl" to "/$GL_ROUTE",
            "toolsNavUrl" to "/$GL_ROUTE/${params.languageCode}",
            "booksNavUrl" to "#"
        ),
        locale = getPreferredLocale(contentLanguage, "books")
    )
}

private fun chaptersView(
    params: Params,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {
    val validator = RoutingValidator(resolver)

    if (
        !validator.isLanguageCodeValid(params.languageCode) ||
        !validator.isProductSlugValid(params.productSlug) ||
        !validator.isBookSlugValid(params.languageCode, params.bookSlug)
    ) {
        return errorPage(
            "Invalid route parameters",
            "Invalid Language, Product, and/or Book.",
            HttpStatusCode.NotFound
        )
    }

    val languageName = getLanguageName(params.languageCode, resolver)
    val bookViewData: BookViewData? = FetchBookViewData(
        resolver.bookRepository,
        resolver.storageAccess,
        params.languageCode
    ).getViewData(params.bookSlug, params.productSlug)
    val chapterViewDataList: List<ChapterViewData>? = try {
        FetchChapterViewData(
            chapterCatalog = resolver.chapterCatalog,
            storage = resolver.storageAccess,
            languageCode = params.languageCode,
            productSlug = params.productSlug,
            bookSlug = params.bookSlug
        ).getViewDataList()
    } catch (ex: ClientRequestException) {
        return errorPage(
            "Internal Server Error",
            "An error occurred on the network. Please refresh and try again.",
            HttpStatusCode.InternalServerError
        )
    }

    return when {
        chapterViewDataList == null -> {
            errorPage(
                "Chapter Data Not Found",
                "Error loading data about the chapters.",
                HttpStatusCode.NotFound
            )
        }
        bookViewData == null -> {
            errorPage(
                "Book Data Not Found",
                "Error loading data about the book.",
                HttpStatusCode.NotFound
            )
        }
        else -> ThymeleafContent(
            template = "chapters",
            model = mapOf(
                "book" to bookViewData,
                "chapterList" to chapterViewDataList,
                "languageNavTitle" to languageName,
                "languagesNavUrl" to "/$GL_ROUTE",
                "toolsNavUrl" to "/$GL_ROUTE/${params.languageCode}",
                "booksNavUrl" to "/$GL_ROUTE/${params.languageCode}/${params.productSlug}"
            ),
            locale = getPreferredLocale(contentLanguage, "chapters")
        )
    }
}

private fun getLanguageName(languageCode: String, resolver: DependencyResolver): String {
    return resolver.languageCatalog.getLanguage(languageCode)?.localizedName ?: ""
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

private fun errorPage(title: String, message: String, errorCode: HttpStatusCode): ThymeleafContent {
    return ThymeleafContent(
        template = "error",
        model = mapOf(
            "errorTitle" to title,
            "errorMessage" to message,
            "errorCode" to errorCode.value
        )
    )
}
