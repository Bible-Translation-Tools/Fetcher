package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.FileNotFoundException
import java.io.InputStream
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.slf4j.LoggerFactory

private const val LANGUAGE_CODE_ID = "lc"
private const val ANGLICIZED_NAME_ID = "ang"
private const val LOCALIZED_NAME_ID = "ln"
private const val IS_GATEWAY = "gw"

class UnfoldingWordHeartLanguagesCatalog : LanguageCatalog {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class UnfoldingWordHeartLanguage(
        @JsonProperty(LANGUAGE_CODE_ID) val code: String,
        @JsonProperty(ANGLICIZED_NAME_ID) val anglicizedName: String,
        @JsonProperty(LOCALIZED_NAME_ID) val localizedName: String,
        @JsonProperty(IS_GATEWAY) val isGateway: Boolean
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val unfoldingWordLanguageFileName = "/unfolding_word_heart_languages.json"
    private val languageList: List<Language> = parseCatalog()

    override fun getAll(): List<Language> = this.languageList

    override fun getLanguage(code: String): Language? = this.languageList.firstOrNull { it.code == code }

    @Throws(FileNotFoundException::class)
    private fun parseCatalog(): List<Language> {
        val jsonInputStream: InputStream = try {
            getLanguageCatalogFile()
        } catch (e: FileNotFoundException) {
            logger.error("$unfoldingWordLanguageFileName not found in resources.", e)
            throw e // crash on fatal exception: critical resource not found
        }

        jsonInputStream.use {
            val languages: List<UnfoldingWordHeartLanguage> =
                jacksonObjectMapper().readValue(jsonInputStream)

            return languages
                .filter {
                    !it.isGateway
                }
                .map {
                    Language(
                        it.code,
                        it.anglicizedName,
                        it.localizedName
                    )
                }
        }
    }

    @Throws(FileNotFoundException::class)
    private fun getLanguageCatalogFile(): InputStream {
        val catalogFileStream = javaClass.getResourceAsStream(unfoldingWordLanguageFileName)
        if (catalogFileStream == null) {
            throw FileNotFoundException()
        }

        return catalogFileStream
    }
}
