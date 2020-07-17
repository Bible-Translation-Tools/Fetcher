package org.bibletranslationtools.fetcher.web

import dev.jbs.ktor.thymeleaf.Thymeleaf
import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import org.bibletranslationtools.fetcher.usecase.*
import org.bibletranslationtools.fetcher.usecase.FetchBookViewData
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

fun Application.appModule() {
    install(DefaultHeaders)
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }
    install(CallLogging)
    intercept(ApplicationCallPipeline.Setup) {
        // display error page here and terminate the pipeline if fatal exception occurs
    }
    install(Routing) {
        val serviceDI = DependencyResolver
        routing {
            // Static contents declared here
            static("static") {
                resources("css")
                resources("js")
                static("img") {
                    resources("img")
                }
            }
            // Application Route
            route("/") {
                get {
                    // landing page
                }
                route("gl") {
                    get {
                        // languages page
                        val languageModel = FetchLanguageViewData(serviceDI.languageRepository)
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
                            val productModel = FetchProductViewData(serviceDI.productCatalog)
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
                                    serviceDI.bookRepository,
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
    }
}
