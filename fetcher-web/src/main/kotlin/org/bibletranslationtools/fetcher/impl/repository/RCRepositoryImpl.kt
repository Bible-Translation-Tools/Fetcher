package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.SourceCacheAccessor
import org.bibletranslationtools.fetcher.repository.StorageAccess
import java.io.File


class RCRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val sourceCacheAccessor: SourceCacheAccessor
) : ResourceContainerRepository {
    private val rcTemplateName = "%s_%s"

    override fun getRC(
        languageCode: String,
        resourceId: String
    ): File? {
        val repoName = String.format(rcTemplateName, languageCode, resourceId)
        return storageAccess.getRepoFromFileSystem(repoName)
    }

    override fun downloadRC(
        languageCode: String,
        resourceId: String
    ): File? {
        return sourceCacheAccessor.getRepoUrl(
            languageCode,
            resourceId
        )?.let { repoUrl ->
            val repoName = "${languageCode}_$resourceId"

            sourceCacheAccessor.downloadRepo(repoName, repoUrl)
            storageAccess.getRepoFromFileSystem(repoName)
        }
    }
}
