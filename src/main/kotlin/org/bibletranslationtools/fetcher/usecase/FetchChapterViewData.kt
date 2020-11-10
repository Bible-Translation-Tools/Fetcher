package org.bibletranslationtools.fetcher.usecase

import io.ktor.client.features.ClientRequestException
import java.io.File
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData

class FetchChapterViewData(
    chapterCatalog: ChapterCatalog,
    private val storage: StorageAccess,
    private val languageCode: String,
    productSlug: String,
    private val bookSlug: String
) {
    private val product = ProductFileExtension.getType(productSlug)!!

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    private val chapters: List<Chapter> = try {
        chapterCatalog.getAll(
            languageCode = languageCode,
            bookSlug = bookSlug
        ).sortedBy { it.number }
    } catch (ex: ClientRequestException) {
        throw ex
    }

    fun getViewDataList(): List<ChapterViewData>? {
        return when (product) {
            ProductFileExtension.BTTR, ProductFileExtension.MP3 -> chaptersFromDirectory()
            else -> chaptersForOrature() // all chapters are available
        }
    }

    fun chaptersFromDirectory(): List<ChapterViewData> {
        val chapterList = mutableListOf<ChapterViewData>()

        for (chapterNumber in 1..chapters.size) {
            var url: String? = null

            for (priority in priorityList) {
                val fileAccessRequest = when (product) {
                    ProductFileExtension.BTTR -> getBTTRFileAccessRequest(chapterNumber, priority)
                    ProductFileExtension.MP3 -> getMp3FileAccessRequest(chapterNumber, priority)
                    else -> return listOf()
                }

                val chapterFile = storage.getChapterFile(fileAccessRequest)
                if (chapterFile != null) {
                    url = getChapterDownloadUrl(chapterFile)
                    break
                }
            }
            chapterList.add(ChapterViewData(chapterNumber, url))
        }

        return chapterList
    }

    private fun chaptersForOrature(): List<ChapterViewData> {
        val requestUrl = "#" // front-end handled script
        return chapters.map {
            ChapterViewData(it.number, url = String.format(requestUrl, it.number))
        }
    }

    private fun getBTTRFileAccessRequest(
        chapterNumber: Int,
        priorityItem: PriorityItem
    ): FileAccessRequest {
        return FileAccessRequest(
            languageCode = languageCode,
            resourceId = "ulb",
            fileExtension = "tr",
            bookSlug = bookSlug,
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
            languageCode = languageCode,
            resourceId = "ulb",
            fileExtension = priorityItem.fileExtension,
            bookSlug = bookSlug,
            chapter = chapterNumber.toString(),
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun getChapterDownloadUrl(chapterFile: File): String {
        val relativeChapterPath = chapterFile.relativeTo(storage.getContentRoot()).invariantSeparatorsPath
        return "//${System.getenv("CDN_BASE_URL")}/$relativeChapterPath"
    }
}
