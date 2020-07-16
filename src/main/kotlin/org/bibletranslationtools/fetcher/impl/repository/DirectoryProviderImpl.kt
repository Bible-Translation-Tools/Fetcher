package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.repository.DirectoryProvider

class DirectoryProviderImpl : DirectoryProvider {
    override fun getContentRoot(): File {
        return File("/")
    }

    override fun getProjectsDir(languageCode: String, dublinCoreId: String): File {
        return getContentRoot().resolve("$languageCode/$dublinCoreId")
    }
}
