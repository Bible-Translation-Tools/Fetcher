package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.data.Language

interface LanguageRepository {
    fun getLanguages(): List<Language>
}
