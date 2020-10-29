package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.client.features.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import java.lang.NumberFormatException
import java.util.Locale
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.bibletranslationtools.fetcher.usecase.RequestResourceContainer
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.ALL_CHAPTERS_PARAM
import org.bibletranslationtools.fetcher.web.controllers.utils.BOOK_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.CHAPTER_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.LANGUAGE_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.PRODUCT_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.RoutingValidator
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.errorPage
import org.bibletranslationtools.fetcher.web.controllers.utils.getLanguageName
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.getProductTitleKey

fun Routing.chapterController(resolver: DependencyResolver) {
    route("/$GL_ROUTE/{$LANGUAGE_PARAM_KEY}/{$PRODUCT_PARAM_KEY}/{$BOOK_PARAM_KEY}") {
        get {
            // chapters page
            val params = UrlParameters(
                _languageCode = call.parameters[LANGUAGE_PARAM_KEY],
                _productSlug = call.parameters[PRODUCT_PARAM_KEY],
                _bookSlug = call.parameters[BOOK_PARAM_KEY]
            )

            if (!validateParameters(params, resolver)) {
                call.respond(
                    errorPage(
                        "invalid_route_parameter",
                        "invalid_route_parameter_message",
                        HttpStatusCode.NotFound,
                        contentLanguage
                    )
                )
                return@get
            }
            call.respond(chaptersView(params, resolver, contentLanguage))
        }
        route("{$CHAPTER_PARAM_KEY}") {
            get {
                val params = UrlParameters(
                    _languageCode = call.parameters[LANGUAGE_PARAM_KEY],
                    _productSlug = call.parameters[PRODUCT_PARAM_KEY],
                    _bookSlug = call.parameters[BOOK_PARAM_KEY],
                    _chapter = call.parameters[CHAPTER_PARAM_KEY]
                )

                if (
                    !validateParameters(params, resolver) ||
                    ProductFileExtension.getType(params.productSlug) != ProductFileExtension.ORATURE
                ) {
                    call.respond(
                        errorPage(
                            "invalid_route_parameter",
                            "invalid_route_parameter_message",
                            HttpStatusCode.NotFound,
                            contentLanguage
                        )
                    )
                    return@get
                }
                val downloadLink = requestRCDownloadLink(params, resolver)
                if (downloadLink == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(downloadLink)
                }
            }
        }
    }
}

private fun chaptersView(
    params: UrlParameters,
    resolver: DependencyResolver,
    contentLanguage: List<Locale.LanguageRange>
): ThymeleafContent {

    val bookViewData: BookViewData? = FetchBookViewData(
        resolver.bookRepository,
        resolver.storageAccess,
        params.languageCode,
        params.productSlug
    ).getViewData(params.bookSlug)

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
            "internal_error",
            "internal_error_message",
            HttpStatusCode.InternalServerError,
            contentLanguage
        )
    }

    return if (chapterViewDataList == null || bookViewData == null) {
        errorPage(
            "not_found",
            "not_found_message",
            HttpStatusCode.NotFound,
            contentLanguage
        )
    } else {
        val languageName = getLanguageName(params.languageCode, resolver)
        val productTitle = getProductTitleKey(params.productSlug, resolver)
        val isRequestLink =
            ProductFileExtension.getType(params.productSlug) == ProductFileExtension.ORATURE
        ThymeleafContent(
            template = "chapters",
            model = mapOf(
                "book" to bookViewData,
                "chapterList" to chapterViewDataList,
                "languagesNavTitle" to languageName,
                "languagesNavUrl" to "/$GL_ROUTE",
                "fileTypesNavTitle" to productTitle,
                "fileTypesNavUrl" to "/$GL_ROUTE/${params.languageCode}",
                "booksNavTitle" to bookViewData.localizedName,
                "booksNavUrl" to "/$GL_ROUTE/${params.languageCode}/${params.productSlug}",
                "isRequestLink" to isRequestLink
            ),
            locale = getPreferredLocale(contentLanguage, "chapters")
        )
    }
}

private fun requestRCDownloadLink(
    params: UrlParameters,
    resolver: DependencyResolver
): String? {
    if (params.chapter == ALL_CHAPTERS_PARAM) {
        // all available chapters
        return RequestResourceContainer(resolver.rcService).getResourceContainer(
            bookSlug = params.bookSlug,
            languageCode = params.languageCode,
            chapterNumber = null
        )?.path
    } else {
        return try {
            val chapterNumber = params.chapter.toInt()
            val downloadFileUrl =
                RequestResourceContainer(resolver.rcService).getResourceContainer(
                    languageCode = params.languageCode,
                    bookSlug = params.bookSlug,
                    chapterNumber = chapterNumber
                )
            downloadFileUrl?.path
        } catch (ex: NumberFormatException) {
            null
        }
    }
}

private fun validateParameters(
    params: UrlParameters,
    resolver: DependencyResolver
): Boolean {
    val validator = RoutingValidator(resolver)

    return validator.isLanguageCodeValid(params.languageCode) &&
            validator.isProductSlugValid(params.productSlug) &&
            validator.isBookSlugValid(params.bookSlug)
}
