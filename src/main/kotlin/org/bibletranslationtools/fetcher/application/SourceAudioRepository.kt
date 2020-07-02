package org.bibletranslationtools.fetcher.application

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.domain.LanguageCatalog

class SourceAudioLanguages(private val languageRepository: LanguageCatalog) {

    private val sourceAudioRoot = "/SourceAudio"

    fun getAvailableLanguages(): List<Language> {
        val availableLanguageCodes = getAvailableLanguageCodes()
        return languageRepository.getLanguages().filter { availableLanguageCodes.contains(it.languageCode) }
    }

    private fun getAvailableLanguageCodes(): List<String> {
        val dirs = Files.list(Paths.get(sourceAudioRoot)).filter { Files.isDirectory(it) }
        return dirs.map { it.fileName.toString() }.toList()
    }
}