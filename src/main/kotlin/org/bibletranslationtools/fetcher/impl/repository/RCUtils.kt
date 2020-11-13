package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.bibletranslationtools.fetcher.data.Deliverable
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.resourcecontainer.ResourceContainer

fun createRCFileName(
    deliverable: Deliverable,
    extension: String
): String {
    val suffix = if (extension.isEmpty()) "" else ".$extension"
    return if (deliverable.chapter == null) {
        "${deliverable.language.code}_${deliverable.resourceId}_${deliverable.book.slug}$suffix" // book rc
    } else {
        "${deliverable.language.code}_${deliverable.resourceId}_${deliverable.book.slug}" +
                "_c${deliverable.chapter.number}$suffix" // chapter rc
    }
}

fun verifyChapterExists(
    rcFile: File,
    bookSlug: String,
    mediaTypes: List<MediaType>,
    chapterNumber: Int?
): Boolean {
    var exists = false
    val chapterNumberPattern = chapterNumber?.toString() ?: "[0-9]{1,3}"

    ResourceContainer.load(rcFile).use { rc ->
        val mediaProject = rc.media?.projects?.firstOrNull {
            it.identifier == bookSlug
        }

        for (mediaType in mediaTypes) {
            val media = mediaProject?.media?.firstOrNull {
                it.identifier == mediaType.name.toLowerCase()
            }
            val pathInRC = media?.chapterUrl ?: continue
            val chapterPath = pathInRC.replace("{chapter}", chapterNumberPattern)

            when (rcFile.extension) {
                "zip" -> {
                    ZipFile(rcFile).use { rcZip ->
                        val listEntries = rcZip.entries().toList()
                        exists = listEntries.any { entry ->
                            entry.name.matches(Regex(".*/$chapterPath\$"))
                        }
                    }
                }
                else -> {
                    exists = rcFile.walk().any {
                        it.invariantSeparatorsPath.matches(Regex(".*/$chapterPath\$"))
                    }
                }
            }
        }
    }

    return exists
}

fun zipDirectory(sourcePath: File, destFile: File): Boolean {
    val rootName = sourcePath.name
    var success = true

    try {
        ZipOutputStream(FileOutputStream(destFile).buffered()).use { zos ->
            sourcePath.walkTopDown().forEach { fileInSource ->
                val zipFileName = fileInSource.absolutePath
                    .removePrefix(sourcePath.absolutePath).removePrefix("\\")
                val suffix = if (fileInSource.isDirectory) "\\" else ""

                val entry = ZipEntry("$rootName\\$zipFileName$suffix")
                zos.putNextEntry(entry)

                if (fileInSource.isFile) fileInSource.inputStream().copyTo(zos)
            }
        }
    } catch (ex: FileNotFoundException) {
        success = false
    } catch (ex: IllegalArgumentException) {
        success = false
    } catch (ex: ZipException) {
        success = false
    } catch (ex: IOException) {
        success = false
    }

    return success
}
