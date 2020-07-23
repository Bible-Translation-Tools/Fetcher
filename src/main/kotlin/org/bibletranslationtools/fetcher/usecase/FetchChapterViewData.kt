package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData

class FetchChapterViewData(
    private val chapterCatalog: ChapterCatalog,
    private val storage: StorageAccess,
    private val languageCode: String,
    private val productSlug: String, // tr / mp3
    private val bookSlug: String
) {
    private val fileType = ProductFileExtension.valueOf(productSlug)
    private val chapters: List<Chapter> = chapterCatalog.getAll(
        languageCode = languageCode,
        bookSlug = bookSlug
    ).sortedBy { it.number }

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    fun getViewDataList(): List<ChapterViewData> {
        val chapterList = mutableListOf<ChapterViewData>()
        val product = ProductFileExtension.getType(productSlug)

        for (chapterNumber in 1..chapters.size) {
            var url: String? = null

            for (priority in priorityList) {
                val fileAccessRequest = when (product) {
                    ProductFileExtension.BTTR -> getTrFileAccessRequest(chapterNumber, priority)
                    ProductFileExtension.MP3 -> getMp3FileAccessRequest(chapterNumber, priority)
                    else -> null
                }

                val chapterFile = storage.getChapterFile(fileAccessRequest)
                if (chapterFile != null) {
                    url = chapterFile.path
                    break
                }
            }
            chapterList.add(ChapterViewData(chapterNumber, url))
        }

        return chapterList
    }

    private fun getTrFileAccessRequest(chapterNumber: Int, priorityItem: PriorityItem): FileAccessRequest {
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

    private fun getMp3FileAccessRequest(chapterNumber: Int, priorityItem: PriorityItem): FileAccessRequest {
        return FileAccessRequest(
            languageCode = languageCode,
            resourceId = "ulb",
            fileExtension = priorityItem.fileExtension,
            bookSlug = bookSlug,
            chapter = chapterNumber.toString(),
            mediaQuality = priorityItem.mediaQuality
        )
    }
}
