package org.bibletranslationtools.fetcher.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.FileType
import org.slf4j.LoggerFactory

class FileTypeRepository: FileTypeCatalog {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fileTypesResourceName = "audio-file-types.json"

    override fun getFileTypes(): List<FileType> {
        val mapper = jacksonObjectMapper()
        val pathToFileType = javaClass.classLoader.getResource(fileTypesResourceName).path
        val jsonData: String = try {
            File(pathToFileType).readText()
        } catch (e: FileNotFoundException) {
            logger.error("Resource File Not Found")
            return listOf()
        }

        return mapper.readValue(jsonData, jacksonTypeRef<List<FileType>>())
    }
}
