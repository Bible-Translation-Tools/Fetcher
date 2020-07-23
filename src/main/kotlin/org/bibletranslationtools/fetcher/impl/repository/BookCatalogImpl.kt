package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.repository.BookCatalog
import org.slf4j.LoggerFactory

class BookCatalogImpl : BookCatalog {

    private companion object {
        const val CATALOG_SLUG_ID = "slug"
        const val CATALOG_NUMBER_ID = "num"
        const val CATALOG_ANGLICIZED_NAME_ID = "name"
        const val BOOK_CATALOG_FILE_NAME = "book_catalog.json"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class BookSchema(
        @JsonProperty(CATALOG_SLUG_ID) val slug: String,
        @JsonProperty(CATALOG_NUMBER_ID) val index: Int,
        @JsonProperty(CATALOG_ANGLICIZED_NAME_ID) val anglicizedName: String
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val books: List<Book> = parseCatalog()

    override fun getAll(): List<Book> = this.books

    override fun getBook(slug: String): Book? {
        if (slug.isNullOrEmpty()) return null
        val book = books.firstOrNull { it.slug == slug }
        return book?.apply { book.localizedName = book.anglicizedName }
    }

    private fun parseCatalog(): List<Book> {
        val jsonBookCatalog: String = try {
            val catalogFile = getBookCatalogFile()
            catalogFile.readText()
        } catch (e: FileNotFoundException) {
            logger.error("$BOOK_CATALOG_FILE_NAME file not found", e)
            throw e // crash on fatal exception: critical resource not found
        }

        val booksFromSchema: List<BookSchema> = jacksonObjectMapper().readValue(jsonBookCatalog)
        return booksFromSchema.map {
            Book(
                index = it.index,
                slug = it.slug,
                anglicizedName = it.anglicizedName,
                localizedName = it.anglicizedName
            )
        }
    }

    @Throws(FileNotFoundException::class)
    private fun getBookCatalogFile(): File {
        val catalogFileURL = javaClass.classLoader.getResource(BOOK_CATALOG_FILE_NAME)
            ?: throw FileNotFoundException()

        return File(catalogFileURL.path)
    }
}
