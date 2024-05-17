package org.bibletranslationtools.fetcher.impl.repository

import com.apollographql.apollo3.ApolloClient
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.fetcher.graphql.generated.GetPrimaryRepoQuery
import org.bibletranslationtools.fetcher.repository.PrimaryRepoRepository

class PrimaryRepoRepositoryImpl : PrimaryRepoRepository {
    override fun getRepoUrl(languageCode: String, resourceType: String): String? {
        val client = ApolloClient.Builder()
            .serverUrl("https://api-biel-dev.walink.org/v1/graphql")
            .build()

        return runBlocking {
            val query = GetPrimaryRepoQuery(lang = languageCode, resType = resourceType)
            val response = client.query(query).execute()
            response.data?.git_repo?.singleOrNull()?.repo_url
        }
    }
}