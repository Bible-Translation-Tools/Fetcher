package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.repository.DirectoryProvider

class DirectoryProviderImpl(private val env: EnvironmentConfig) : DirectoryProvider {
    override fun getContentRoot(): File {
        return File(env.CONTENT_ROOT_DIR)
    }

    override fun getProjectsDir(languageCode: String, resourceId: String): File {
        return getContentRoot().resolve("$languageCode/$resourceId")
    }

    override fun getChaptersDir(languageCode: String, resourceId: String, bookSlug: String): File {
        return getProjectsDir(languageCode, resourceId).resolve(bookSlug)
    }

    override fun getRCExportDir(): File {
        return File(env.RC_OUTPUT_DIR).apply { mkdirs() }
    }

    override fun getRCRepositoriesDir(): File {
        return File(env.ORATURE_REPO_DIR)
    }
}
