package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess

class RCRepositoryImpl(
    private val storageAccess: StorageAccess
) : ResourceContainerRepository {
    private val rcTemplateName = "%s_%s"

    override fun getRC(
        languageCode: String,
        resourceId: String
    ): File? {
        val repoName = String.format(rcTemplateName, languageCode, resourceId)
        return storageAccess.getRepoFromFileSystem(repoName)
    }
}
