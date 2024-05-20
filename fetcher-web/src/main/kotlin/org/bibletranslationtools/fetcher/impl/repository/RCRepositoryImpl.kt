package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.SourceTextAccessor
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.slf4j.LoggerFactory
import java.io.File


class RCRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val sourceTextAccessor: SourceTextAccessor
) : ResourceContainerRepository {
    private val logger = LoggerFactory.getLogger(javaClass)
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
        return sourceTextAccessor.getRepoUrl(
            languageCode,
            resourceId
        )?.let { repoUrl ->
            val repoName = "${languageCode}_$resourceId"
            val reposDirectory = storageAccess.getReposDir()

            val process = ProcessBuilder()
                .command("git", "clone", repoUrl, repoName)
                .directory(reposDirectory)
                .start()

            val exit = process.waitFor()

            if (exit != 0) {
                logger.error("An error occurred in cloneRepo with exit code: $exit")
            }

            storageAccess.getRepoFromFileSystem(repoName)
        }
    }
}
