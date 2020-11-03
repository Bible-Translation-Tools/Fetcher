package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

class RCRepositoryImpl(
    private val directoryProvider: DirectoryProvider,
    private val downloadClient: IDownloadClient
) : ResourceContainerRepository {
    private val rcRepoTemplateUrl = System.getenv("RC_Repository")
        ?: DEFAULT_REPO_TEMPLATE_URL

    override fun getRC(
        languageCode: String,
        resourceId: String
    ): File? {
        val url = String.format(rcRepoTemplateUrl, languageCode, resourceId)
        // download rc from repo
        val downloadLocation = directoryProvider.getDownloadDir()

        return downloadClient.downloadFromUrl(url, downloadLocation)
    }

    private companion object {
        const val DEFAULT_REPO_TEMPLATE_URL =
            "https://content.bibletranslationtools.org/WA-Catalog/%s_%s/archive/master.zip"
    }
}
