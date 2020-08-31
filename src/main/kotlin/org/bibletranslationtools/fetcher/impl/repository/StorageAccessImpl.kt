package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.io.FileFilter
import org.bibletranslationtools.fetcher.data.CompressedExtensions
import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.slf4j.LoggerFactory

class StorageAccessImpl(private val directoryProvider: DirectoryProvider) : StorageAccess {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getContentRoot(): File {
        return directoryProvider.getContentRoot()
    }

    override fun getLanguageCodes(): List<String> {
        val sourceFileRootDir = directoryProvider.getContentRoot()
        val dirs = sourceFileRootDir.listFiles(File::isDirectory)

        return if (dirs.isNullOrEmpty()) listOf() else dirs.map { it.name }
    }

    override fun getBookFile(request: FileAccessRequest): File? {
        val bookPrefixDir = getPathPrefixDir(
            languageCode = request.languageCode,
            resourceId = request.resourceId,
            bookSlug = request.bookSlug,
            fileExtension = request.fileExtension
        )

        val grouping = "book"
        val bookContentDir = getContentDir(
            prefixDir = bookPrefixDir,
            fileExtension = request.fileExtension,
            mediaExtension = request.mediaExtension,
            mediaQuality = request.mediaQuality,
            grouping = grouping
        )

        return try {
            bookContentDir.listFiles(File::isFile)?.single()
        } catch (e: NoSuchElementException) {
            // no content
            null
        } catch (e: IllegalArgumentException) {
            // there are more than 1 file under the dir
            logger.error("Max files allowed: 1. Too many files found at $bookContentDir", e)
            null
        }
    }

    override fun getChapterFile(request: FileAccessRequest): File? {
        val chapterPrefixDir = getPathPrefixDir(
            languageCode = request.languageCode,
            resourceId = request.resourceId,
            bookSlug = request.bookSlug,
            fileExtension = request.fileExtension,
            chapter = request.chapter
        )

        val grouping = "chapter"
        val chapterContentDir = getContentDir(
            prefixDir = chapterPrefixDir,
            fileExtension = request.fileExtension,
            mediaExtension = request.mediaExtension,
            mediaQuality = request.mediaQuality,
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

    override fun hasBookContent(
        languageCode: String,
        resourceId: String,
        bookSlug: String,
        fileExtensionList: List<String>
    ): Boolean {
        // look for book files
        return hasBookFile(languageCode, resourceId, bookSlug, fileExtensionList) ||
                hasChapterFile(languageCode, resourceId, bookSlug, fileExtensionList)
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

    private fun hasBookFile(
        languageCode: String,
        resourceId: String,
        bookSlug: String,
        fileExtensionList: List<String>
    ): Boolean {
        for (ext in fileExtensionList) {
            val bookPrefixDir = getPathPrefixDir(
                languageCode = languageCode,
                resourceId = resourceId,
                bookSlug = bookSlug,
                fileExtension = ext
            )
            val walkBookDir = bookPrefixDir.walk()
            val hasBook = walkBookDir.any() {
                it.parentFile.name == "book" && it.extension == ext
            }
            if (hasBook) return true
        }
        return false
    }

    private fun hasChapterFile(
        languageCode: String,
        resourceId: String,
        bookSlug: String,
        fileExtensionList: List<String>
    ): Boolean {
        val sourceContentRootDir = directoryProvider.getContentRoot()
        val bookDir = sourceContentRootDir.resolve("$languageCode/$resourceId/$bookSlug")
        val chapterDirList = bookDir.listFiles(
            FileFilter {
                it.name.matches(Regex("[0-9]{1,3}")) && it.isDirectory
            }
        )
        if (chapterDirList != null) {
            for (chapterDir in chapterDirList) {
                fileExtensionList.forEach { ext ->
                    val walkChapterDir = bookDir.resolve("$chapterDir/CONTENTS/$ext").walk()
                    val hasContent = walkChapterDir.any {
                        it.parentFile.name == "chapter" && it.extension == ext
                    }
                    if (hasContent) return true
                }
            }
        }
        return false
    }
}
