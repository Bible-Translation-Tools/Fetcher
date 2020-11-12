package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.util.error
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ChapterCatalogImpl : ChapterCatalog {
    private val catalogUrlTemplate = "https://api.unfoldingword.org/ts/txt/2/%s/en/ulb/chunks.json"
    private val logger = LoggerFactory.getLogger(javaClass)

    private data class Chunk(
        val firstvs: String,
        val id: String,
        val lastvs: String
    ) {
        fun getChapter(): Int = id.split("-")[0].toInt()
    }

    @Throws(IOException::class)
    override fun getAll(languageCode: String, bookSlug: String): List<Chapter> {
        val url = getChunksURL(bookSlug)
        var response: String = ""

        try {
            val conn = (URL(url).openConnection() as HttpURLConnection)
            conn.requestMethod = "GET"
            conn.inputStream.reader().use {
                response = it.readText()
            }
            conn.disconnect()
        } catch (ex: IOException) {
            throw ex
        }

        val mapper = ObjectMapper().registerModule(KotlinModule())
        val chunkList: MutableList<Chunk> = mapper.readValue(response)
        val totalChapters = getLastChunk(chunkList).getChapter()

        val chapterList = mutableListOf<Chapter>()
        for (chapterNum in 1..totalChapters) {
            chapterList.add(Chapter(chapterNum))
        }

        return chapterList
    }

    private fun getChunksURL(bookSlug: String): String {
        return String.format(catalogUrlTemplate, bookSlug)
    }

    private fun getLastChunk(chunkList: MutableList<Chunk>): Chunk {
        chunkList.sortBy { it.id }
        return chunkList.last()
    }
}
