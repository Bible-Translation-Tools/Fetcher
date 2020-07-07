package org.bibletranslationtools.fetcher.domain

import java.io.File

class DirectoryProvider {
    fun getContentRoot(): File {
        return File("/")
    }
}
