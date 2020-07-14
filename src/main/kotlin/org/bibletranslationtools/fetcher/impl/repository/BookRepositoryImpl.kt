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

    override fun getBooks(language: Language): List<Book> {
        return getBooks(language.code)
    }

    override fun getBook(slug: String, languageCode: String): Book? {
        return if (languageCode == englishLanguageCode) bookCatalog.getBook(slug) else null // This will get the localized name
    }

    override fun getBook(slug: String, language: Language): Book? {
        return getBook(slug, language.code)
    }
}
