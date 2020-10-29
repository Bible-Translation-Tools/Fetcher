package org.bibletranslationtools.fetcher.repository

import org.wycliffeassociates.rcmediadownloader.data.MediaType
import java.io.File

interface ResourceContainerRepository {
    fun getRC(
        languageCode: String,
        bookSlug: String,
        mediaTypes: List<MediaType>,
        chapterNumber: Int?,
        resourceId: String = "ulb"
    ): File?
}
