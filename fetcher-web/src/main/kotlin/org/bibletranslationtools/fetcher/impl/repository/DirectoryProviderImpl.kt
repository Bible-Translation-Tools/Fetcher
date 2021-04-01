package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.config.CONTENT_ROOT_DIR
import org.bibletranslationtools.fetcher.config.ORATURE_REPO_DIR
import org.bibletranslationtools.fetcher.config.RC_OUTPUT_DIR
import org.bibletranslationtools.fetcher.repository.DirectoryProvider

class DirectoryProviderImpl : DirectoryProvider {
    override fun getContentRoot(): File {
        return File(CONTENT_ROOT_DIR)
    }

    override fun getProjectsDir(languageCode: String, resourceId: String): File {
        return getContentRoot().resolve("$languageCode/$resourceId")
    }

    override fun getChaptersDir(languageCode: String, resourceId: String, bookSlug: String): File {
        return getProjectsDir(languageCode, resourceId).resolve(bookSlug)
    }

    override fun getRCExportDir(): File {
        return File(RC_OUTPUT_DIR).apply { mkdirs() }
    }

    override fun getRCRepositoriesDir(): File {
        return File(ORATURE_REPO_DIR)
    }
}
