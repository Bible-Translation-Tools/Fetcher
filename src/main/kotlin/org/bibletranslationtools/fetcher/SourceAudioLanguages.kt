package org.bibletranslationtools.fetcher

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList
import org.bibletranslationtools.fetcher.data.Language

class SourceAudioLanguages(private val languageRepository: LanguageRepository) {

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
