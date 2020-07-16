package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.data.ChapterContent
import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.StorageAccess

class StorageAccessImpl(private val directoryProvider: DirectoryProvider) : StorageAccess {
    override fun getLanguageCodes(): List<String> {
        val sourceFileRootDir = directoryProvider.getContentRoot()
        val dirs = sourceFileRootDir.listFiles(File::isDirectory)

        return if (dirs.isNullOrEmpty()) listOf() else dirs.map { it.name }
    }

    override fun getBookSlugs(languageCode: String): List<String> {
        val projectsDir = directoryProvider.getProjectsDir(languageCode)
        val dirs = projectsDir.listFiles(File::isDirectory)

        return if (dirs.isNullOrEmpty()) listOf() else dirs.map { it.name }
    }

    override fun getChaptersContent(
        languageCode: String,
        bookSlug: String,
        totalChapters: Int,
        fileType: String
    ): List<ChapterContent> {
        val chapterList = mutableListOf<ChapterContent>()

        for (chapter in 1..totalChapters) {
            chapterList.add(
                getChapterContent(
                    languageCode,
                    bookSlug,
                    chapter.toString(),
                    fileType
                )
            )
        }

        return chapterList
    }

    private fun getChapterContent(
        languageCode: String,
        bookSlug: String,
        chapter: String,
        fileType: String
    ): ChapterContent {
        val pathPrefix = getPathPrefixDir(languageCode, bookSlug, chapter)
        val chapterDownloadFile = if (ContainerExtensions.isSupported(fileType)) {
            seekFile(pathPrefix.resolve(fileType), "chapter")
        } else {
            seekFile(pathPrefix, "chapter")
        }

        return ChapterContent(chapter.toInt(), chapterDownloadFile)
    }

    private fun getPathPrefixDir(
        languageCode: String,
        bookSlug: String,
        chapter: String = ""
    ): File {
        val sourceContentRootDir = directoryProvider.getContentRoot()

        return if (chapter.isBlank()) {
            sourceContentRootDir.resolve(
                "$languageCode/ulb/$bookSlug/CONTENTS"
            )
        } else {
            sourceContentRootDir.resolve(
                "$languageCode/ulb/$bookSlug/${chapter.toInt()}/CONTENTS"
            )
        }
    }

    private fun seekFile(
        pathPrefix: File,
        grouping: String
    ): File? {
        val paths = getAudioPathPriority()

        for (path in paths) {
            val contentDir = pathPrefix.resolve("$path/$grouping")
            val contentFiles = contentDir.listFiles()

            if (contentFiles.isNullOrEmpty()) continue
            return contentFiles.first()
        }
        return null
    }

    private fun getAudioPathPriority(): List<String> {
        return listOf(
            "mp3/hi",
            "mp3/low",
            "wav"
        )
    }
}
