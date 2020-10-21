package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ChapterRepository
import java.io.File
import org.wycliffeassociates.rcmediadownloader.RCMediaDownloader
import org.wycliffeassociates.rcmediadownloader.data.MediaDivision
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.data.MediaUrlParameter
import org.wycliffeassociates.rcmediadownloader.io.DownloadClient
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import org.wycliffeassociates.resourcecontainer.ResourceContainer
import java.util.zip.ZipFile

class ChapterRepositoryImpl(
    private val chapterCatalog: ChapterCatalog
) : ChapterRepository {
    private val rcRepoUrlTemplate = "https://content.bibletranslationtools.org/WA-Catalog/%s_%s/archive/master.zip"

    override fun getAll(languageCode: String, bookSlug: String): List<Chapter> {
        return chapterCatalog.getAll(languageCode, bookSlug)
    }

    override fun getChapterRC(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        resourceId: String
    ): File? {
        // get the rc from git repo
        val downloadClient: IDownloadClient = DownloadClient()
        val rcFile = getTemplateResourceContainer(
            languageCode,
            resourceId,
            downloadClient
        )!!

        // pass into the download library
        val downloadParameters = MediaUrlParameter(
            projectId = bookSlug,
            mediaDivision = MediaDivision.CHAPTER,
            mediaTypes = listOf(MediaType.WAV),
            chapter = chapterNumber
        )
        val downloadedRC = RCMediaDownloader.download(
            rcFile,
            downloadParameters,
            downloadClient,
            overwrite = true
        )
        // verify the chapter is existing
        return if (verifyChapterExisting(downloadedRC, bookSlug, MediaType.WAV, chapterNumber)) {
            rcFile
        } else null

    }

    private fun getTemplateResourceContainer(
        languageCode: String,
        resourceId: String,
        downloadClient: IDownloadClient
    ): File? {
        val url = String.format(rcRepoUrlTemplate, languageCode, resourceId)
        // download rc from repo
        val downloadLocation = File("path/on/file/system")

        return downloadClient.downloadFromUrl(url, downloadLocation)
    }

    private fun verifyChapterExisting(
        rcFile: File,
        bookSlug: String,
        mediaType: MediaType,
        chapterNumber: Int
    ): Boolean {
        var pathInRC: String? = null
        ResourceContainer.load(rcFile).use { rc ->
            val mediaProject = rc.media?.projects?.firstOrNull {
                it.identifier == bookSlug
            }
            val media = mediaProject?.media?.firstOrNull {
                it.identifier == mediaType.name.toLowerCase()
            }
            pathInRC = media?.chapterUrl
        }
        if (pathInRC == null) return false

        var isExisting = false
        val pathInMediaManifest = pathInRC!!.replace("{chapter}", chapterNumber.toString())
        ZipFile(rcFile).use { rcZip ->
            val listEntries = rcZip.entries().toList()
            isExisting = listEntries.any { entry ->
                entry.name.contains(pathInMediaManifest)
            }
        }
        return isExisting
    }
}