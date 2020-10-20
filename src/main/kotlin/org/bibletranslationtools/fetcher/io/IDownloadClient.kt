package org.bibletranslationtools.fetcher.io

import java.io.File

interface IDownloadClient {
    fun downloadFromUrl(url: String, outputDir: File): File?
}
