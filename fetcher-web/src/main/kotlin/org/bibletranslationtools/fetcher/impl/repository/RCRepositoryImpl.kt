package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.repository.PrimaryRepoRepository
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.CloneRemoteRepo
import java.io.File


class RCRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val primaryRepoRepository: PrimaryRepoRepository
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
        return primaryRepoRepository.getRepoUrl(
            languageCode,
            resourceId
        )?.let { repoUrl ->
            val repoName = "${languageCode}_$resourceId"

            CloneRemoteRepo(storageAccess)
                .cloneRepo(repoName, repoUrl)

            storageAccess.getRepoFromFileSystem(repoName)
        }
    }
}
