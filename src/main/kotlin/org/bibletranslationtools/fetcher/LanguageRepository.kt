package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.entities.Language

interface LanguageRepository {
    fun getLanguages(): List<Language>
}