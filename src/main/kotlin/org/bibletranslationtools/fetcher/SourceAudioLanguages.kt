package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.data.Language
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

class SourceAudioLanguages {

    private val sourceAudioRoot = "/home/dj/SourceAudio"

    private fun getAvailableLanguageCodes(): List<String> {
        val dirs = Files.list(Paths.get(sourceAudioRoot)).filter { Files.isDirectory(it) }
        return dirs.map{ it.fileName.toString() }.toList()
    }
}
