package org.bibletranslationtools.fetcher.data

interface SupportedExtensions {
    fun isSupported(extension: String): Boolean
}
