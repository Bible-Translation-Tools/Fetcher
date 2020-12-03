package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Language

interface LanguageRepository {
    fun getAll(): List<Language>
    fun getGatewayLanguages(): List<Language>
    fun getHeartLanguages(): List<Language>
    fun getLanguage(code: String): Language?
    fun isGateway(code: String): Boolean
}
