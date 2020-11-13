package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.BookCatalog
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess

class BookRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val bookCatalog: BookCatalog
) : BookRepository {
    private val englishLanguageCode = "en"

    override fun getBooks(resourceId: String, languageCode: String): List<Book> {
        val books = bookCatalog.getAll()
        // set localized name here

        return books
    }

    override fun getBooks(resourceId: String, language: Language): List<Book> {
        return getBooks(resourceId, language.code)
    }

    override fun getBook(slug: String, languageCode: String): Book? {
        if (languageCode == englishLanguageCode) {
            return bookCatalog.getBook(slug)
        } else {
            return null // This will get the localized name
        }
    }

    override fun getBook(slug: String, language: Language): Book? {
        return getBook(slug, language.code)
    }
}
