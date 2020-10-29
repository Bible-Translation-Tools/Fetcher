package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.util.zip.ZipFile
import org.bibletranslationtools.fetcher.web.controllers.utils.MediaResourceParameters
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.resourcecontainer.ResourceContainer

object RCUtils {
    fun createRCFileName(
        params: MediaResourceParameters,
        extension: String,
        chapter: Int?
    ): String {
        return if (chapter == null) {
            "${params.languageCode}_${params.resourceId}_${params.bookSlug}.$extension" // book rc
        } else {
            "${params.languageCode}_${params.resourceId}_${params.bookSlug}_c$chapter.$extension" // chapter rc
        }
    }

    fun verifyChapterExists(
        rcFile: File,
        bookSlug: String,
        mediaTypes: List<MediaType>,
        chapterNumber: Int?
    ): Boolean {
        var isExisting = false
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

                ZipFile(rcFile).use { rcZip ->
                    val listEntries = rcZip.entries().toList()
                    isExisting = listEntries.any { entry ->
                        entry.name.matches(Regex(".*/$chapterPath\$"))
                    }
                }
            }
            if (isExisting) return true
        }

        return isExisting
    }
}
