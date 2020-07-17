package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.data.CompressedExtensions
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

    override fun getChapterNumbers(languageCode: String, bookSlug: String): List<String> {
        val chaptersDir = directoryProvider.getChaptersDir(languageCode, bookSlug)
        val dirs = chaptersDir.listFiles(File::isDirectory)

        return if (dirs.isNullOrEmpty()) listOf() else dirs.map { it.name }
    }

    override fun getChapterFile(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        fileExtension: String,
        mediaExtension: String,
        mediaQuality: String
    ): File? {
        val chapterPrefixDir = getPathPrefixDir(
            languageCode,
            bookSlug,
            fileExtension,
            chapterNumber.toString()
        )

        val grouping = "chapter"
        val chapterContentDir = getContentDir(
            chapterPrefixDir,
            fileExtension,
            mediaExtension,
            mediaQuality,
            grouping
        )

        return chapterContentDir.listFiles(File::isFile)?.firstOrNull() ?: null
    }

    private fun getPathPrefixDir(
        languageCode: String,
        fileExtension: String,
        bookSlug: String = "",
        chapter: String = ""
    ): File {
        val sourceContentRootDir = directoryProvider.getContentRoot()
        return when {
            bookSlug.isNotEmpty() && chapter.isNotEmpty() ->
                sourceContentRootDir.resolve(
                    "$languageCode/ulb/$bookSlug/$chapter/CONTENTS/$fileExtension"
                )
            bookSlug.isNotEmpty() -> sourceContentRootDir.resolve(
                "$languageCode/ulb/$bookSlug/CONTENTS/$fileExtension"
            )
            else -> sourceContentRootDir.resolve(
                "$languageCode/ulb/CONTENTS/$fileExtension"
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
