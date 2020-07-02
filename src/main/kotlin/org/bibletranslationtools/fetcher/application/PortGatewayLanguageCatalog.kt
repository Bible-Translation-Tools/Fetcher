package org.bibletranslationtools.fetcher.application

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
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
        val languageList = mutableListOf<Language>()
        val rows: List<Map<String, String>> = try {
            getPortLanguagesList()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            listOf()
        }

        for (row in rows) {
            val languageCode = row[PORT_LANGUAGE_CODE_ID] ?: ""
            val anglicizedName = row[PORT_ANGLICIZED_NAME_ID] ?: ""
            val localizedName = row[PORT_LOCALIZED_NAME_ID] ?: ""

            languageList.add(Language(languageCode, anglicizedName, localizedName))
        }

        return languageList
    }

    @Throws(FileNotFoundException::class)
    private fun getPortLanguagesList(): List<Map<String, String>> {
        val portLanguagesResource: URL? = this::class.java.classLoader.getResource(portLanguageFileName)
        if (portLanguagesResource == null) throw FileNotFoundException("$portLanguageFileName not found in resources.")

        val portLanguagesFile = File(portLanguagesResource.file)

        return csvReader().readAllWithHeader(portLanguagesFile)
    }
}
