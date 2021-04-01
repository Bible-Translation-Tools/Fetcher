package org.bibletranslationtools.fetcher.io

import org.bibletranslationtools.fetcher.config.CDN_BASE_URL
import org.bibletranslationtools.fetcher.config.CONTENT_ROOT_DIR
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import java.io.File

class LocalFileTransferClient: IDownloadClient {
    override fun downloadFromUrl(url: String, outputDir: File): File? {
        val relativePath = File(url).relativeTo(File(CDN_BASE_URL))
        // map to local path
        val sourceFile = File(CONTENT_ROOT_DIR).resolve(relativePath)

        return if (sourceFile.exists()) {
            val targetFile = outputDir.resolve(File(url).name)
            sourceFile.copyTo(targetFile, true)
            sourceFile
        } else {
            null
        }
    }
}