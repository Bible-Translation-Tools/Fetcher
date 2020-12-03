package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.LanguageRepository

class LanguageRepositoryImpl(
    private val gatewayCatalog: LanguageCatalog,
    private val heartCatalog: LanguageCatalog
) : LanguageRepository {
    override fun getAll(): List<Language> {
        return getGatewayLanguages() + getHeartLanguages()
    }

    override fun getGatewayLanguages(): List<Language> {
        return gatewayCatalog.getAll()
    }

    override fun getHeartLanguages(): List<Language> {
        return heartCatalog.getAll()
    }

    override fun getLanguage(code: String): Language? {
        return getAll().firstOrNull { it.code == code }
    }

    override fun isGateway(code: String): Boolean {
        return getGatewayLanguages().any { it.code == code }
    }
}
