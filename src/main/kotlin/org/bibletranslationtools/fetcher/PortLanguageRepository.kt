package org.bibletranslationtools.fetcher

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import org.bibletranslationtools.fetcher.data.Language

class PortLanguageRepository : LanguageRepository {

    private val portLanguageFileName = "port_gateway_languages.csv"

    @Throws(FileNotFoundException::class)
    override fun getLanguages(): List<Language> {
        val rows = getPortLanguagesList()
        val languageList = mutableListOf<Language>()

        for (row in rows) {
            val languageCode = row["IETF Tag"] ?: ""
            val anglicizedName = row["Name"] ?: ""
            val localizedName = row["National Name"] ?: ""

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
