package org.bibletranslationtools.fetcher.domain

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import java.io.File

class SourceAudioContent(
    private val languageCatalog: LanguageCatalog,
    private val directoryProvider: DirectoryProvider
) {
    fun getLanguages(): List<Language> {
        val availableLanguageCodes = getLanguageCodes(directoryProvider.getSourceAudioRoot())

        return languageCatalog.getLanguages().filter {
            availableLanguageCodes.contains(it.code)
        }
    }

    private fun getLanguageCodes(sourceAudioRoot: File): List<String> {
        val dirs = sourceAudioRoot.listFiles(File::isDirectory)

        if(dirs.isNullOrEmpty()) return listOf()
        return dirs.map { it.name.toString() }.toList()
    }
}
