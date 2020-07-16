package org.bibletranslationtools.fetcher.web

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.repository.BookRepository

class BookModel(private val bookRepo: BookRepository, private val languageCode: String) {
    val viewData = bookRepo.getBooks(languageCode)
    fun getBookInfo(bookSlug: String): Book? = bookRepo.getBook(bookSlug, languageCode)
}