package org.bibletranslationtools.fetcher.domain

import org.bibletranslationtools.fetcher.data.Language

interface LanguageRepository {
    fun getLanguages(): List<Language>
}
