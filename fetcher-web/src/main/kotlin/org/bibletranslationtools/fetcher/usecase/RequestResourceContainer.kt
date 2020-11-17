package org.bibletranslationtools.fetcher.usecase

import java.io.File
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.data.RCDeliverable
import org.bibletranslationtools.fetcher.impl.repository.RCUtils
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.slf4j.LoggerFactory
import org.wycliffeassociates.rcmediadownloader.RCMediaDownloader
import org.wycliffeassociates.rcmediadownloader.data.MediaDivision
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.data.MediaUrlParameter
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

class RequestResourceContainer(
    private val rcRepository: ResourceContainerRepository,
    private val storageAccess: StorageAccess,
    private val downloadClient: IDownloadClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getResourceContainer(
        deliverable: Deliverable
    ): RCDeliverable? {
        val templateRC = rcRepository.getRC(
            deliverable.language.code,
            deliverable.resourceId
        ) ?: return null

        // allocate rc to delivery location
        val zipName = RCUtils.createRCFileName(deliverable, "zip")
        val zipFile = storageAccess.allocateRCFileLocation(zipName)

        val packedUp = try {
            ZipFile(zipFile).addFolder(templateRC)
            true
        } catch (ex: ZipException) {
            logger.error("error zipping folder $templateRC", ex)
            false
        }

        downloadMediaInRC(zipFile, deliverable)

        val hasContent = RCUtils.verifyChapterExists(
            zipFile,
            deliverable.book.slug,
            mediaTypes,
            deliverable.chapter?.number
        )

        return if (hasContent && packedUp) {
            RCDeliverable(deliverable, zipFile.path)
        } else {
            zipFile.deleteRecursively()
            null
        }
    }

    private fun downloadMediaInRC(rcFile: File, deliverable: Deliverable): File {
        val downloadParameters = MediaUrlParameter(
            projectId = deliverable.book.slug,
            mediaDivision = MediaDivision.CHAPTER,
            mediaTypes = mediaTypes,
            chapter = deliverable.chapter?.number
        )
        return RCMediaDownloader.download(
            rcFile,
            downloadParameters,
            downloadClient,
            overwrite = true
        )
    }

    companion object {
        val mediaTypes = listOf(MediaType.WAV, MediaType.MP3)
    }
}
