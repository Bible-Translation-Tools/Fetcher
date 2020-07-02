package org.bibletranslationtools.fetcher.application

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.domain.LanguageCatalog

class SourceAudioRepository(private val languageRepository: LanguageCatalog) {

    private val sourceAudioRoot = "/SourceAudio"

    fun getLanguages(): List<Language> {
        val availableLanguageCodes = getLanguageCodes()
        return languageRepository.getLanguages().filter {
            availableLanguageCodes.contains(it.code)
        }
    }

    private fun getLanguageCodes(): List<String> {
        val dirs = Files.list(Paths.get(sourceAudioRoot)).filter { Files.isDirectory(it) }
        return dirs.map { it.fileName.toString() }.toList()
    }
}
