package org.bibletranslationtools.fetcher.repository

import java.io.File
import org.bibletranslationtools.fetcher.data.Chapter

interface ChapterRepository {
    fun getAll(languageCode: String, bookSlug: String): List<Chapter>
    fun getChapterRC(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        resourceId: String = "ulb"
    ): File?
}
