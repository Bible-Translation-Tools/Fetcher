package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Book
import java.io.File
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData

class FetchBookViewData(
    private val bookRepo: BookRepository,
    private val storage: StorageAccess,
    private val languageCode: String
) {
    private val resourceId = "ulb"
    private val books: List<Book>

    init {
        books = bookRepo.getBooks(languageCode = languageCode, resourceId = resourceId)
        books.forEach { book ->
            book.availability = storage.hasBookContent(languageCode, resourceId = resourceId,
                bookSlug = book.slug,
                mediaExtensionList = listOf("mp3", "wav")
            )
        }
    }

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
            resourceId = resourceId,
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
            resourceId = resourceId,
            fileExtension = priorityItem.fileExtension,
            bookSlug = bookSlug,
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun getBookDownloadUrl(bookFile: File): String {
        val relativeBookPath = bookFile.relativeTo(storage.getContentRoot()).invariantSeparatorsPath
        return "//${System.getenv("CDN_BASE_URL")}/$relativeBookPath"
    }
}
