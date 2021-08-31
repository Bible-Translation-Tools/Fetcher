package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.data.RCDeliverable
import org.bibletranslationtools.fetcher.impl.repository.RCUtils
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.wycliffeassociates.rcmediadownloader.RCMediaDownloader
import org.wycliffeassociates.rcmediadownloader.data.MediaDivision
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import org.wycliffeassociates.rcmediadownloader.data.MediaUrlParameter
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import org.wycliffeassociates.resourcecontainer.ResourceContainer
import org.wycliffeassociates.resourcecontainer.entity.Media

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
        val rc = prepareRC(deliverable) ?: return null
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

    private fun prepareRC(deliverable: Deliverable): File? {
        val templateRC = rcRepository.getRC(
            deliverable.language.code,
            deliverable.resourceId
        )
        if (templateRC == null || !templateRC.exists()) {
            return null
        }

        // allocate rc to delivery location
        val rcName = RCUtils.createRCFileName(deliverable, "")
        val rcFile = storageAccess.allocateRCFileLocation(rcName)
        templateRC.copyRecursively(rcFile)

        ResourceContainer.load(rcFile).use { rc ->
            val mediaProject = rc.media?.projects?.firstOrNull {
                it.identifier == deliverable.book.slug
            }

            val mediaList = mutableListOf<Media>()
            for (mediaType in mediaTypes) {
                val mediaIdentifier = mediaType.name.toLowerCase()
                val chapterUrl = buildChapterMediaPath(
                    deliverable,
                    mediaIdentifier,
                    mediaQualityMap[mediaIdentifier]!!)
                val newMediaEntry = Media(
                    mediaIdentifier,
                    "",
                    "",
                    listOf(),
                    chapterUrl.invariantSeparatorsPath
                )
                mediaList.add(newMediaEntry)
            }
            mediaProject?.media = mediaList
            rc.writeMedia()
        }

        return rcFile
    }

    private fun buildChapterMediaPath(
        deliverable: Deliverable,
        extension: String,
        quality: String
    ): File {
        val root = File(baseRCUrl)
        val prefixPath = StorageAccessImpl.getPathPrefixDir(
            root,
            deliverable.language.code,
            deliverable.resourceId,
            extension,
            deliverable.book.slug,
            "{chapter}"
        )

        val chapterPath = StorageAccessImpl.getContentDir(
            prefixPath,
            extension,
            extension,
            quality,
            "chapter"
        )

        val fileName = "${deliverable.language.code}_${deliverable.resourceId}" +
                "_${deliverable.book.slug}_c{chapter}.$extension"

        return chapterPath.resolve(fileName)
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

        private val mediaQualityMap = mapOf(
            "mp3" to "hi",
            "wav" to ""
        )
    }
}
