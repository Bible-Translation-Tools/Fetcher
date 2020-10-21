package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.client.features.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import java.util.Locale
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.*
import java.lang.NumberFormatException

fun Routing.chapterController(resolver: DependencyResolver) {
    route("/$GL_ROUTE/{$LANGUAGE_PARAM_KEY}/{$PRODUCT_PARAM_KEY}/{$BOOK_PARAM_KEY}") {
        get {
            // chapters page
            val params = UrlParameters(
                lc = call.parameters[LANGUAGE_PARAM_KEY],
                ps = call.parameters[PRODUCT_PARAM_KEY],
                bs = call.parameters[BOOK_PARAM_KEY]
            )

            val validator = RoutingValidator(resolver)
            if (
                !validator.isLanguageCodeValid(params.languageCode) ||
                !validator.isProductSlugValid(params.productSlug) ||
                !validator.isBookSlugValid(params.bookSlug)
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
            call.respond(chaptersView(params, resolver, contentLanguage))
        }
        route("{$CHAPTER_PARAM_KEY}") {
            get {
                val params = UrlParameters(
                    lc = call.parameters[LANGUAGE_PARAM_KEY],
                    ps = call.parameters[PRODUCT_PARAM_KEY],
                    bs = call.parameters[BOOK_PARAM_KEY],
                    ch = call.parameters[CHAPTER_PARAM_KEY]
                )
                val validator = RoutingValidator(resolver)
                if (
                    !validator.isLanguageCodeValid(params.languageCode) ||
//                    !validator.isProductSlugValid(params.productSlug) ||
                    params.productSlug != "bttr" ||
                    !validator.isBookSlugValid(params.bookSlug)
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
                val downloadLink = requestRCDownloadLink(params, resolver) ?: "haha"
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
            chapterRepository = resolver.chapterRepository,
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
        val isRequestLink = params.productSlug == "bttr"
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
    if (params.chapter == "all") {
        // all available chapter
        return ""
    } else {
        return try {
            val chapterNumber = params.chapter.toInt()
            val downloadFileUrl = resolver.chapterRepository.getChapterRC(
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