package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData

class FetchBookViewData(
    private val bookRepo: BookRepository,
    private val languageCode: String
) {
    private val books = bookRepo.getBooks(languageCode = languageCode, resourceId = "ulb")

    fun getListViewData(currentPath: String): List<BookViewData> = books.map {
        BookViewData(
            index = it.index,
            slug = it.slug,
            anglicizedName = it.anglicizedName,
            localizedName = it.localizedName,
            availability = it.availability,
            url = "$currentPath/${it.slug}"
        )
    }

    fun getBookInfo(bookSlug: String): Book? = bookRepo.getBook(bookSlug, languageCode)
}
