package org.bibletranslationtools.fetcher.web.controllers.utils

class MediaResourceParameters(
    resourceId: String? = null,
    languageCode: String? = null,
    productSlug: String? = null,
    bookSlug: String? = null,
    chapter: String? = null
) {
    val resourceId: String = resourceId ?: ""
    val languageCode: String = languageCode ?: ""
    val productSlug: String = productSlug ?: ""
    val bookSlug: String = bookSlug ?: ""
    val chapter: String? = chapter
}
