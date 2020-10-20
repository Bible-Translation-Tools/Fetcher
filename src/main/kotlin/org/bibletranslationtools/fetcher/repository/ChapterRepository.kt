package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter
import java.io.File

interface ChapterRepository {
    fun getAll(languageCode: String, bookSlug: String): List<Chapter>
    fun getChapterRC(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        resourceId: String = "ulb"
    ): File
}