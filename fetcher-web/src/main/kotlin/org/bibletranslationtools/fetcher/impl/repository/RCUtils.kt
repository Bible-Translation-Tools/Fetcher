package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import net.lingala.zip4j.ZipFile as Zip4J
import net.lingala.zip4j.model.ZipParameters
import org.bibletranslationtools.fetcher.data.Deliverable
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.resourcecontainer.ResourceContainer

object RCUtils {
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

    @Throws(IOException::class)
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
                    it.identifier == mediaType.toString()
                }
                val pathInRC = media?.chapterUrl ?: continue
                val chapterPath = pathInRC.replace("{chapter}", chapterNumberPattern)

                exists = when (rcFile.extension) {
                    "zip" -> checkFromZipFile(rcFile, chapterPath)
                    else -> checkFromDirectory(rcFile, chapterPath)
                }
            }
        }

        return exists
    }

    @Suppress("TooGenericExceptionCaught")
    fun zipDirectory(source: File, dest: File): Boolean {
        return try {
            val options = ZipParameters()
            options.isIncludeRootFolder = false
            Zip4J(dest).addFolder(source, options)
            true
        } catch (ex: Exception) {
            false
        }
    }

    private fun checkFromZipFile(rcFile: File, chapterPath: String): Boolean {
        ZipFile(rcFile).use { rcZip ->
            val listEntries = rcZip.entries().toList()
            return listEntries.any { entry ->
                entry.name.matches(Regex("^$chapterPath\$"))
            }
        }
    }

    private fun checkFromDirectory(rcFile: File, chapterPath: String): Boolean {
        return rcFile.walk().any {
            it.invariantSeparatorsPath.matches(Regex(".*$chapterPath\$"))
        }
    }
}
