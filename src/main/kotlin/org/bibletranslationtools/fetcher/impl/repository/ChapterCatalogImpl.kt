package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.fetcher.repository.ChapterCatalog

class ChapterCatalogImpl : ChapterCatalog {

    private data class Chunk(
        val firstvs: String,
        val id: String,
        val lastvs: String
    ) {
        fun getChapter(): Int = id.split("-")[0].toInt()
    }

    override fun getChapterCount(languageCode: String, bookSlug: String): Int {
        val client = HttpClient()
        val response: ByteArray? = runBlocking {
            try {
                client.get<ByteArray>(getChunksURL(languageCode, bookSlug))
            } catch (ex: ClientRequestException) {
                null
            }
        }

        if (response == null) return 0

        val mapper = ObjectMapper().registerModule(KotlinModule())
        val chunkList: MutableList<Chunk> = mapper.readValue(response)
        val lastChunk = getLastChunk(chunkList)

        return lastChunk.getChapter()
    }

    private fun getChunksURL(languageCode: String, bookSlug: String): String {
        return "https://api.unfoldingword.org/ts/txt/2/$bookSlug/$languageCode/ulb/chunks.json"
    }

    private fun getLastChunk(chunkList: MutableList<Chunk>): Chunk {
        chunkList.sortBy { it.id }
        return chunkList.last()
    }
}
