package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.repository.DirectoryProvider

class DirectoryProviderImpl : DirectoryProvider {
    override fun getContentRoot(): File {
        return File(System.getenv("CONTENT_ROOT"))
    }

    override fun getProjectsDir(languageCode: String, resourceId: String): File {
        return getContentRoot().resolve("$languageCode/$resourceId")
    }

    override fun getChaptersDir(languageCode: String, resourceId: String, bookSlug: String): File {
        return getProjectsDir(languageCode, resourceId).resolve(bookSlug)
    }

    override fun getRCExportDir(): File {
        return File(System.getenv("RC_TEMP_DIR")).apply { mkdirs() }
    }

    override fun getRCRepositoriesDir(): File {
        return File(System.getenv("ORATURE_REPO_DIR"))
    }
}
