package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.repository.ResourceContainerService

class RequestResourceContainer(private val service: ResourceContainerService) {
    fun getChapterRC(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int
    ): File? {
        val rcFile = service.getChapterRC(
            languageCode = languageCode,
            bookSlug = bookSlug,
            chapterNumber = chapterNumber,
            resourceId = "ulb")
        // to do: replace path with file server url for download
        return rcFile
    }

    fun getBookRC(languageCode: String, bookSlug: String): File? {
        val rcFile = service.getBookRC(
            languageCode = languageCode,
            bookSlug = bookSlug,
            resourceId = "ulb"
        )
        // to do: replace path with file server url for download
        return rcFile
    }
}
