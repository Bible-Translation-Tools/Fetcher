package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData

class FetchChapterViewData(
    chapterCatalog: ChapterCatalog,
    private val storage: StorageAccess,
    private val languageCode: String,
    productSlug: String, // tr / mp3
    private val bookSlug: String
) {
    private val product = ProductFileExtension.getType(productSlug)

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    private val chapters: List<Chapter> = chapterCatalog.getAll(
        languageCode = languageCode,
        bookSlug = bookSlug
    ).sortedBy { it.number }

    fun getViewDataList(): List<ChapterViewData> {
        val chapterList = mutableListOf<ChapterViewData>()

        for (chapterNumber in 1..chapters.size) {
            var url: String? = null

            for (priority in priorityList) {
                val fileAccessRequest = when (product) {
                    ProductFileExtension.BTTR -> getBTTRFileAccessRequest(chapterNumber, priority)
                    ProductFileExtension.MP3 -> getMp3FileAccessRequest(chapterNumber, priority)
                    else -> null
                }

                val chapterFile = storage.getChapterFile(fileAccessRequest)
                if (chapterFile != null) {
                    url = chapterFile.invariantSeparatorsPath
                    break
                }
            }
            chapterList.add(ChapterViewData(chapterNumber, url))
        }

        return chapterList
    }

    private fun getBTTRFileAccessRequest(chapterNumber: Int, priorityItem: PriorityItem): FileAccessRequest {
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
