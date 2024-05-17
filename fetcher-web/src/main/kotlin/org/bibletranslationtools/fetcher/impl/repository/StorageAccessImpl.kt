package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.io.FileFilter
import java.util.UUID
import kotlin.NoSuchElementException
import org.bibletranslationtools.fetcher.data.CompressedExtensions
import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.data.Division
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.bibletranslationtools.fetcher.usecase.resourceIdByLanguage
import org.slf4j.LoggerFactory

class StorageAccessImpl(private val directoryProvider: DirectoryProvider) : StorageAccess {

    companion object {
        fun getPathPrefixDir(
            root: File,
            languageCode: String,
            resourceId: String,
            fileExtension: String? = null,
            bookSlug: String? = null,
            chapter: String? = null
        ): File {
            val trimmedChapter = chapter?.trimStart('0')

            val languagePart = languageCode
            val resourcePart = "/$resourceId"
            val bookPart = if (!bookSlug.isNullOrEmpty()) "/$bookSlug" else ""
            val chapterPart = if (!trimmedChapter.isNullOrEmpty()) "/$trimmedChapter" else ""
            val extensionPart = if (!fileExtension.isNullOrEmpty()) "/CONTENTS/$fileExtension" else ""

            return root.resolve(
                "$languagePart$resourcePart$bookPart$chapterPart$extensionPart"
            )
        }

        fun getContentDir(
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

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getContentRoot(): File {
        return directoryProvider.getContentRoot()
    }

    override fun getReposRoot(): File {
        return directoryProvider.getRCRepositoriesDir()
    }

    override fun hasLanguageContent(languageCode: String): Boolean {
        val sourceFileRootDir = directoryProvider.getContentRoot()
        val dirs = sourceFileRootDir.listFiles(File::isDirectory)

        return dirs?.any { it.name == languageCode } ?: false
    }

    override fun hasProductContent(languageCode: String, fileExtensions: List<String>): Boolean {
        val resourceId = resourceIdByLanguage(languageCode)
        val booksDir = getPathPrefixDir(
            directoryProvider.getContentRoot(),
            languageCode,
            resourceId
        )

        return booksDir.listFiles(File::isDirectory)?.any { bookDir ->
            val bookSlug = bookDir.name
            fileExtensions.any { ext ->
                val dir = getPathPrefixDir(
                    directoryProvider.getContentRoot(),
                    languageCode,
                    resourceId,
                    ext,
                    bookSlug
                )
                dir.exists()
            }
        } ?: false
    }

    override fun getBookFile(request: FileAccessRequest): File? {
        val bookPrefixDir = getPathPrefixDir(
            directoryProvider.getContentRoot(),
            languageCode = request.languageCode,
            resourceId = request.resourceId,
            bookSlug = request.bookSlug,
            fileExtension = request.fileExtension
        )

        val grouping = getGrouping(request.fileExtension, Division.BOOK)
        val bookContentDir = getContentDir(
            prefixDir = bookPrefixDir,
            fileExtension = request.fileExtension,
            mediaExtension = request.mediaExtension,
            mediaQuality = request.mediaQuality,
            grouping = grouping
        )

        return try {
            bookContentDir.listFiles(
                FileFilter { it.isFile && !it.isHidden }
            )?.single()
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
            directoryProvider.getContentRoot(),
            languageCode = request.languageCode,
            resourceId = request.resourceId,
            bookSlug = request.bookSlug,
            fileExtension = request.fileExtension,
            chapter = request.chapter
        )

        val grouping = getGrouping(request.fileExtension, Division.CHAPTER)
        val chapterContentDir = getContentDir(
            prefixDir = chapterPrefixDir,
            fileExtension = request.fileExtension,
            mediaExtension = request.mediaExtension,
            mediaQuality = request.mediaQuality,
            grouping = grouping
        )

        return try {
            chapterContentDir.listFiles(
                FileFilter { it.isFile && !it.name.startsWith(".") } // non-hidden files
            )?.single()
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
        // look for book files first, then chapters
        return hasBookFile(languageCode, resourceId, bookSlug, fileExtensionList) ||
                hasChapterFile(languageCode, resourceId, bookSlug, fileExtensionList)
    }

    override fun allocateRCFileLocation(newFileName: String): File {
        return directoryProvider.getRCExportDir()
            .resolve(UUID.randomUUID().toString())
            .apply { mkdirs() }
            .resolve(newFileName)
    }

    override fun getRepoFromFileSystem(name: String): File? {
        val repo = directoryProvider.getRCRepositoriesDir().resolve(name)
        return if (repo.exists()) repo else null
    }

    private fun hasBookFile(
        languageCode: String,
        resourceId: String,
        bookSlug: String,
        fileExtensionList: List<String>
    ): Boolean {
        for (ext in fileExtensionList) {
            val bookPrefixDir = getPathPrefixDir(
                directoryProvider.getContentRoot(),
                languageCode = languageCode,
                resourceId = resourceId,
                bookSlug = bookSlug,
                fileExtension = ext
            )
            val walkBookDir = bookPrefixDir.walk()
            val grouping = getGrouping(ext, Division.BOOK)

            val hasContent = walkBookDir.any() {
                it.parentFile.name == grouping && it.extension == ext
            }
            if (hasContent) return true
        }
        return false
    }

    @Suppress("NestedBlockDepth")
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

        if (chapterDirList != null && chapterDirList.any()) {
            for (chapterDir in chapterDirList) {
                fileExtensionList.forEach { ext ->
                    val walkChapterDir = bookDir.resolve("${chapterDir.name}/CONTENTS/$ext").walk()
                    val grouping = getGrouping(ext, Division.CHAPTER)

                    val hasContent = walkChapterDir.any {
                        it.parentFile.name == grouping && it.extension == ext
                    }
                    if (hasContent) return true
                }
            }
        }
        return false
    }

    private fun getGrouping(ext: String, division: Division): String {
        return when {
            ext == ProductFileExtension.BTTR.fileType -> "verse"
            else -> division.name.toLowerCase()
        }
    }
}
