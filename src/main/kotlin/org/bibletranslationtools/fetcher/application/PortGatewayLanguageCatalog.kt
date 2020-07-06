package org.bibletranslationtools.fetcher.application

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
import org.bibletranslationtools.fetcher.domain.LanguageCatalog
import org.slf4j.LoggerFactory

const val PORT_LANGUAGE_CODE_ID = "IETF Tag"
const val PORT_ANGLICIZED_NAME_ID = "Name"
const val PORT_LOCALIZED_NAME_ID = "National Name"

class PortGatewayLanguageCatalog : LanguageCatalog {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class PortLanguageModel(
        @JsonProperty(PORT_LANGUAGE_CODE_ID) val code: String,
        @JsonProperty(PORT_ANGLICIZED_NAME_ID) val anglicizedName: String,
        @JsonProperty(PORT_LOCALIZED_NAME_ID) val localizedName: String
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val portLanguageFileName = "port_gateway_languages.csv"

    override fun getLanguages(): List<Language> {
        val languagesFile: File = try {
            getLanguagesFile()
        } catch (e: FileNotFoundException) {
            logger.error("PORT Languages File Not Found")
            return listOf()
        }

        val mapper = CsvMapper().registerModule(KotlinModule())
        val schema = CsvSchema.emptySchema().withHeader()
        val languagesIterator: MappingIterator<PortLanguageModel> = mapper.readerFor(PortLanguageModel::class.java)
            .with(schema)
            .readValues(languagesFile)

        val languageList = mutableListOf<Language>()
        languagesIterator.forEach { languageList.add(Language(it.code, it.anglicizedName, it.localizedName)) }

        return languageList
    }

    @Throws(FileNotFoundException::class)
    private fun getLanguagesFile(): File {
        val portLanguagesResource: URL? = javaClass.classLoader.getResource(portLanguageFileName)
        if (portLanguagesResource == null) {
            throw FileNotFoundException("$portLanguageFileName not found in resources.")
        }

        return File(portLanguagesResource.file)
    }
}
