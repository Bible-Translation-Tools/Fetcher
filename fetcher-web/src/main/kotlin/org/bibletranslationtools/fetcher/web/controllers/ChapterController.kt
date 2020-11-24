package org.bibletranslationtools.fetcher.web.controllers

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.client.features.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.bibletranslationtools.fetcher.usecase.DeliverableBuilder
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.bibletranslationtools.fetcher.usecase.RequestResourceContainer
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.BOOK_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.CHAPTER_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.HL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.LANGUAGE_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.PRODUCT_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.errorPage
import org.bibletranslationtools.fetcher.web.controllers.utils.getLanguageName
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.getProductTitleKey
import org.bibletranslationtools.fetcher.web.controllers.utils.validator

fun Routing.chapterController(resolver: DependencyResolver) {
    route("/$GL_ROUTE/{$LANGUAGE_PARAM_KEY}/{$PRODUCT_PARAM_KEY}/{$BOOK_PARAM_KEY}") {
        get {
            // chapters page
            val params = UrlParameters(
                languageCode = call.parameters[LANGUAGE_PARAM_KEY],
                productSlug = call.parameters[PRODUCT_PARAM_KEY],
                bookSlug = call.parameters[BOOK_PARAM_KEY]
            )

            if (!validateParameters(params, resolver)) {
                call.respond(
                    errorPage(
                        "invalid_route_parameter",
                        "invalid_route_parameter_message",
                        HttpStatusCode.NotFound
                    )
                )
                return@get
            }
            call.respond(chaptersView(params, resolver, true))
        }
        route("{$CHAPTER_PARAM_KEY}") {
            oratureChapters(resolver)
        }
    }
    route("/$HL_ROUTE/{$LANGUAGE_PARAM_KEY}/{$PRODUCT_PARAM_KEY}/{$BOOK_PARAM_KEY}") {
        get {
            // chapters page
            val params = UrlParameters(
                languageCode = call.parameters[LANGUAGE_PARAM_KEY],
                productSlug = call.parameters[PRODUCT_PARAM_KEY],
                bookSlug = call.parameters[BOOK_PARAM_KEY]
            )

            if (!validateParameters(params, resolver)) {
                call.respond(
                    errorPage(
                        "invalid_route_parameter",
                        "invalid_route_parameter_message",
                        HttpStatusCode.NotFound
                    )
                )
                return@get
            }
            call.respond(chaptersView(params, resolver, false))
        }
    }
}

private fun Route.oratureChapters(resolver: DependencyResolver) {
    get {
        val params = UrlParameters(
            languageCode = call.parameters[LANGUAGE_PARAM_KEY],
            productSlug = call.parameters[PRODUCT_PARAM_KEY],
            bookSlug = call.parameters[BOOK_PARAM_KEY],
            chapter = call.parameters[CHAPTER_PARAM_KEY]
        )

        if (
            !validateParameters(params, resolver) ||
            ProductFileExtension.getType(params.productSlug) != ProductFileExtension.ORATURE
        ) {
            call.respond(
                errorPage(
                    "invalid_route_parameter",
                    "invalid_route_parameter_message",
                    HttpStatusCode.NotFound
                )
            )
            return@get
        }

        val downloadLink = oratureFileDownload(params, resolver)
        if (downloadLink == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(downloadLink)
        }
    }
}

private fun chaptersView(
    params: UrlParameters,
    resolver: DependencyResolver,
    isGateway: Boolean
): ThymeleafContent {

    val bookViewData: BookViewData? = FetchBookViewData(
        resolver.bookRepository,
        resolver.storageAccess,
        params.languageCode,
        params.productSlug
    ).getViewData(params.bookSlug, resolver.contentCache, isGateway)

    val chapterViewDataList: List<ChapterViewData>? = try {
        FetchChapterViewData(
            chapterCatalog = resolver.chapterCatalog,
            storage = resolver.storageAccess,
            languageCode = params.languageCode,
            productSlug = params.productSlug,
            bookSlug = params.bookSlug
        ).getViewDataList(resolver.contentCache, isGateway)
    } catch (ex: ClientRequestException) {
        return errorPage(
            "internal_error",
            "internal_error_message",
            HttpStatusCode.InternalServerError
        )
    }

    return if (chapterViewDataList == null || bookViewData == null) {
        errorPage(
            "not_found",
            "not_found_message",
            HttpStatusCode.NotFound
        )
    } else {
        val languageName = getLanguageName(params.languageCode, resolver)
        val productTitle = getProductTitleKey(params.productSlug, resolver)
        val languageRoute = if (isGateway) GL_ROUTE else HL_ROUTE
        val isRequestLink =
            ProductFileExtension.getType(params.productSlug) == ProductFileExtension.ORATURE

        ThymeleafContent(
            template = "chapters",
            model = mapOf(
                "book" to bookViewData,
                "chapterList" to chapterViewDataList,
                "languagesNavTitle" to languageName,
                "languagesNavUrl" to "/$languageRoute",
                "fileTypesNavTitle" to productTitle,
                "fileTypesNavUrl" to "/$languageRoute/${params.languageCode}",
                "booksNavTitle" to bookViewData.localizedName,
                "booksNavUrl" to "/$languageRoute/${params.languageCode}/${params.productSlug}",
                "isRequestLink" to isRequestLink
            ),
            locale = getPreferredLocale(contentLanguage, "chapters")
        )
    }
}

private fun validateParameters(
    params: UrlParameters,
    resolver: DependencyResolver
): Boolean {
    return validator.isLanguageCodeValid(params.languageCode) &&
            validator.isProductSlugValid(params.productSlug) &&
            validator.isBookSlugValid(params.bookSlug) &&
            validator.isChapterValid(params.chapter)
}

private fun oratureFileDownload(
    params: UrlParameters,
    resolver: DependencyResolver
): String? {
    val deliverable = DeliverableBuilder(
        resolver.languageRepository,
        resolver.productCatalog,
        resolver.bookRepository
    ).build(params)

    return RequestResourceContainer(
        resolver.rcRepository,
        resolver.storageAccess,
        resolver.downloadClient
    ).getResourceContainer(deliverable)?.url
}
