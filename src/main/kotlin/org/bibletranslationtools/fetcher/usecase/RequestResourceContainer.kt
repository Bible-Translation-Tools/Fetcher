package org.bibletranslationtools.fetcher.usecase

import java.io.File
import java.lang.NumberFormatException
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.web.controllers.utils.ALL_CHAPTERS_PARAM
import org.bibletranslationtools.fetcher.web.controllers.utils.MediaResourceParameters
import org.wycliffeassociates.rcmediadownloader.data.MediaType

class RequestResourceContainer(
    private val rcRepository: ResourceContainerRepository
) {
    private val mediaTypes = listOf(MediaType.WAV, MediaType.MP3)

    fun getResourceContainer(
        parameters: MediaResourceParameters
    ): File? {
        val chapterNumber: Int? = try {
            parameters.chapter?.toInt()
        } catch (ex: NumberFormatException) {
            null
        }

        val rcFile = rcRepository.getRC(
            parameters,
            mediaTypes = mediaTypes,
            chapterNumber = chapterNumber
        )
        // to do: replace path with file server url for download
        return rcFile
    }
}
