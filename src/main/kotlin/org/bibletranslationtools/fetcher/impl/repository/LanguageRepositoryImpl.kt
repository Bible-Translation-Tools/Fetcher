package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.ContentCacheRepository
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.DependencyResolver

class LanguageRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val languageCatalog: LanguageCatalog
) : LanguageRepository {
    override fun getLanguages(): List<Language> {
        val availableLanguageCodes = storageAccess.getLanguageCodes()
        val languages = languageCatalog.getAll()

        languages.forEach {
            it.availability = it.code in availableLanguageCodes
        }

        return languages
    }
}
