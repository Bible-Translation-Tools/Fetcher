package org.bibletranslationtools.fetcher.data

enum class CompressedExtensions(vararg val ext: String) {
    MP3("mp3"),
    JPG("jpeg", "jpg");

    companion object : SupportedExtensions {
        override fun isSupported(extension: String): Boolean {
            return values().any {
                it.name == extension.toUpperCase() || it.ext.contains(extension)
            }
        }
    }
}