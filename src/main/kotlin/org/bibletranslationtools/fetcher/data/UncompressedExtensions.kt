package org.bibletranslationtools.fetcher.data

enum class UncompressedExtensions(ext: String) {
    WAV("wav");

    companion object : SupportedExtensions {
        override fun isSupported(extension: String): Boolean = values().any { it.name == extension.toUpperCase() }
    }
}