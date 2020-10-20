package org.bibletranslationtools.fetcher.io

import java.io.File

interface IDownloadClient {
    fun downloadFile(url: String, outputDir: File): File?
}
