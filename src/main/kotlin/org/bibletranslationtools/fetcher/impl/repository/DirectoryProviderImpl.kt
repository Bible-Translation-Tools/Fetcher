package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.repository.DirectoryProvider

class DirectoryProviderImpl : DirectoryProvider {
    private val dublinCoreId = "/ulb"

    override fun getContentRoot(): File {
        return File("/")
    }

    override fun getProjectsDir(languageCode: String): File {
        return getContentRoot().resolve("$languageCode/$dublinCoreId")
    }

    override fun getChaptersDir(languageCode: String, bookSlug: String): File {
        return getProjectsDir(languageCode).resolve(bookSlug)
    }
}
