package org.bibletranslationtools.fetcher.repository

import java.io.File

interface DirectoryProvider {
    fun getContentRoot(): File
    fun getProjectsDir(languageCode: String): File
    fun getChaptersDir(languageCode: String, bookSlug: String): File
}
