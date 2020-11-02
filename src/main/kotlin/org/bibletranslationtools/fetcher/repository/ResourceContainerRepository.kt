package org.bibletranslationtools.fetcher.repository

import java.io.File
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

interface ResourceContainerRepository {
    fun getRC(
        languageCode: String,
        resourceId: String
    ): File?
}
