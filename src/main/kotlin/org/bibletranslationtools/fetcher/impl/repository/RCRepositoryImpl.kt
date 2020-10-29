package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.util.zip.ZipFile
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.wycliffeassociates.rcmediadownloader.RCMediaDownloader
import org.wycliffeassociates.rcmediadownloader.data.MediaDivision
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.data.MediaUrlParameter
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import org.wycliffeassociates.resourcecontainer.ResourceContainer

class RCRepositoryImpl(
    private val downloadClient: IDownloadClient
) : ResourceContainerRepository {
    private val rcRepoTemplateUrl = System.getenv("RC_Repository")
        ?: DEFAULT_REPO_TEMPLATE_URL

    override fun getRC(
        languageCode: String,
        bookSlug: String,
        mediaTypes: List<MediaType>,
        chapterNumber: Int?,
        resourceId: String
    ): File? {
        // get the rc from git repo
        val templateRC = getTemplateResourceContainer(
            languageCode,
            resourceId,
            downloadClient
        ) ?: return null

        // make new copy of the original
        val fileName = createRCFileName(
            languageCode = languageCode,
            resourceId = resourceId,
            bookSlug = bookSlug,
            extension = templateRC.extension,
            chapter = chapterNumber
        )
        val newFilePath = templateRC.parentFile.resolve(fileName)
        val rcFile = templateRC.copyTo(newFilePath, true)

        // pass into the download library
        val downloadParameters = MediaUrlParameter(
            projectId = bookSlug,
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
            verifyChapterExists(rcWithMedia, bookSlug, mediaTypes, chapterNumber)
        ) {
            rcWithMedia
        } else null
    }

    private fun getTemplateResourceContainer(
        languageCode: String,
        resourceId: String,
        downloadClient: IDownloadClient
    ): File? {
        val url = String.format(rcRepoTemplateUrl, languageCode, resourceId)
        // download rc from repo
        val downloadLocation = File(System.getenv("RC_TEMP")).resolve(languageCode)
        downloadLocation.mkdir()
        return downloadClient.downloadFromUrl(url, downloadLocation)
    }

    private fun createRCFileName(
        languageCode: String,
        resourceId: String,
        bookSlug: String,
        extension: String,
        chapter: Int?
    ): String {
        return if (chapter == null) {
            "${languageCode}_${resourceId}_$bookSlug.$extension" // book rc
        } else {
            "${languageCode}_${resourceId}_${bookSlug}_c$chapter.$extension" // chapter rc
        }
    }

    private fun verifyChapterExists(
        rcFile: File,
        bookSlug: String,
        mediaTypes: List<MediaType>,
        chapterNumber: Int?
    ): Boolean {
        var isExisting = false
        val chapterNumberPattern = chapterNumber?.toString() ?: "[0-9]{1,3}"

        ResourceContainer.load(rcFile).use { rc ->
            val mediaProject = rc.media?.projects?.firstOrNull {
                it.identifier == bookSlug
            }

            for (mediaType in mediaTypes) {
                val media = mediaProject?.media?.firstOrNull {
                    it.identifier == mediaType.name.toLowerCase()
                }
                val pathInRC = media?.chapterUrl ?: continue
                val chapterPath = pathInRC.replace("{chapter}", chapterNumberPattern)

                ZipFile(rcFile).use { rcZip ->
                    val listEntries = rcZip.entries().toList()
                    isExisting = listEntries.any { entry ->
                        entry.name.matches(Regex(".*/$chapterPath\$"))
                    }
                }
            }
            if (isExisting) return true
        }

        return isExisting
    }

    private companion object {
        const val DEFAULT_REPO_TEMPLATE_URL =
            "https://content.bibletranslationtools.org/WA-Catalog/%s_%s/archive/master.zip"
    }
}
