package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import java.io.FileNotFoundException
import java.io.InputStream
import org.slf4j.LoggerFactory

const val UW_LANGUAGE_CODE_ID = "lc"
const val UW_ANGLICIZED_NAME_ID = "ang"
const val UW_LOCALIZED_NAME_ID = "ln"

class UnfoldingWordHeartLanguagesCatalog : LanguageCatalog {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class UnfoldingWordHeartLanguage(
        @JsonProperty(UW_LANGUAGE_CODE_ID) val code: String,
        @JsonProperty(UW_ANGLICIZED_NAME_ID) val anglicizedName: String,
        @JsonProperty(UW_LOCALIZED_NAME_ID) val localizedName: String
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val unfoldingWordLanguageFileName = "/unfolding_word_heart_languages.json"
    private val languageList: List<Language> = parseCatalog()

    override fun getAll(): List<Language> = this.languageList

    override fun getLanguage(code: String): Language? = this.languageList.firstOrNull { it.code == code }

    @Throws(FileNotFoundException::class)
    private fun parseCatalog(): List<Language> {
        val jsonLanguages: InputStream = try {
            getLanguageCatalogFile()
        } catch (e: FileNotFoundException) {
            logger.error("$unfoldingWordLanguageFileName not found in resources.", e)
            throw e // crash on fatal exception: critical resource not found
        }

        val mapper = jacksonObjectMapper().registerModule(KotlinModule())
        val languagesIterator: MappingIterator<UnfoldingWordHeartLanguage> = mapper.readerFor(
            UnfoldingWordHeartLanguage::class.java
        ).readValues(jsonLanguages)

        val languageList = mutableListOf<Language>()
        languagesIterator.forEach {
            languageList.add(Language(it.code, it.anglicizedName, it.localizedName))
        }

        return languageList
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