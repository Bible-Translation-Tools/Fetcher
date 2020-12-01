package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
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

    private val fileExtensionList = if (ContainerExtensions.isSupported(product.fileType)) {
        listOf("tr")
    } else {
        listOf("wav", "mp3")
    }

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    fun getViewDataList(
        currentPath: String,
        cacheAccessor: ContentCacheAccessor,
        isGateway: Boolean = true
    ): List<BookViewData> {
        val books = bookRepo.getBooks(resourceId = resourceId, languageCode = languageCode)
        return books.map { book ->
            book.availability = if (isGateway) {
                cacheAccessor.isBookAvailable(book.slug, languageCode, productSlug)
            } else {
                storage.hasBookContent(
                    languageCode,
                    resourceId,
                    book.slug,
                    fileExtensionList
                )
            }

            BookViewData(
                index = book.index,
                slug = book.slug,
                anglicizedName = book.anglicizedName,
                localizedName = book.localizedName,
                url = if (book.availability) "$currentPath/${book.slug}" else null
            )
        }
    }

    fun getViewData(
        bookSlug: String,
        cacheAccessor: ContentCacheAccessor,
        isGateway: Boolean = true
    ): BookViewData? {
        val book = bookRepo.getBook(bookSlug)
        val url = if (isGateway) {
            cacheAccessor.getBookUrl(bookSlug, languageCode, productSlug)
        } else {
            getBookDownloadUrl(bookSlug)
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
                url = getBookDownloadUrl(bookFile)
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

    private fun getBookDownloadUrl(bookFile: File): String {
        val relativePath = bookFile.relativeTo(storage.getContentRoot()).invariantSeparatorsPath
        return "${System.getenv("CDN_BASE_URL")}/$relativePath"
    }
}
