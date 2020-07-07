package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Language

interface LanguageCatalog {
    fun getLanguages(): List<Language>
}
