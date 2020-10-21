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
    ): File {
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
        val rc = RCMediaDownloader.download(
            rcFile,
            downloadParameters,
            downloadClient
        )
        // verify the chapter is existing
        return rcFile ?: File("")
    }

    private fun getTemplateResourceContainer(
        languageCode: String,
        resourceId: String,
        downloadClient: IDownloadClient
    ): File? {
        val url = String.format(rcRepoUrlTemplate, languageCode, resourceId)
        // download rc from repo
        val downloadLocation = File("path/on/file/system")

        return downloadClient.downloadFile(url, downloadLocation)
    }
}