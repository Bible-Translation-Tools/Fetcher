package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.slf4j.LoggerFactory

class ChapterCatalogImpl : ChapterCatalog {
    private val catalogUrlTemplate = "https://api.unfoldingword.org/ts/txt/2/%s/en/ulb/chunks.json"
    private val logger = LoggerFactory.getLogger(javaClass)

    private data class Chunk(
        val firstvs: String,
        val id: String,
        val lastvs: String
    ) {
        // e.g. "id": "04-06", then chapter number is 04
        val chapterNumber: Int = id.split('-').first().toInt()
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
            logger.error("An error occurred when getting chapter catalog: $languageCode - $bookSlug", ex)
            throw ex
        }

        val mapper = ObjectMapper().registerModule(KotlinModule())
        val chunkList: MutableList<Chunk> = mapper.readValue(response)
        val totalChapters = getLastChunk(chunkList).chapterNumber

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
        chunkList.sortBy { it.chapterNumber }
        return chunkList.last()
    }
}
