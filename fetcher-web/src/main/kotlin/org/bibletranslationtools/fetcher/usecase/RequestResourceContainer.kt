package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.data.RCDeliverable
import org.bibletranslationtools.fetcher.impl.repository.RCUtils
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.wycliffeassociates.rcmediadownloader.RCMediaDownloader
import org.wycliffeassociates.rcmediadownloader.data.MediaDivision
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.data.MediaUrlParameter
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

class RequestResourceContainer(
    envConfig: EnvironmentConfig,
    private val rcRepository: ResourceContainerRepository,
    private val storageAccess: StorageAccess,
    private val downloadClient: IDownloadClient
) {
    private val baseRCUrl = envConfig.CDN_BASE_RC_URL

    fun getResourceContainer(
        deliverable: Deliverable
    ): RCDeliverable? {
        val templateRC = rcRepository.getRC(
            deliverable.language.code,
            deliverable.resourceId
        ) ?: return null

        // allocate rc to delivery location
        val rcName = RCUtils.createRCFileName(deliverable, "")
        val rc = storageAccess.allocateRCFileLocation(rcName)
        templateRC.copyRecursively(rc)

        downloadMediaInRC(rc, deliverable)

        val hasContent = RCUtils.verifyChapterExists(
            rc,
            deliverable.book.slug,
            mediaTypes,
            deliverable.chapter?.number
        )
        val zipFile = rc.parentFile.resolve(rc.name + ".orature")
        val packedUp = RCUtils.zipDirectory(rc, zipFile)

        return if (hasContent && packedUp) {
            rc.deleteRecursively()
            val url = formatDownloadUrl(zipFile)
            RCDeliverable(deliverable, url)
        } else {
            zipFile.parentFile.deleteRecursively()
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
            singleProject = true,
            overwrite = true
        )
    }

    private fun formatDownloadUrl(file: File): String {
        val relativePath = file.parentFile.name + File.separator + file.name
        return "$baseRCUrl/$relativePath"
    }

    companion object {
        val mediaTypes = listOf(MediaType.MP3)
    }
}
