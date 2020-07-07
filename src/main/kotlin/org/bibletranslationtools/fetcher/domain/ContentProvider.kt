package org.bibletranslationtools.fetcher.domain

import java.io.File
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog

class ContentProvider(
    private val directoryProvider: DirectoryProvider,
    private val languageCatalog: LanguageCatalog
) {
    fun getLanguages(): List<Language> {
        val availableLanguageCodes = getLanguageCodes(directoryProvider.getContentRoot())

        return languageCatalog.getLanguages().filter {
            availableLanguageCodes.contains(it.code)
        }
    }

    private fun getLanguageCodes(contentRoot: File): List<String> {
        val dirs = contentRoot.listFiles(File::isDirectory)

        if (dirs.isNullOrEmpty()) return listOf()
        return dirs.map { it.name.toString() }.toList()
    }
}
