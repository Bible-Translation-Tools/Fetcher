package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.graphql.generated.GetPrimaryReposQuery
import org.bibletranslationtools.fetcher.repository.SourceCacheAccessor
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.slf4j.LoggerFactory

class SourceAvailabilityCacheAccessor(
    private val cacheBuilder: SourceAvailabilityCacheBuilder,
    private val storageAccess: StorageAccess
) : SourceCacheAccessor {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var cache: List<GetPrimaryReposQuery.GitRepo>

    init {
        cache = cacheBuilder.build()
    }

    override fun update() {
        cache = cacheBuilder.build()
    }

    override fun getRepoUrl(languageCode: String, resourceId: String): String? {
        return cache.singleOrNull {
            it.content?.language?.languageCode == languageCode &&
                    it.content.resourceType == resourceId
        }?.repoUrl
    }

    override fun downloadRepo(repoName: String, repoUrl: String) {
        val reposDirectory = storageAccess.getReposDir()

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