package org.bibletranslationtools.fetcher.web.controllers

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.thymeleaf.ThymeleafContent
import org.bibletranslationtools.fetcher.web.controllers.utils.GL_ROUTE
import org.bibletranslationtools.fetcher.web.controllers.utils.contentLanguage
import org.bibletranslationtools.fetcher.web.controllers.utils.getPreferredLocale

fun Routing.homeController() {
    route("/") {
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
    }
}
