package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.web.controllers.utils.MediaResourceParameters
import java.io.File
import org.wycliffeassociates.rcmediadownloader.data.MediaType

interface ResourceContainerRepository {
    fun getRC(
        resourceParams: MediaResourceParameters,
        mediaTypes: List<MediaType>,
        chapterNumber: Int?
    ): File?
}
