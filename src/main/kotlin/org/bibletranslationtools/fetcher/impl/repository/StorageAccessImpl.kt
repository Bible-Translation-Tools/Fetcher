package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.data.Chapter
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

    override fun getChapterWithAudioFile(
        languageCode: String,
        bookSlug: String,
        chapter: String,
        fileType: String
    ): Chapter {
        val chapterDownloadFile = getChapterAudioFile(
            languageCode,
            bookSlug,
            chapter,
            fileType
        )

        return Chapter(chapter.toInt(), chapterDownloadFile)
    }

    private fun getChapterAudioFile(
        languageCode: String,
        bookSlug: String,
        chapter: String,
        fileType: String
    ): File? {
        val pathPrefix = getPathPrefixDir(languageCode, bookSlug, chapter)

        return if (ContainerExtensions.isSupported(fileType)) {
            seekAudioFile(pathPrefix.resolve(fileType), "chapter")
        } else {
            seekAudioFile(pathPrefix, "chapter")
        }
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

    private fun seekAudioFile(
        pathPrefix: File,
        grouping: String
    ): File? {
        val paths = listOf(
            "mp3/hi",
            "mp3/low",
            "wav"
        )

        for (path in paths) {
            val contentDir = pathPrefix.resolve("$path/$grouping")
            val contentFiles = contentDir.listFiles()

            if (contentFiles.isNullOrEmpty()) continue
            return contentFiles.first()
        }
        return null
    }
}
