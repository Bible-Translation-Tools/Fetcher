package org.bibletranslationtools.fetcher.repository

import java.io.File

interface ResourceContainerService {
    fun getChapterRC(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        resourceId: String = "ulb"
    ): File?

    fun getBookRC(languageCode: String, bookSlug: String, resourceId: String = "ulb"): File?
}
