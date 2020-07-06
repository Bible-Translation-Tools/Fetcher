package org.bibletranslationtools.fetcher.application

import java.io.File

class DirectoryProvider {
    fun getSourceAudioDir(): File {
        return File("/home/dj/SourceAudio")
    }
}
