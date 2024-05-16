package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData

class FetchBookViewData(
    environmentConfig: EnvironmentConfig,
    private val bookRepo: BookRepository,
    private val storage: StorageAccess,
    private val language: Language,
    private val product: Product
) {
    private val resourceId = resourceIdByLanguage(language.code)
    private val productExtension = ProductFileExtension.getType(product.slug)!!
    private val baseUrl = environmentConfig.CDN_BASE_URL

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val fileExtensionList =
        if (ContainerExtensions.isSupported(productExtension.fileType)) {
            listOf(ProductFileExtension.BTTR.fileType)
        } else {
            listOf(ProductFileExtension.MP3.fileType, ProductFileExtension.WAV.fileType)
        }

    private val priorityList = listOf(
        PriorityItem(ProductFileExtension.MP3.fileType, ProductFileQuality.HI.quality),
        PriorityItem(ProductFileExtension.MP3.fileType, ProductFileQuality.LOW.quality),
        PriorityItem(ProductFileExtension.WAV.fileType, "")
    )

    fun getViewDataList(currentPath: String): List<BookViewData> {
        val books = bookRepo.getBooks(resourceId = resourceId, languageCode = language.code)
        return books.map { book ->
            book.availability = storage.hasBookContent(
                language.code,
                resourceId,
                book.slug,
                fileExtensionList
            )

            BookViewData(
                index = book.index,
                slug = book.slug,
                anglicizedName = book.anglicizedName,
                localizedName = book.localizedName,
                url = if (book.availability) "$currentPath/${book.slug}" else null
            )
        }
    }

    fun getViewData(bookSlug: String): BookViewData? {
        val book = bookRepo.getBook(bookSlug)
        val url = getBookDownloadUrl(bookSlug)

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
                ProductFileExtension.ORATURE -> return "javascript:void(0)"
                ProductFileExtension.BTTR -> getBTTRFileAccessRequest(bookSlug, priority)
                ProductFileExtension.MP3 -> getMp3FileAccessRequest(bookSlug, priority)
                else -> return ""
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
            fileExtension = ProductFileExtension.BTTR.fileType,
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
        return "$baseUrl/$relativePath"
    }
}
