package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.util.zip.ZipFile
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.web.controllers.utils.MediaResourceParameters
import org.wycliffeassociates.rcmediadownloader.RCMediaDownloader
import org.wycliffeassociates.rcmediadownloader.data.MediaDivision
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.data.MediaUrlParameter
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import org.wycliffeassociates.resourcecontainer.ResourceContainer

class RCRepositoryImpl(
    private val downloadClient: IDownloadClient,
    private val util: RCUtils
) : ResourceContainerRepository {
    private val rcRepoTemplateUrl = System.getenv("RC_Repository")
        ?: DEFAULT_REPO_TEMPLATE_URL

    override fun getRC(
        resourceParams: MediaResourceParameters,
        mediaTypes: List<MediaType>,
        chapterNumber: Int?
    ): File? {
        // get the rc from git repo
        val templateRC = getTemplateResourceContainer(
            resourceParams,
            downloadClient
        ) ?: return null

        // make new copy of the original
        val fileName = util.createRCFileName(
            resourceParams,
            extension = templateRC.extension,
            chapter = chapterNumber
        )
        val newFilePath = templateRC.parentFile.resolve(fileName)
        val rcFile = templateRC.copyTo(newFilePath, true)

        // pass into the download library
        val downloadParameters = MediaUrlParameter(
            projectId = resourceParams.bookSlug,
            mediaDivision = MediaDivision.CHAPTER,
            mediaTypes = mediaTypes,
            chapter = chapterNumber
        )
        val rcWithMedia = RCMediaDownloader.download(
            rcFile,
            downloadParameters,
            downloadClient,
            overwrite = true
        )

        // verify the chapter is downloaded properly
        return if (
            util.verifyChapterExists(rcWithMedia, resourceParams.bookSlug, mediaTypes, chapterNumber)
        ) {
            rcWithMedia
        } else null
    }

    private fun getTemplateResourceContainer(
        params: MediaResourceParameters,
        downloadClient: IDownloadClient
    ): File? {
        val url = String.format(rcRepoTemplateUrl, params.languageCode, params.resourceId)
        // download rc from repo
        val downloadLocation = File(System.getenv("RC_TEMP")).resolve(params.languageCode)
        downloadLocation.mkdir()
        return downloadClient.downloadFromUrl(url, downloadLocation)
    }


    private companion object {
        const val DEFAULT_REPO_TEMPLATE_URL =
            "https://content.bibletranslationtools.org/WA-Catalog/%s_%s/archive/master.zip"
    }
}
