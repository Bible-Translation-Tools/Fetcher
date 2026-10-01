package org.bibletranslationtools.fetcher.data

enum class CompressedExtensions(vararg val ext: String) {
    MP3("mp3"),
    JPG("jpeg", "jpg");

    companion object : SupportedExtensions {
        override fun isSupported(extension: String): Boolean {
            return entries.any {
                it.name == extension.uppercase() || it.ext.contains(extension)
            }
        }
    }
}
