package org.bibletranslationtools.fetcher.usecase

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
            url = if (it.availability) "$currentPath/${it.slug}" else null
        )
    }

    fun getBookInfo(bookSlug: String): BookViewData? {
        val book = bookRepo.getBook(bookSlug, languageCode)
        return if (book != null) BookViewData(
            index = book.index,
            slug = book.slug,
            anglicizedName = book.anglicizedName,
            localizedName = book.localizedName,
            url = "" // provided by getBookFile
        ) else {
            null
        }
    }
}
