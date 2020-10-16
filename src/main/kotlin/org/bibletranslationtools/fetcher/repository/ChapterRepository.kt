package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter
import java.io.File

interface ChapterRepository {
    fun getAll(languageCode: String, bookSlug: String): List<Chapter>
    fun requestChapterRC(
        rcFile: File,
        resourceId: String = "ulb",
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int
    ): File
}