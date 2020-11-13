package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.data.RCDeliverable
import org.bibletranslationtools.fetcher.impl.repository.createRCFileName
import org.bibletranslationtools.fetcher.impl.repository.verifyChapterExists
import org.bibletranslationtools.fetcher.impl.repository.zipDirectory
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
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
    fun getResourceContainer(
        deliverable: Deliverable
    ): RCDeliverable? {
        val templateRC = rcRepository.getRC(
            deliverable.language.code,
            deliverable.resourceId
        ) ?: return null

        // allocate rc to delivery location
        val rcFile = allocateRcFileLocation(templateRC, deliverable)
        val rcWithMedia = downloadMediaInRC(rcFile, deliverable)

        return if (
            verifyChapterExists(
                rcWithMedia,
                deliverable.book.slug,
                mediaTypes,
                deliverable.chapter?.number
            )
        ) {
            val zipFile = rcWithMedia.parentFile.resolve("${rcFile.name}.zip")
                .apply { createNewFile() }
            zipDirectory(rcFile, zipFile)
            RCDeliverable(deliverable, zipFile.path)
        } else {
            rcFile.deleteRecursively()
            null
        }
    }

    private fun allocateRcFileLocation(rcFile: File, deliverable: Deliverable): File {
        val rcName = createRCFileName(
            deliverable,
            extension = rcFile.extension
        )
        return storageAccess.allocateRCFileLocation(rcFile, rcName)
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
