package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.io.DownloadClient
import org.bibletranslationtools.fetcher.io.IDownloadClient
import java.io.File

interface ChapterRepository {
    fun getAll(languageCode: String, bookSlug: String): List<Chapter>
    fun getChapterRC(
        rcFile: File,
        resourceId: String = "ulb",
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int
    ): File
}