package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData

class FetchBookViewData(
    private val bookRepo: BookRepository,
    private val storage: StorageAccess,
    private val languageCode: String
) {
    private val books = bookRepo.getBooks(languageCode = languageCode, resourceId = "ulb")

    fun getViewDataList(currentPath: String): List<BookViewData> = books.map {
        BookViewData(
            index = it.index,
            slug = it.slug,
            anglicizedName = it.anglicizedName,
            localizedName = it.localizedName,
            url = if (it.availability) "$currentPath/${it.slug}" else null
        )
    }

    fun getViewData(bookSlug: String, productSlug: String): BookViewData? {
        val book = bookRepo.getBook(bookSlug, languageCode)
        val product = ProductFileExtension.getType(productSlug)
        var url: String? = null

        for (priority in FetchChapterViewData.priorityList) {
            val fileAccessRequest = when (product) {
                ProductFileExtension.BTTR -> getTrFileAccessRequest(bookSlug, priority)
                ProductFileExtension.MP3 -> getMp3FileAccessRequest(bookSlug, priority)
                else -> null
            }

            val chapterFile = storage.getChapterFile(fileAccessRequest)
            if (chapterFile != null) {
                url = chapterFile.invariantSeparatorsPath
                break
            }
        }

        return if (book != null) BookViewData(
            index = book.index,
            slug = book.slug,
            anglicizedName = book.anglicizedName,
            localizedName = book.localizedName,
            url = url
        ) else {
            null
        }
    }

    private fun getTrFileAccessRequest(
        bookSlug: String,
        priorityItem: FetchChapterViewData.Companion.PriorityItem
    ): FileAccessRequest {
        return FileAccessRequest(
            languageCode = languageCode,
            resourceId = "ulb",
            fileExtension = "tr",
            bookSlug = bookSlug,
            mediaExtension = priorityItem.fileExtension,
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun getMp3FileAccessRequest(
        bookSlug: String,
        priorityItem: FetchChapterViewData.Companion.PriorityItem
    ): FileAccessRequest {
        return FileAccessRequest(
            languageCode = languageCode,
            resourceId = "ulb",
            fileExtension = priorityItem.fileExtension,
            bookSlug = bookSlug,
            mediaQuality = priorityItem.mediaQuality
        )
    }
}
