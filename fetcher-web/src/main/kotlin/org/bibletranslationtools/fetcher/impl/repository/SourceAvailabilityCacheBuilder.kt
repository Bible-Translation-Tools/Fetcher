package org.bibletranslationtools.fetcher.impl.repository

import com.apollographql.apollo3.ApolloClient
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.fetcher.graphql.generated.GetPrimaryReposQuery

class SourceAvailabilityCacheBuilder {
    fun build(): List<GetPrimaryReposQuery.GitRepo> {
        val client = ApolloClient.Builder()
            .serverUrl("https://api-biel-dev.walink.org/v1/graphql")
            .build()

        return runBlocking {
            val query = GetPrimaryReposQuery()
            val response = client.query(query).execute()
            response.data?.GitRepos ?: listOf()
        }
    }
}