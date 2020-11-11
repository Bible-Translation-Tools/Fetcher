package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Language

interface LanguageRepository {
    fun getLanguages(): List<Language>
}
