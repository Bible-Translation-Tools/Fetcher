package org.bibletranslationtools.fetcher.usecase

import java.io.File
import java.lang.NumberFormatException
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.wycliffeassociates.rcmediadownloader.data.MediaType

class RequestResourceContainer(
    private val rcRepository: ResourceContainerRepository
) {
    private val mediaTypes = listOf(MediaType.WAV, MediaType.MP3)

    fun getResourceContainer(
        parameters: UrlParameters,
        resourceId: String = "ulb"
    ): File? {
        val chapterNumber: Int? = try {
            parameters.chapter.toInt()
        } catch (ex: NumberFormatException) {
            null
        }

        val rcFile = rcRepository.getRC(
            languageCode = parameters.languageCode,
            bookSlug = parameters.bookSlug,
            mediaTypes = mediaTypes,
            chapterNumber = chapterNumber,
            resourceId = resourceId
        )
        // to do: replace path with file server url for download
        return rcFile
    }
}
