package org.bibletranslationtools.fetcher.repository

import java.io.File

interface DirectoryProvider {
    fun getSourceFileRootDir(): File
}
