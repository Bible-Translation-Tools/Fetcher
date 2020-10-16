package org.bibletranslationtools.fetcher.usecase

import java.lang.IllegalArgumentException

enum class ProductFileExtension(val fileType: String) {
    MP3("mp3"),
    BTTR("tr"),
    ORATURE("zip"); // TODO: I don't think this filetype is accurate...

    companion object {
        fun getType(productSlug: String): ProductFileExtension? {
            return try {
                valueOf(productSlug.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                null
            }
        }
    }
}
