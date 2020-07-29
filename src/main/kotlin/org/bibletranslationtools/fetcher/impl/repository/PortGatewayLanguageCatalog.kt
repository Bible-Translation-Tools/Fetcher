package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.slf4j.LoggerFactory

const val PORT_LANGUAGE_CODE_ID = "IETF Tag"
const val PORT_ANGLICIZED_NAME_ID = "Name"
const val PORT_LOCALIZED_NAME_ID = "National Name"

class PortGatewayLanguageCatalog : LanguageCatalog {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class PortGatewayLanguage(
        @JsonProperty(PORT_LANGUAGE_CODE_ID) val code: String,
        @JsonProperty(PORT_ANGLICIZED_NAME_ID) val anglicizedName: String,
        @JsonProperty(PORT_LOCALIZED_NAME_ID) val localizedName: String
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val portLanguageFileName = System.getenv("GATEWAY_LANG_FILE")
    private val languageList: List<Language> = parseCatalog()

    override fun getAll(): List<Language> = this.languageList

    @Throws(FileNotFoundException::class)
    private fun parseCatalog(): List<Language> {
        val languagesFile: File = try {
            getLanguagesFile()
        } catch (e: FileNotFoundException) {
            logger.error("Port Gateway Languages file not found at $portLanguageFileName", e)
            throw e // crash on fatal exception: critical resource not found
        }

        val mapper = CsvMapper().registerModule(KotlinModule())
        val schema = CsvSchema.emptySchema().withHeader()
        val languagesIterator: MappingIterator<PortGatewayLanguage> = mapper.readerFor(
            PortGatewayLanguage::class.java
        )
            .with(schema)
            .readValues(languagesFile)

        val languageList = mutableListOf<Language>()
        languagesIterator.forEach {
            languageList.add(Language(it.code, it.anglicizedName, it.localizedName))
        }

        return languageList
    }

    @Throws(FileNotFoundException::class)
    private fun getLanguagesFile(): File {
        val portFile = File(portLanguageFileName)
        if(!portFile.exists()) {
            throw FileNotFoundException()
        }

        return portFile
    }
}
