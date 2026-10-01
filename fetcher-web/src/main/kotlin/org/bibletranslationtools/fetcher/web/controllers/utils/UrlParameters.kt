package org.bibletranslationtools.fetcher.web.controllers.utils

import org.bibletranslationtools.fetcher.usecase.resourceIdByLanguage

class UrlParameters(
    val languageCode: String = "",
    val resourceId: String = resourceIdByLanguage(languageCode),
    val productSlug: String = "",
    val bookSlug: String = "",
    val chapter: String? = null
)
