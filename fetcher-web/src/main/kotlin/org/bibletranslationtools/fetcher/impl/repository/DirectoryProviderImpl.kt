package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import java.lang.NullPointerException

class DirectoryProviderImpl : DirectoryProvider {
    @Throws(NullPointerException::class)
    override fun getContentRoot(): File {
        return File(System.getenv("CONTENT_ROOT"))
    }

    override fun getProjectsDir(languageCode: String, resourceId: String): File {
        return getContentRoot().resolve("$languageCode/$resourceId")
    }

    override fun getChaptersDir(languageCode: String, resourceId: String, bookSlug: String): File {
        return getProjectsDir(languageCode, resourceId).resolve(bookSlug)
    }

    @Throws(NullPointerException::class)
    override fun getRCExportDir(): File {
        return File(System.getenv("RC_TEMP_DIR")).apply { mkdirs() }
    }

    @Throws(NullPointerException::class)
    override fun getRCRepositoriesDir(): File {
        return File(System.getenv("ORATURE_REPO_DIR"))
    }
}
