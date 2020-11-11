package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Language

interface LanguageCatalog {
    fun getAll(): List<Language>
    fun getLanguage(code: String): Language?
}
