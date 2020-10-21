package org.bibletranslationtools.fetcher.io

import java.io.File

interface DownloadClientInterface {
    fun downloadFile(url: String, outputDir: File): File?
}
