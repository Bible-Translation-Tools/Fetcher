package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.call
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.bibletranslationtools.fetcher.usecase.FetchLanguageViewData
import org.bibletranslationtools.fetcher.usecase.FetchProductViewData

fun Routing.root(resolver: DependencyResolver) {
    route("/") {
        get {
            // landing page
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
                        )
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
                            )
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
                                )
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
