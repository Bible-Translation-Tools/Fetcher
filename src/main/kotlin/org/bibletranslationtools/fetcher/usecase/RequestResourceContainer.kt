package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.wycliffeassociates.rcmediadownloader.data.MediaType

class RequestResourceContainer(
    private val rcRepository: ResourceContainerRepository
) {
    private val mediaTypes = listOf(MediaType.WAV, MediaType.MP3)

    fun getResourceContainer(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int?,
        resourceId: String = "ulb"
    ): File? {
        val rcFile = rcRepository.getRC(
            languageCode = languageCode,
            bookSlug = bookSlug,
            mediaTypes = mediaTypes,
            chapterNumber = chapterNumber,
            resourceId = resourceId
        )
        // to do: replace path with file server url for download
        return rcFile
    }
}
