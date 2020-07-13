package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.repository.BookCatalog
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess

class BookRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val bookCatalog: BookCatalog
) : BookRepository {

    override fun getBooks(languageCode: String): List<Book> {
        val books = bookCatalog.getAll()
        val availableBookCodes = storageAccess.getBookSlugs(languageCode)

        books.forEach {
            if (it.slug in availableBookCodes) {
                it.availability = true
            }
        }

        return books
    }
}
