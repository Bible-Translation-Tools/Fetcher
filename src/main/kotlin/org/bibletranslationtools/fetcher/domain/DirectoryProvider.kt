package org.bibletranslationtools.fetcher.domain

import java.io.File

class DirectoryProvider {
    fun getSourceAudioRoot(): File {
        return File("/SourceAudio")
    }
}
