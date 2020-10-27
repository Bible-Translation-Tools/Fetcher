package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.util.zip.ZipFile
import org.bibletranslationtools.fetcher.repository.ResourceContainerService
import org.wycliffeassociates.rcmediadownloader.RCMediaDownloader
import org.wycliffeassociates.rcmediadownloader.data.MediaDivision
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.data.MediaUrlParameter
import org.wycliffeassociates.rcmediadownloader.io.DownloadClient
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import org.wycliffeassociates.resourcecontainer.ResourceContainer

class RCServiceImpl : ResourceContainerService {
    private val rcRepoUrlTemplate = System.getenv("RC_Repository")
    ?: "https://content.bibletranslationtools.org/WA-Catalog/%s_%s/archive/master.zip"

    override fun getChapterRC(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        resourceId: String
    ): File? {
        // get the rc from git repo
        val downloadClient: IDownloadClient = DownloadClient()
        val templateRC = getTemplateResourceContainer(
            languageCode,
            resourceId,
            downloadClient
        ) ?: return null

        // make new copy of the original
        val newFilePath = templateRC.parentFile.resolve(
            "RC_chapter_$chapterNumber.${templateRC.extension}"
        )
        val rcFile = templateRC.copyTo(newFilePath, true)

        // pass into the download library
        val mediaTypes = listOf(MediaType.WAV, MediaType.MP3)
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
        // verify the chapter is existing
        return if (
            verifyChapterExisting(rcWithMedia, bookSlug, mediaTypes, chapterNumber)
        ) {
            rcWithMedia
        } else null
    }

    override fun getBookRC(
        languageCode: String,
        bookSlug: String,
        resourceId: String
    ): File? {
        val downloadClient: IDownloadClient = DownloadClient()
        val rcFile = getTemplateResourceContainer(
            languageCode,
            resourceId,
            downloadClient
        ) ?: return null

        val mediaTypes = listOf(MediaType.WAV, MediaType.MP3)
        val downloadParameters = MediaUrlParameter(
            projectId = bookSlug,
            mediaDivision = MediaDivision.CHAPTER,
            mediaTypes = mediaTypes,
            chapter = null
        )
        val rcWithMedia = RCMediaDownloader.download(
            rcFile,
            downloadParameters,
            downloadClient,
            overwrite = true
        )
        // verify the content is available
        return if (
            anyChapterInRC(rcWithMedia, bookSlug, mediaTypes)
        ) {
            rcWithMedia
        } else null
    }

    private fun getTemplateResourceContainer(
        languageCode: String,
        resourceId: String,
        downloadClient: IDownloadClient
    ): File? {
        val url = String.format(rcRepoUrlTemplate, languageCode, resourceId)
        // download rc from repo
        val downloadLocation = File(System.getenv("RC_TEMP")).resolve(languageCode)
        downloadLocation.mkdir()
        return downloadClient.downloadFromUrl(url, downloadLocation)
    }

    private fun verifyChapterExisting(
        rcFile: File,
        bookSlug: String,
        mediaTypes: List<MediaType>,
        chapterNumber: Int
    ): Boolean {
        var isExisting = false
        ResourceContainer.load(rcFile).use { rc ->
            val mediaProject = rc.media?.projects?.firstOrNull {
                it.identifier == bookSlug
            }

            for (mediaType in mediaTypes) {
                val media = mediaProject?.media?.firstOrNull {
                    it.identifier == mediaType.name.toLowerCase()
                }
                val pathInRC = media?.chapterUrl ?: continue
                val pathInMediaManifest = pathInRC.replace("{chapter}", chapterNumber.toString())

                ZipFile(rcFile).use { rcZip ->
                    val listEntries = rcZip.entries().toList()
                    isExisting = listEntries.any { entry ->
                        entry.name.contains(pathInMediaManifest)
                    }
                }
                if (isExisting) return true
            }
        }
        return isExisting
    }

    private fun anyChapterInRC(rcFile: File, slug: String, mediaTypes: List<MediaType>): Boolean {
        var isExisting = false
        ResourceContainer.load(rcFile).use { rc ->
            val mediaProject = rc.media?.projects?.firstOrNull {
                it.identifier == slug
            }

            for (mediaType in mediaTypes) {
                val media = mediaProject?.media?.firstOrNull {
                    it.identifier == mediaType.name.toLowerCase()
                }
                val pathInRC = media?.chapterUrl ?: continue
                val chapterPathRegex = pathInRC.replace("{chapter}", "[0-9]{1,3}")

                ZipFile(rcFile).use { rcZip ->
                    val listEntries = rcZip.entries().toList()
                    isExisting = listEntries.any { entry ->
                        entry.name.matches(Regex(".*/$chapterPathRegex\$"))
                    }
                }
                if (isExisting) return true
            }
        }
        return isExisting
    }
}
