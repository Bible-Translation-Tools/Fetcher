package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ContentCacheRepository
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData

class FetchBookViewData(
    private val bookRepo: BookRepository,
    private val storage: StorageAccess,
    private val languageCode: String,
    private val productSlug: String
) {
    private val resourceId = "ulb"
    private val product = ProductFileExtension.getType(productSlug)!!

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    fun getViewDataList(
        currentPath: String,
        contentCache: ContentCacheRepository
    ): List<BookViewData> {
        val books = bookRepo.getBooks(resourceId = resourceId, languageCode = languageCode)
        return books.map { book ->
            book.availability = contentCache.isBookAvailable(book.slug, languageCode, productSlug)
            BookViewData(
                index = book.index,
                slug = book.slug,
                anglicizedName = book.anglicizedName,
                localizedName = book.localizedName,
                url = if (book.availability) "$currentPath/${book.slug}" else null
            )
        }
    }

    fun getViewData(bookSlug: String, cacheRepository: ContentCacheRepository): BookViewData? {
        val book = bookRepo.getBook(bookSlug)
        val url = cacheRepository.getBookUrl(bookSlug, languageCode, productSlug)

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

    fun getBookDownloadUrl(bookSlug: String): String? {
        var url: String? = null
        for (priority in priorityList) {
            val fileAccessRequest = when (product) {
                ProductFileExtension.ORATURE -> return "#"
                ProductFileExtension.BTTR -> getBTTRFileAccessRequest(bookSlug, priority)
                ProductFileExtension.MP3 -> getMp3FileAccessRequest(bookSlug, priority)
            }

            val bookFile = storage.getBookFile(fileAccessRequest)
            if (bookFile != null) {
                val relativeBookPath = bookFile.relativeTo(storage.getContentRoot()).invariantSeparatorsPath
                url = "//${System.getenv("CDN_BASE_URL")}/$relativeBookPath"
                break
            }
        }
        return url
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
}
