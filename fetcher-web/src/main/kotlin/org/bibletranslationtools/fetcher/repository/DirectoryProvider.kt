package org.bibletranslationtools.fetcher.repository

import java.io.File

interface DirectoryProvider {
    fun getContentRoot(): File
    fun getProjectsDir(languageCode: String, resourceId: String): File
    fun getChaptersDir(languageCode: String, resourceId: String, bookSlug: String): File
    fun getDownloadDir(): File
}
