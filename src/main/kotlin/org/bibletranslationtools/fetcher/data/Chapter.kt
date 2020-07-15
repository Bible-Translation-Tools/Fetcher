package org.bibletranslationtools.fetcher.data

import java.io.File

data class Chapter(
    val number: Int,
    val file: File?
) {
    fun hasContent(): Boolean = file != null
}
