package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.repository.BookCatalog

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

    override fun getAll(): List<Book> {
        PORT_ANGLICIZED_NAME_ID
        val jsonBookCatalog: String = try {
            val catalogFile = getBookCatalogFile()
            catalogFile.readText()
        } catch (e: FileNotFoundException) {
            return listOf()
        }

        val booksFromSchema: List<BookSchema> = jacksonObjectMapper().readValue(jsonBookCatalog)
        return booksFromSchema.map {
            Book(
                index = it.index,
                slug = it.slug,
                anglicizedName = it.anglicizedName
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
