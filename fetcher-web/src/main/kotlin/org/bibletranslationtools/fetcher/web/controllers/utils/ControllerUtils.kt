package org.bibletranslationtools.fetcher.web.controllers.utils

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.http.HttpStatusCode
import java.lang.IllegalArgumentException
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.slf4j.LoggerFactory

var contentLanguage = listOf<Locale.LanguageRange>()

fun normalizeUrl(path: String): String = java.io.File(path).invariantSeparatorsPath

fun getLanguageName(languageCode: String, resolver: DependencyResolver): String {
    return resolver.languageRepository.getLanguage(languageCode)?.localizedName ?: ""
}

fun getProductTitleKey(productSlug: String, resolver: DependencyResolver): String {
    return resolver.productCatalog.getProduct(productSlug)?.titleKey ?: ""
}

fun getPreferredLocale(languageRanges: List<Locale.LanguageRange>, templateName: String): Locale {
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

fun errorPage(
    titleKey: String,
    messageKey: String,
    errorCode: HttpStatusCode
): ThymeleafContent {
    return ThymeleafContent(
        template = "error",
        model = mapOf(
            "errorTitleKey" to titleKey,
            "errorMessageKey" to messageKey,
            "errorCode" to errorCode.value
        ),
        locale = getPreferredLocale(contentLanguage, "chapters")
    )
}
