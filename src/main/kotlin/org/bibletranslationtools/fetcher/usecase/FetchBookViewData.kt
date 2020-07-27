package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData
import java.io.File

class FetchBookViewData(
    private val bookRepo: BookRepository,
    private val storage: StorageAccess,
    private val languageCode: String
) {
    private val books = bookRepo.getBooks(languageCode = languageCode, resourceId = "ulb")

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

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
        val product = ProductFileExtension.getType(productSlug) ?: return null
        val book = bookRepo.getBook(bookSlug, languageCode)
        var url: String? = null

        for (priority in priorityList) {
            val fileAccessRequest = when (product) {
                ProductFileExtension.BTTR -> getBTTRFileAccessRequest(bookSlug, priority)
                ProductFileExtension.MP3 -> getMp3FileAccessRequest(bookSlug, priority)
            }

            val bookFile = storage.getBookFile(fileAccessRequest)
            if (bookFile != null) {
                url = getBookDownloadUrl(bookFile)
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

    private fun getBTTRFileAccessRequest(
        bookSlug: String,
        priorityItem: PriorityItem
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
        priorityItem: PriorityItem
    ): FileAccessRequest {
        return FileAccessRequest(
            languageCode = languageCode,
            resourceId = "ulb",
            fileExtension = priorityItem.fileExtension,
            bookSlug = bookSlug,
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun getBookDownloadUrl(chapterFile: File): String {
        return chapterFile.relativeTo(storage.getContentRoot()).invariantSeparatorsPath
    }
}
