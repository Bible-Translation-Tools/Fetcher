package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess

class LanguageRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val languageCatalog: LanguageCatalog
) : LanguageRepository {
    override fun getLanguages(): List<Language> {
        val languages = languageCatalog.getAll()

        languages.forEach {
            it.availability = true
        }

        return languages
    }
}
