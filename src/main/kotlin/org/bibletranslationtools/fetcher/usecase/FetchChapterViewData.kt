package org.bibletranslationtools.fetcher.usecase
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.impl.repository.ChapterCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.DirectoryProviderImpl
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData
import java.io.File
import kotlin.system.measureTimeMillis

class FetchChapterMp3ViewData(
    private val chapterCatalog: ChapterCatalog,
    private val storage: StorageAccess,
    private val languageCode: String,
    private val product: String,    // tr / mp3
    private val bookSlug: String
) {
    private val chapters: List<Chapter> = chapterCatalog.getAll(
        languageCode = languageCode,
        bookSlug = bookSlug
    )
    private val priority = listOf(
        Pair<String, String>("mp3", "hi"),
        Pair<String, String>("mp3", "low"),
        Pair<String, String>("wav", "")
    )
    private val priorityMap = listOf(
        mapOf<String, String>("fileExtension" to "mp3", "mediaQuality" to "hi"),
        mapOf<String, String>("fileExtension" to "mp3", "mediaQuality" to "low"),
        mapOf<String, String>("fileExtension" to "wav", "mediaQuality" to "")
    )

    fun getListViewData(): Map<Int, ChapterViewData> {
        val chapterViewData = mutableMapOf<Int, ChapterViewData>()

        for (chapterNumber in 1..chapters.size) {
            chapterViewData[chapterNumber] = ChapterViewData(
                chapterNumber,
                null
            )

            for(pri in priorityMap) {
                val fileAccessRequest = if (product == "tr") {
                    getTrFileAccessRequest(chapterNumber, pri)
                } else {
                    getMp3FileAccessRequest(chapterNumber, pri)
                }

                val chapterFile = storage.getChapterFile(fileAccessRequest) ?: continue

                chapterViewData[chapterNumber]!!.url = chapterFile.path
                break
            }
        }

        return chapterViewData
    }

    private fun getTrFileAccessRequest(chapterNumber: Int, priorityValue: Map<String, String>): FileAccessRequest {
        return FileAccessRequest(
            languageCode,
            "ulb",
            "tr",
            bookSlug,
            chapterNumber.toString(),
            priorityValue["fileExtension"]!!,
            priorityValue["mediaQuality"]!!
        )
    }

    private fun getMp3FileAccessRequest(chapterNumber: Int, priorityValue: Map<String, String>): FileAccessRequest {
        return FileAccessRequest(
            languageCode,
            "ulb",
            priorityValue["fileExtension"]!!,
            bookSlug,
            chapterNumber.toString(),
            "",
            priorityValue["mediaQuality"]!!
        )
    }
}
