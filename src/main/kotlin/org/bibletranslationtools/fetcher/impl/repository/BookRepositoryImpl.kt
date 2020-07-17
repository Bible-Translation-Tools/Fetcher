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

    override fun getBooks(languageCode: String, resourceContainer: String): List<Book> {
        val books = bookCatalog.getAll()
        val availableBookCodes = storageAccess.getBookSlugs(languageCode, resourceContainer)

        books.forEach {
            if (it.slug in availableBookCodes) {
                it.availability = true
            }
        }

        return books
    }

    override fun getBooks(language: Language, resourceContainer: String): List<Book> {
        return getBooks(language.code, resourceContainer)
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
