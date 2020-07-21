package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.request.acceptLanguage
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchLanguageViewData
import org.bibletranslationtools.fetcher.usecase.FetchProductViewData
import java.util.*

fun Routing.root(resolver: DependencyResolver) {
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
                val languageModel = FetchLanguageViewData(resolver.languageRepository)
                val path = call.request.path()

                call.respond(
                    ThymeleafContent(
                        template = "",
                        model = mapOf(
                            "languageList" to languageModel.getListViewData(path)
                        ),
                        locale = getPreferredLocale(contentLanguage, "")
                    )
                )
            }
            route("{languageCode}") {
                get {
                    // products page
                    val productModel = FetchProductViewData(resolver.productCatalog)
                    val path = call.request.path()

                    call.respond(
                        ThymeleafContent(
                            template = "",
                            model = mapOf(
                                "productList" to productModel.getListViewData(path)
                            ),
                            locale = getPreferredLocale(contentLanguage, "")
                        )
                    )
                }
                route("{productSlug}") {
                    get {
                        // books page
                        val languageCode = call.parameters["languageCode"]
                        if (languageCode.isNullOrBlank()) {
                            // invalid route parameter
                            call.respond(
                                ThymeleafContent(
                                    template = "error",
                                    model = mapOf()
                                )
                            )
                        }
                        val bookModel = FetchBookViewData(
                            resolver.bookRepository,
                            languageCode!!
                        )
                        val path = call.request.path()

                        call.respond(
                            ThymeleafContent(
                                template = "",
                                model = mapOf(
                                    "bookList" to bookModel.getListViewData(path)
                                ),
                                locale = getPreferredLocale(contentLanguage, "")
                            )
                        )
                    }
                    route("{bookSlug}") {
                        get {
                            // chapters page
                            val languageCode = call.parameters["languageCode"]
                            val bookSlug = call.parameters["bookSlug"]
                            val productSlug = call.parameters["productSlug"]
                            if (
                                languageCode.isNullOrBlank() ||
                                bookSlug.isNullOrBlank() ||
                                productSlug.isNullOrBlank()
                            ) {
                                // invalid route parameters
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getPreferredLocale(languageRanges: List<Locale.LanguageRange>, templateName: String): Locale {
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
