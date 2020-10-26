package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.BookCatalog
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.wycliffeassociates.rcmediadownloader.RCMediaDownloader
import org.wycliffeassociates.rcmediadownloader.data.MediaDivision
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.data.MediaUrlParameter
import org.wycliffeassociates.rcmediadownloader.io.DownloadClient
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import org.wycliffeassociates.resourcecontainer.ResourceContainer
import java.io.File
import java.util.zip.ZipFile

class BookRepositoryImpl(
    private val bookCatalog: BookCatalog
) : BookRepository {
    private val englishLanguageCode = "en"
    private val rcRepoUrlTemplate = "https://content.bibletranslationtools.org/WA-Catalog/%s_%s/archive/master.zip"

    override fun getBooks(resourceId: String, languageCode: String): List<Book> {
        val books = bookCatalog.getAll()
        // set localized name here

        return books
    }

    override fun getBooks(resourceId: String, language: Language): List<Book> {
        return getBooks(resourceId, language.code)
    }

    override fun getBook(slug: String, languageCode: String): Book? {
        if (languageCode == englishLanguageCode) {
            return bookCatalog.getBook(slug)
        } else {
            return null // This will get the localized name
        }
    }

    override fun getBook(slug: String, language: Language): Book? {
        return getBook(slug, language.code)
    }

    override fun getBookRC(
        slug: String,
        languageCode: String,
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
            projectId = slug,
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
            anyChapterInRC(rcWithMedia, slug, mediaTypes)
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
        val downloadLocation = File(System.getenv("RC_TEMP"))

        return downloadClient.downloadFromUrl(url, downloadLocation)
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
