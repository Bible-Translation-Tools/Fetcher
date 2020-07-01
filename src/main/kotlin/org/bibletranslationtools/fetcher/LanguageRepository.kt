package org.bibletranslationtools.fetcher

import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Language

interface LanguageRepository {
    @Throws(FileNotFoundException::class)
    fun getLanguages(): List<Language>
}
