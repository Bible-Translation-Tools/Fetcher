package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.data.RCDeliverable
import org.bibletranslationtools.fetcher.impl.repository.RCUtils
import org.bibletranslationtools.fetcher.repository.*
import java.io.File
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
    private val mediaTypes = listOf(MediaType.WAV, MediaType.MP3)

    fun getResourceContainer(
        deliverable: Deliverable
    ): RCDeliverable? {
        val templateRC = rcRepository.getRC(
            deliverable.language.code,
            deliverable.resourceId
        ) ?: return null

        // make new copy to serve out
        val rcFile = allocateRcFileLocation(templateRC, deliverable)

        val rcWithMedia = downloadMediaInRC(rcFile, deliverable)

        return if  (
            RCUtils.verifyChapterExists(
                rcWithMedia,
                deliverable.book.slug,
                mediaTypes,
                deliverable.chapter?.number
            )
        ) {
            RCDeliverable(deliverable, rcWithMedia.path)
        } else null
    }

    private fun allocateRcFileLocation(rcFile: File, deliverable: Deliverable): File {
        val newFileName = RCUtils.createRCFileName(
            deliverable,
            extension = rcFile.extension
        )
        return storageAccess.allocateRCFileLocation(rcFile, newFileName)
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

    private fun contentDownloadUrl(rcFile: File): String {
        // TODO: replace path with file server url for download
        return rcFile.path
    }
}
