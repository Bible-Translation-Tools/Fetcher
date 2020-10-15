package org.bibletranslationtools.fetcher.web.controllers.utils

import dev.jbs.ktor.thymeleaf.ThymeleafContent
import io.ktor.http.HttpStatusCode
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

object ParamKeys {
    const val languageParamKey = "languageCode"
    const val productParamKey = "productSlug"
    const val bookParamKey = "bookSlug"
}

data class Params(
    private val lc: String? = null, // language code
    private val ps: String? = null, // product slug
    private val bs: String? = null // book slug
) {
    val languageCode = lc ?: ""
    val productSlug = ps ?: ""
    val bookSlug = bs ?: ""
}

fun normalizeUrl(path: String): String = java.io.File(path).invariantSeparatorsPath

fun getLanguageName(languageCode: String, resolver: DependencyResolver): String {
    return resolver.languageCatalog.getLanguage(languageCode)?.localizedName ?: ""
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
    errorCode: HttpStatusCode,
    contentLanguage: List<Locale.LanguageRange>
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