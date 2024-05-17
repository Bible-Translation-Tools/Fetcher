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
import org.wycliffeassociates.resourcecontainer.entity.MediaManifest
import org.wycliffeassociates.resourcecontainer.entity.MediaProject
import java.util.zip.Adler32

class RequestResourceContainer(
    envConfig: EnvironmentConfig,
    private val rcRepository: ResourceContainerRepository,
    private val storageAccess: StorageAccess,
    private val downloadClient: IDownloadClient
) {
    private val baseRCUrl = envConfig.CDN_BASE_RC_URL
    private val baseContentUrl = envConfig.CONTENT_ROOT_DIR
    private val outputDir = envConfig.RC_OUTPUT_DIR

    fun getResourceContainer(
        deliverable: Deliverable
    ): RCDeliverable? {
        val rcName = RCUtils.createRCFileName(deliverable, "")
        val zipFile = File("$outputDir/$rcName.orature")

        val chapterFiles = getChapterFiles(deliverable)
        val hasUpdatedFiles = checkHasUpdatedFiles(chapterFiles)

        if (hasUpdatedFiles || !zipFile.exists()) {
            val rc = prepareRC(deliverable, rcName) ?: return null
            val tempZipFile = rc.parentFile.resolve(zipFile.name)
            downloadMediaInRC(rc, deliverable)

            val hasContent = RCUtils.verifyChapterExists(
                rc,
                deliverable.book.slug,
                mediaTypes,
                deliverable.chapter?.number
            )
            val packedUp = RCUtils.zipDirectory(rc, tempZipFile)
            if (packedUp) tempZipFile.copyTo(zipFile, true)

            rc.parentFile.deleteRecursively()

            return if (hasContent && packedUp) {
                // If a chapter has been changed and rebuilt, remove corresponding book file
                // The book file will be rebuilt on the next book request
                deliverable.chapter?.number?.let {
                    val bookName = rcName.replace("_c$it", "")
                    val bookFile = File("$outputDir/$bookName.orature")
                    if (hasUpdatedFiles && bookFile.exists()) {
                        bookFile.delete()
                    }
                }

                val url =  "$baseRCUrl/${zipFile.name}"
                RCDeliverable(deliverable, url)
            } else null
        } else {
            val url = "$baseRCUrl/${zipFile.name}"
            return RCDeliverable(deliverable, url)
        }
    }

    private fun getChapterFiles(deliverable: Deliverable): List<File> {
        val templateRC = getTemplateRC(deliverable) ?: return listOf()

        val manifest = ResourceContainer.load(templateRC).use { rc ->
            buildMedia(rc, deliverable, baseContentUrl)
        }

        val project = manifest.projects
            .singleOrNull {
                it.identifier == deliverable.book.slug
            } ?: return listOf()

        val chapterFilesList = mutableListOf<File>()
        for (media in project.media) {
            val possibleChapterRange = 200
            val templateUrl = media.chapterUrl

            for (chapterNumber in 1..possibleChapterRange) {
                if (deliverable.chapter?.number == chapterNumber || deliverable.chapter?.number == null) {
                    val chapterUrl = templateUrl.replace("{chapter}", chapterNumber.toString())
                    val chapterFile = File(chapterUrl)
                    if (chapterFile.exists()) {
                        chapterFilesList.add(chapterFile)
                    }
                }
            }
        }

        return chapterFilesList
    }

    private fun checkHasUpdatedFiles(files: List<File>): Boolean {
        var hasUpdated = false

        for (file in files) {
            val hashFile = file.parentFile.resolve(".hash")
            val crc = Adler32().apply {
                update(file.readBytes())
            }

            val oldHash = if (hashFile.exists()) {
                hashFile.readLines().first().toLong()
            } else 0
            val newHash = crc.value

            if (newHash != oldHash) {
                hasUpdated = true
                hashFile.writeText(newHash.toString())
            }
        }

        return hasUpdated
    }

    private fun prepareRC(deliverable: Deliverable, rcName: String): File? {
        val rcFile = storageAccess.allocateRCFileLocation(rcName)
        val templateRC = getTemplateRC(deliverable) ?: return null

        // allocate rc to delivery location
        templateRC.copyRecursively(rcFile)
        templateRC.walk().filter {
            it.isDirectory && it.name.startsWith(".git")
        }.forEach { it.deleteRecursively() }

        overwriteRCMediaManifest(rcFile, deliverable)

        return rcFile
    }

    private fun getTemplateRC(deliverable: Deliverable): File? {
        val templateRC = rcRepository.getRC(
            deliverable.language.code,
            deliverable.resourceId
        ) ?: rcRepository.downloadRC(
            deliverable.language.code,
            deliverable.resourceId
        )

        return if (templateRC != null && templateRC.exists()) {
            templateRC
        } else null
    }

    private fun overwriteRCMediaManifest(rcFile: File, deliverable: Deliverable) {
        ResourceContainer.load(rcFile).use { rc ->
            rc.media = buildMedia(rc, deliverable, baseRCUrl)
            rc.writeMedia()
        }
    }

    private fun buildMedia(rc: ResourceContainer, deliverable: Deliverable, baseUrl: String): MediaManifest {
        var manifest = rc.media

        if (manifest == null) {
            manifest = MediaManifest().apply {
                projects = listOf(MediaProject(identifier = deliverable.book.slug))
            }
        }

        var mediaProject = manifest.projects.firstOrNull {
            it.identifier == deliverable.book.slug
        }

        val mediaList = mutableListOf<Media>()
        for (mediaType in mediaTypes) {
            val mediaIdentifier = mediaType.toString()
            val chapterUrl = buildChapterMediaUrl(
                deliverable,
                mediaIdentifier,
                mediaQualityMap[mediaIdentifier]!!,
                baseUrl
            )
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

        return manifest
    }

    private fun buildChapterMediaUrl(
        deliverable: Deliverable,
        extension: String,
        quality: String,
        baseUrl: String
    ): File {
        val root = File(baseUrl)
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

    companion object {
        val mediaTypes = listOf(MediaType.MP3, MediaType.CUE)

        private val mediaQualityMap = mapOf(
            MediaType.MP3.toString() to ProductFileQuality.HI.quality,
            MediaType.WAV.toString() to "",
            MediaType.CUE.toString() to ""
        )
    }
}
