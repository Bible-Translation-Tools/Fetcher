package org.bibletranslationtools.fetcher.usecase

import java.lang.IllegalArgumentException

enum class ProductFileExtension(val fileType: String) {
    MP3("mp3"),
    WAV("wav"),
    BTTR("tr"),
    ORATURE("zip");

    companion object {
        fun getType(productSlug: String): ProductFileExtension? {
            return try {
                valueOf(productSlug.uppercase())
            } catch (ex: IllegalArgumentException) {
                null
            }
        }
    }
}
