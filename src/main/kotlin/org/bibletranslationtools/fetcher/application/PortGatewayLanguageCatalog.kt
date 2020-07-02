package org.bibletranslationtools.fetcher.application

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.domain.LanguageCatalog

const val PORT_LANGUAGE_CODE_ID = "IETF Tag"
const val PORT_ANGLICIZED_NAME_ID = "Name"
const val PORT_LOCALIZED_NAME_ID = "National Name"

class PortGatewayLanguageCatalog : LanguageCatalog {

    private val portLanguageFileName = "port_gateway_languages.csv"

    override fun getLanguages(): List<Language> {
        val languagesFile: File = try {
            getLanguagesFile()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return listOf()
        }

        val languageList = mutableListOf<Language>()
        val mapper = CsvMapper().registerModule(KotlinModule())
        val schema = CsvSchema.emptySchema().withHeader()
        val languagesIterator: MappingIterator<Map<String, String>> = mapper.readerFor(Map::class.java)
            .with(schema)
            .readValues(languagesFile.readText())

        while(languagesIterator.hasNext()) {
            val language = languagesIterator.next()

            val languageCode = language[PORT_LANGUAGE_CODE_ID] ?: ""
            val anglicizedName = language[PORT_ANGLICIZED_NAME_ID] ?: ""
            val localizedName = language[PORT_LOCALIZED_NAME_ID] ?: ""

            languageList.add(Language(languageCode, anglicizedName, localizedName))
        }

        return languageList
    }

    private fun getLanguagesFile(): File {
        val portLanguagesResource: URL? = javaClass.classLoader.getResource(portLanguageFileName)
        if (portLanguagesResource == null) {
            throw FileNotFoundException("$portLanguageFileName not found in resources.")
        }

        return File(portLanguagesResource.file)
    }
}
