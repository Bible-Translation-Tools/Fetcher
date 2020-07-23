package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.data.CompressedExtensions
import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.slf4j.LoggerFactory

class StorageAccessImpl(private val directoryProvider: DirectoryProvider) : StorageAccess {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getLanguageCodes(): List<String> {
        val sourceFileRootDir = directoryProvider.getContentRoot()
        val dirs = sourceFileRootDir.listFiles(File::isDirectory)

        return if (dirs.isNullOrEmpty()) listOf() else dirs.map { it.name }
    }

    override fun getBookSlugs(languageCode: String, resourceId: String): List<String> {
        val projectsDir = directoryProvider.getProjectsDir(languageCode, resourceId)
        val dirs = projectsDir.listFiles(File::isDirectory)

        return if (dirs.isNullOrEmpty()) listOf() else dirs.map { it.name }
    }

    override fun getChapterFile(model: FileAccessRequest): File? {
        val chapterPrefixDir = getPathPrefixDir(
            languageCode = model.languageCode,
            resourceId = model.resourceId,
            bookSlug = model.bookSlug,
            fileExtension = model.fileExtension,
            chapter = model.chapter
        )

        val grouping = "chapter"
        val chapterContentDir = getContentDir(
            prefixDir = chapterPrefixDir,
            fileExtension = model.fileExtension,
            mediaExtension = model.mediaExtension,
            mediaQuality = model.mediaQuality,
            grouping = grouping
        )

        return try {
            chapterContentDir.listFiles(File::isFile)?.single()
        } catch (e: NoSuchElementException) {
            // no content
            null
        } catch (e: IllegalArgumentException) {
            // there are more than 1 file under the dir
            logger.error("Max files allowed: 1. Too many files found at $chapterContentDir", e)
            null
        }
    }

    private fun getPathPrefixDir(
        languageCode: String,
        resourceId: String,
        fileExtension: String,
        bookSlug: String = "",
        chapter: String = ""
    ): File {
        val trimmedChapter = chapter.trimStart('0')
        val sourceContentRootDir = directoryProvider.getContentRoot()

        return when {
            bookSlug.isNotEmpty() && trimmedChapter.isNotEmpty() ->
                sourceContentRootDir.resolve(
                    "$languageCode/$resourceId/$bookSlug/$trimmedChapter/CONTENTS/$fileExtension"
                )
            bookSlug.isNotEmpty() -> sourceContentRootDir.resolve(
                "$languageCode/$resourceId/$bookSlug/CONTENTS/$fileExtension"
            )
            else -> sourceContentRootDir.resolve(
                "$languageCode/$resourceId/CONTENTS/$fileExtension"
            )
        }
    }

    private fun getContentDir(
        prefixDir: File,
        fileExtension: String,
        mediaExtension: String,
        mediaQuality: String,
        grouping: String
    ): File {
        val isContainer = ContainerExtensions.isSupported(fileExtension)
        val isContainerAndCompressed = isContainer && CompressedExtensions.isSupported(mediaExtension)
        val isFileAndCompressed = !isContainer && CompressedExtensions.isSupported(fileExtension)

        return prefixDir.resolve(
            when {
                isContainerAndCompressed -> "$mediaExtension/$mediaQuality/$grouping"
                isContainer -> "$mediaExtension/$grouping"
                isFileAndCompressed -> "$mediaQuality/$grouping"
                else -> grouping
            }
        )
    }
}
