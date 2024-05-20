package org.bibletranslationtools.fetcher.impl.repository

import com.apollographql.apollo3.ApolloClient
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.fetcher.graphql.generated.GetPrimaryReposQuery
import org.bibletranslationtools.fetcher.repository.SourceCacheAccessor

class SourceTextAccessor : SourceCacheAccessor {
    private var cache: List<GetPrimaryReposQuery.GitRepo> = listOf()

    init {
        buildCache()
    }

    override fun update() {
        buildCache()
    }

    override fun getRepoUrl(languageCode: String, resourceId: String): String? {
        return cache.singleOrNull {
            it.content?.language?.languageCode == languageCode &&
                    it.content.resourceType == resourceId
        }?.repoUrl
    }

    private fun buildCache() {
        val client = ApolloClient.Builder()
            .serverUrl("https://api-biel-dev.walink.org/v1/graphql")
            .build()

        cache = runBlocking {
            val query = GetPrimaryReposQuery()
            val response = client.query(query).execute()
            response.data?.GitRepos ?: listOf()
        }
    }
}