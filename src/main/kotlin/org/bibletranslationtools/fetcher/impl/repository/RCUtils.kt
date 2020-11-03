package org.bibletranslationtools.fetcher.impl.repository

import java.io.File
import java.util.zip.ZipFile
import org.bibletranslationtools.fetcher.data.Deliverable
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.resourcecontainer.ResourceContainer

fun createRCFileName(
    deliverable: Deliverable,
    extension: String
): String {
    return if (deliverable.chapter == null) {
        "${deliverable.language.code}_${deliverable.resourceId}_${deliverable.book.slug}.$extension" // book rc
    } else {
        "${deliverable.language.code}_${deliverable.resourceId}_${deliverable.book.slug}" +
                "_c${deliverable.chapter.number}.$extension" // chapter rc
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

            ZipFile(rcFile).use { rcZip ->
                val listEntries = rcZip.entries().toList()
                exists = listEntries.any { entry ->
                    entry.name.matches(Regex(".*/$chapterPath\$"))
                }
            }
        }
        if (exists) return true
    }

    return exists
}
