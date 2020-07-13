package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Book

interface BookRepository {
    fun getBooks(languageCode: String): List<Book>
}
