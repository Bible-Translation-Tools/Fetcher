package org.bibletranslationtools.fetcher.repository

import java.io.File
import org.wycliffeassociates.rcmediadownloader.data.MediaType

interface ResourceContainerRepository {
    fun getRC(
        languageCode: String,
        bookSlug: String,
        mediaTypes: List<MediaType>,
        chapterNumber: Int?,
        resourceId: String = "ulb"
    ): File?
}
