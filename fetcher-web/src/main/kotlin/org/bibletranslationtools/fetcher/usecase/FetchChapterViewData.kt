package org.bibletranslationtools.fetcher.usecase

import io.ktor.client.features.ClientRequestException
import java.io.File
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData

class FetchChapterViewData(
    environmentConfig: EnvironmentConfig,
    chapterCatalog: ChapterCatalog,
    private val storage: StorageAccess,
    private val language: Language,
    private val product: Product,
    private val book: Book
) {
    private val productExtension = ProductFileExtension.getType(product.slug)!!
    private val baseUrl = environmentConfig.CDN_BASE_URL

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    private val chapters: List<Chapter> = try {
        chapterCatalog.getAll(
            languageCode = language.code,
            bookSlug = book.slug
        ).sortedBy { it.number }
    } catch (ex: ClientRequestException) {
        throw ex
    }

    fun getViewDataList(
        contentCache: ContentCacheAccessor,
        isGateway: Boolean
    ): List<ChapterViewData> {
        return if (isGateway) {
            chapters.map {
                val requestUrl = contentCache.getChapterUrl(
                    number = it.number,
                    bookSlug = book.slug,
                    languageCode = language.code,
                    productSlug = product.slug
                )
                ChapterViewData(it.number, url = requestUrl)
            }
        } else {
            chaptersFromDirectory()
        }
    }

    fun chaptersFromDirectory(): List<ChapterViewData> {
        val chapterList = mutableListOf<ChapterViewData>()

        for (chapterNumber in 1..chapters.size) {
            var url: String? = null

            for (priority in priorityList) {
                val fileAccessRequest = when (productExtension) {
                    ProductFileExtension.BTTR -> getBTTRFileAccessRequest(chapterNumber, priority)
                    ProductFileExtension.MP3 -> getMp3FileAccessRequest(chapterNumber, priority)
                    else -> return listOf()
                }

                val chapterFile = storage.getChapterFile(fileAccessRequest)
                if (chapterFile != null) {
                    url = formatChapterDownloadUrl(chapterFile)
                    break
                }
            }
            chapterList.add(ChapterViewData(chapterNumber, url))
        }

        return chapterList
    }

    private fun getBTTRFileAccessRequest(
        chapterNumber: Int,
        priorityItem: PriorityItem
    ): FileAccessRequest {
        return FileAccessRequest(
            languageCode = language.code,
            resourceId = "ulb",
            fileExtension = "tr",
            bookSlug = book.slug,
            chapter = chapterNumber.toString(),
            mediaExtension = priorityItem.fileExtension,
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun getMp3FileAccessRequest(
        chapterNumber: Int,
        priorityItem: PriorityItem
    ): FileAccessRequest {
        return FileAccessRequest(
            languageCode = language.code,
            resourceId = "ulb",
            fileExtension = priorityItem.fileExtension,
            bookSlug = book.slug,
            chapter = chapterNumber.toString(),
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun formatChapterDownloadUrl(chapterFile: File): String {
        val relativeChapterPath = chapterFile.relativeTo(storage.getContentRoot()).invariantSeparatorsPath
        return "$baseUrl/$relativeChapterPath"
    }
}
