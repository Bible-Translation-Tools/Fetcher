package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.repository.ResourceContainerService

class RequestResourceContainer(private val service: ResourceContainerService) {
    fun getChapterRC(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        resourceId: String = "ulb"
    ): File? {
        val rcFile = service.getChapterRC(languageCode, bookSlug, chapterNumber, resourceId)
        // to do: replace path with file server url for download
        return rcFile
    }
}
