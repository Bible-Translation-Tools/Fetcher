package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.io.DownloadClient
import org.bibletranslationtools.fetcher.io.IDownloadClient
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ChapterRepository
import java.io.File

class ChapterRepositoryImpl(
    private val chapterCatalog: ChapterCatalog
) : ChapterRepository {
    private val rcRepoUrlTemplate = "https://content.bibletranslationtools.org/WA-Catalog/%s_%s/archive/master.zip"

    override fun getAll(languageCode: String, bookSlug: String): List<Chapter> {
        return chapterCatalog.getAll(languageCode, bookSlug)
    }

    override fun getChapterRC(
        rcFile: File,
        resourceId: String,
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int
    ): File {
        // get the rc from git repo
        val rcFile = getTemplateResourceContainer(languageCode, resourceId, DownloadClient())

        // pass into the download library

        return File("")
    }

    private fun getTemplateResourceContainer(
        languageCode: String,
        resourceId: String,
        downloadClient: IDownloadClient
    ): File? {
        val url = String.format(rcRepoUrlTemplate, languageCode, resourceId)
        // download rc from repo
        val downloadLocation = File("E:/miscs/rc") // path/on/file/system

        return downloadClient.downloadFile(url, downloadLocation)
    }
}