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
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.di.ext.CommonKoinExt.get
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.RequestResourceContainer
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.DeliverableBuilder
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchChapterViewData
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData
import org.bibletranslationtools.fetcher.web.controllers.utils.BOOK_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.CHAPTER_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.LANGUAGE_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.PRODUCT_PARAM_KEY
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.errorPage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale
import org.bibletranslationtools.fetcher.web.controllers.utils.validator

fun Routing.chapterController() {
    route("/$GL_ROUTE/{$LANGUAGE_PARAM_KEY}/{$PRODUCT_PARAM_KEY}/{$BOOK_PARAM_KEY}") {
        get {
            // chapters page
            val params = UrlParameters(
                languageCode = call.parameters[LANGUAGE_PARAM_KEY] ?: "",
                productSlug = call.parameters[PRODUCT_PARAM_KEY] ?: "",
                bookSlug = call.parameters[BOOK_PARAM_KEY] ?: ""
            )

            if (!validateParameters(params)) {
                call.respond(
                    errorPage(
                        "invalid_route_parameter",
                        "invalid_route_parameter_message",
                        HttpStatusCode.NotFound
                    )
                )
                return@get
            }

            val paramObjects = DeliverableBuilder(
                get<LanguageRepository>(),
                get<ProductCatalog>(),
                get<BookRepository>()
            ).build(params)

            call.respond(chaptersView(paramObjects))
        }
        route("{$CHAPTER_PARAM_KEY}") {
            oratureChapters()
        }
    }
}

private fun Route.oratureChapters() {
    get {
        val params = UrlParameters(
            languageCode = call.parameters[LANGUAGE_PARAM_KEY] ?: "",
            productSlug = call.parameters[PRODUCT_PARAM_KEY] ?: "",
            bookSlug = call.parameters[BOOK_PARAM_KEY] ?: "",
            chapter = call.parameters[CHAPTER_PARAM_KEY]
        )

        if (
            !validateParameters(params) ||
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

        val downloadLink = oratureFileDownload(params)
        if (downloadLink == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(downloadLink)
        }
    }
}

private fun chaptersView(paramObjects: Deliverable): ThymeleafContent {
    val envConfig = get<EnvironmentConfig>()
    val storageAccess = get<StorageAccess>()

    val bookViewData: BookViewData? = FetchBookViewData(
        envConfig,
        get<BookRepository>(),
        storageAccess,
        paramObjects.language,
        paramObjects.product
    ).getViewData(paramObjects.book.slug)

    val chapterViewDataList: List<ChapterViewData>? = try {
        FetchChapterViewData(
            envConfig,
            get<ChapterCatalog>(),
            storageAccess,
            paramObjects.language,
            paramObjects.product,
            paramObjects.book
        ).getViewDataList()
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
        val isRequestLink =
            ProductFileExtension.getType(paramObjects.product.slug) == ProductFileExtension.ORATURE

        ThymeleafContent(
            template = "chapters",
            model = mapOf(
                "book" to bookViewData,
                "chapterList" to chapterViewDataList,
                "languagesNavTitle" to paramObjects.language.localizedName,
                "languagesNavUrl" to "/$GL_ROUTE",
                "fileTypesNavTitle" to paramObjects.product.titleKey,
                "fileTypesNavUrl" to "/$GL_ROUTE/${paramObjects.language.code}",
                "booksNavTitle" to bookViewData.localizedName,
                "booksNavUrl" to "/$GL_ROUTE/${paramObjects.language.code}/${paramObjects.product.slug}",
                "isRequestLink" to isRequestLink
            ),
            locale = getPreferredLocale(contentLanguage, "chapters")
        )
    }
}

private fun validateParameters(params: UrlParameters): Boolean {
    return validator.isLanguageCodeValid(params.languageCode) &&
            validator.isProductSlugValid(params.productSlug) &&
            validator.isBookSlugValid(params.bookSlug) &&
            validator.isChapterValid(params.chapter)
}

private fun oratureFileDownload(
    params: UrlParameters
): String? {
    val deliverable = DeliverableBuilder(
        get<LanguageRepository>(),
        get<ProductCatalog>(),
        get<BookRepository>()
    ).build(params)

    return get<RequestResourceContainer>()
        .getResourceContainer(deliverable)?.url
}
