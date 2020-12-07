package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData

class FetchBookViewData(
    private val bookRepo: BookRepository,
    private val storage: StorageAccess,
    private val language: Language,
    private val product: Product
) {
    private val resourceId = "ulb"
    private val productExtension = ProductFileExtension.getType(product.slug)!!

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val fileExtensionList =
        if (ContainerExtensions.isSupported(productExtension.fileType)) {
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
        val books = bookRepo.getBooks(resourceId = resourceId, languageCode = language.code)
        return books.map { book ->
            book.availability = if (isGateway) {
                cacheAccessor.isBookAvailable(book.slug, language.code, product.slug)
            } else {
                storage.hasBookContent(
                    language.code,
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
            cacheAccessor.getBookUrl(bookSlug, language.code, product.slug)
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
            val fileAccessRequest = when (productExtension) {
                ProductFileExtension.ORATURE -> return "#"
                ProductFileExtension.BTTR -> getBTTRFileAccessRequest(bookSlug, priority)
                ProductFileExtension.MP3 -> getMp3FileAccessRequest(bookSlug, priority)
            }

            val bookFile = storage.getBookFile(fileAccessRequest)
            if (bookFile != null) {
                url = formatBookDownloadUrl(bookFile)
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
            languageCode = language.code,
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
            languageCode = language.code,
            resourceId = resourceId,
            fileExtension = priorityItem.fileExtension,
            bookSlug = bookSlug,
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun formatBookDownloadUrl(bookFile: File): String {
        val relativePath = bookFile.relativeTo(storage.getContentRoot()).invariantSeparatorsPath
        return "${System.getenv("CDN_BASE_URL")}/$relativePath"
    }
}
