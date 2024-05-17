package org.bibletranslationtools.fetcher.impl.repository

import com.apollographql.apollo3.ApolloClient
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.fetcher.graphql.generated.GetPrimaryRepoQuery
import org.bibletranslationtools.fetcher.repository.PrimaryRepoRepository

class PrimaryRepoRepositoryImpl : PrimaryRepoRepository {
    override fun fetch(languageCode: String, resourceType: String): String? {
        val client = ApolloClient.Builder()
            .serverUrl("https://api-biel-dev.walink.org/v1/graphql")
            .build()

        return runBlocking {
            val query = client.query(GetPrimaryRepoQuery(lang = languageCode, resType = resourceType))
            val response = query.execute()
            response.data?.git_repo?.singleOrNull()?.repo_url
        }
    }
}