package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.StorageAccess

class StorageAccessImpl(private val directoryProvider: DirectoryProvider) : StorageAccess {
    override fun getLanguageCodes(): List<String> {
        val sourceFileRootDir = directoryProvider.getContentRoot()
        val dirs = sourceFileRootDir.listFiles(File::isDirectory)

        if (dirs.isNullOrEmpty()) return listOf()
        return dirs.map { it.name.toString() }.toList()
    }

    override fun getChapter(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        fileExtension: String,
        mediaExtension: String,
        mediaQuality: String
    ): Chapter {
        val isContainer = mediaExtension.isNotEmpty()
        val isCompressed = mediaQuality.isNotEmpty()

        val pathPrefix = getChapterPathPrefix(languageCode, bookSlug, chapterNumber, fileExtension)
        val fullPath = when {
            isContainer && isCompressed -> "$pathPrefix/$mediaExtension/$mediaQuality/chapter"
            isContainer -> "$pathPrefix/$mediaExtension/chapter"
            !isContainer && isCompressed -> "$pathPrefix/$mediaQuality/chapter"
            else -> "$pathPrefix/chapter"
        }

        val sourceContentRoot = directoryProvider.getContentRoot()
        val chapterPath = File(sourceContentRoot, fullPath)
        val chapterFiles = chapterPath.listFiles()

        return if (chapterFiles.isNullOrEmpty() || chapterFiles.size > 1) Chapter(chapterNumber, null)
        else Chapter(chapterNumber, chapterFiles[0])
    }

    private fun getChapterPathPrefix(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        fileExtension: String
    ): String {
        return "$languageCode/ulb/$bookSlug/$chapterNumber/CONTENTS/$fileExtension"
    }
}
