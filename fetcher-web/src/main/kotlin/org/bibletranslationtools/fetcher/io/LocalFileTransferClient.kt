package org.bibletranslationtools.fetcher.io

import java.io.File
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

/**
 * This client provides better performance
 * for file transfer without network communication.
 * However, it must be locally hosted in the same system
 * and have direct access to the resources.
 *
 * Pass an instance of this class to RC Media Downloader
 */
class LocalFileTransferClient(
    private val envVars: EnvironmentConfig
) : IDownloadClient {

//    private val urlPrefix = System.getenv("CDN_BASE_URL")
//    private val pathPrefix = System.getenv("CONTENT_ROOT")
    /**
     * This method automatically converts the requested url
     * into a local path on the system, then copies it to outputDir
     */
    override fun downloadFromUrl(url: String, outputDir: File): File? {
        val relativePath = File(url).relativeTo(File(envVars.CDN_BASE_URL))

        // map to local path
        val sourceFile = File(envVars.CONTENT_ROOT_DIR).resolve(relativePath)

        return if (sourceFile.exists()) {
            val targetFile = outputDir.resolve(File(url).name)
            sourceFile.copyTo(targetFile, true)
            targetFile
        } else {
            null
        }
    }
}
