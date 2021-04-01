package org.bibletranslationtools.fetcher.io

import java.io.File
import org.bibletranslationtools.fetcher.config.CDN_BASE_URL
import org.bibletranslationtools.fetcher.config.CONTENT_ROOT_DIR
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

/**
 * This client provides better performance
 * for file transfer without network communication.
 * However, it must be locally hosted in the same system
 * and have direct access to the resources.
 *
 * Pass an instance of this class to RC Media Downloader
 */
class LocalFileTransferClient : IDownloadClient {
    /**
     * This method automatically converts the requested url
     * into a local path on the system, then copies it to outputDir
     */
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
