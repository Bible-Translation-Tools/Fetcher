package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.slf4j.LoggerFactory

class CloneRemoteRepo(
    private val storageAccess: StorageAccess
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun cloneRepo(
        repoName: String,
        repoUrl: String
    ) {
        val reposDirectory = storageAccess.getReposRoot()

        val process = ProcessBuilder()
            .command("git", "clone", repoUrl, repoName)
            .directory(reposDirectory)
            .start()

        val exit = process.waitFor()

        if (exit != 0) {
            logger.error("An error occurred in cloneRepo with exit code: $exit")
        }
    }
}