package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.FileType

interface FileTypeCatalog {
    fun getFileTypes(): List<FileType>
}
