package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sun.org.slf4j.internal.LoggerFactory
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog

const val UW_LANGUAGE_CODE_ID = "lc"
const val UW_ANGLICIZED_NAME_ID = "ang"
const val UW_LOCALIZED_NAME_ID = "ln"

class UnfoldingWordHeartLanguagesCatalog : LanguageCatalog {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class UnfoldingWordGatewayLanguage(
        @JsonProperty(UW_LANGUAGE_CODE_ID) val code: String,
        @JsonProperty(UW_ANGLICIZED_NAME_ID) val anglicizedName: String,
        @JsonProperty(UW_LOCALIZED_NAME_ID) val localizedName: String
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val unfoldingWordLanguageFileName = "/unfolding_word_heart_languages.json"
    private val languageList: List<Language> = parseCatalog()

    override fun getAll(): List<Language> = this.languageList

    override fun getLanguage(code: String): Language? = this.languageList.firstOrNull { it.code == code }

    private fun parseCatalog(): List<Language> {
        

        return listOf()
    }

}