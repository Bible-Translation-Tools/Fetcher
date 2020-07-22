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
    private val product: String, // tr / mp3
    private val bookSlug: String
) {
    private val chapters: List<Chapter> = chapterCatalog.getAll(
        languageCode = languageCode,
        bookSlug = bookSlug
    )

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    fun getListViewData(): Map<Int, ChapterViewData> {
        val chapterList = mutableMapOf<Int, ChapterViewData>()

        for (chapterNumber in 1..chapters.size) {
            var url: String? = null

            for (priority in priorityList) {
                val fileAccessRequest = if (product == "tr") {
                    getTrFileAccessRequest(chapterNumber, priority)
                } else {
                    getMp3FileAccessRequest(chapterNumber, priority)
                }

                val chapterFile = storage.getChapterFile(fileAccessRequest) ?: continue

                url = chapterFile.path
                break
            }

            chapterList[chapterNumber] = ChapterViewData(
                chapterNumber,
                url
            )
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
