package org.bibletranslationtools.fetcher.data

import java.io.File
import java.nio.file.Files


data class ChapterContent(
    val number: Int,
    val file: File?
) {
    val mimeType: String = if(file == null) "" else Files.probeContentType(file.toPath())

    fun hasContent(): Boolean = file != null
}
