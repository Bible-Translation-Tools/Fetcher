package org.bibletranslationtools.fetcher.application

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.domain.LanguageCatalog
import java.io.File

class SourceAudioRepository(
    private val languageCatalog: LanguageCatalog,
    private val directoryProvider: DirectoryProvider
) {
    fun getLanguages(): List<Language> {
        val availableLanguageCodes = getLanguageCodes()

        return languageCatalog.getLanguages().filter {
            availableLanguageCodes.contains(it.code)
        }
    }

    private fun getLanguageCodes(): List<String> {
        val sourceAudioRoot = directoryProvider.getSourceAudioDir()
        val dirs = sourceAudioRoot.listFiles(File::isDirectory)

        if(dirs.isNullOrEmpty()) return listOf()
        return dirs.map { it.name.toString() }.toList()
    }
}
