package org.bibletranslationtools.fetcher.application

import java.io.File

class DirectoryProvider {
    fun getSourceAudioRoot(): File {
        return File("/SourceAudio")
    }
}
