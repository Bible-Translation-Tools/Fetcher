package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language
import java.io.File

interface BookRepository {
    fun getBooks(resourceId: String, languageCode: String = "en"): List<Book> // default to English
    fun getBooks(resourceId: String, language: Language): List<Book>
    fun getBook(slug: String, languageCode: String = "en"): Book? // default to English
    fun getBook(slug: String, language: Language): Book?
    fun getBookRC(slug: String, languageCode: String, resourceId: String = "ulb"): File?
}
