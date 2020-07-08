package org.bibletranslationtools.fetcher.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.FileType
import org.slf4j.LoggerFactory

class FileTypeRepository : FileTypeCatalog {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fileTypesResourceName = "audio-file-types.json"

    override fun getFileTypes(): List<FileType> {
        val jsonFileTypes: String = try {
            val resourceFile = getResourceFile()
            resourceFile.readText()
        } catch (e: FileNotFoundException) {
            logger.error("Resource File Not Found")
            return listOf()
        }

        val mapper = jacksonObjectMapper()
        return mapper.readValue(jsonFileTypes, jacksonTypeRef<List<FileType>>())
    }

    @Throws(FileNotFoundException::class)
    private fun getResourceFile(): File {
        val resourceFileURL = javaClass.classLoader.getResource(fileTypesResourceName)
            ?: throw FileNotFoundException()

        return File(resourceFileURL.path)
    }
}
