package org.bibletranslationtools.fetcher.domain

import org.bibletranslationtools.fetcher.data.Language

interface LanguageCatalog {
    fun getLanguages(): List<Language>
}
