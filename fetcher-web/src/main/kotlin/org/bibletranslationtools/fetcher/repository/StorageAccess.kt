package org.bibletranslationtools.fetcher.repository

import java.io.File

interface StorageAccess {
    fun getContentRoot(): File
    fun hasLanguageContent(languageCode: String): Boolean
    fun hasProductContent(languageCode: String, fileExtensions: List<String>): Boolean
    fun getBookFile(request: FileAccessRequest): File?
    fun getChapterFile(request: FileAccessRequest): File?
    fun hasBookContent(
        languageCode: String,
        resourceId: String,
        bookSlug: String,
        fileExtensionList: List<String>
    ): Boolean
    fun allocateRCFileLocation(newFileName: String): File
    fun getRepoFromFileSystem(name: String): File?
}
