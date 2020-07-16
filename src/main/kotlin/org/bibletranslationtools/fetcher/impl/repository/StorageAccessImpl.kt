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
        val chapterRootDir = getPathPrefixDir(
            languageCode,
            bookSlug,
            fileExtension,
            chapterNumber
        )

        val isContainer = ContainerExtensions.isSupported(fileExtension)
        val isContainerAndCompressed = isContainer && CompressedExtensions.isSupported(mediaExtension)
        val isFileAndCompressed = !isContainer && CompressedExtensions.isSupported(fileExtension)

        val chapterFileDir = chapterRootDir.resolve(
            when {
                isContainerAndCompressed -> "$mediaExtension/$mediaQuality/chapter"
                isContainer -> "$mediaExtension/chapter"
                isFileAndCompressed -> "$mediaQuality/chapter"
                else -> "chapter"
            }
        )

        val chapterDirContents = chapterFileDir.listFiles()

        if(chapterDirContents.isNullOrEmpty()) return null
        return chapterDirContents.first()
    }

    private fun getPathPrefixDir(
        languageCode: String,
        bookSlug: String,
        fileExtension: String,
        chapter: Int? = null
    ): File {
        val sourceContentRootDir = directoryProvider.getContentRoot()

        return if (chapter == null) {
            sourceContentRootDir.resolve(
                "$languageCode/ulb/$bookSlug/CONTENTS/$fileExtension"
            )
        } else {
            sourceContentRootDir.resolve(
                "$languageCode/ulb/$bookSlug/$chapter/CONTENTS/$fileExtension"
            )
        }
    }
}
