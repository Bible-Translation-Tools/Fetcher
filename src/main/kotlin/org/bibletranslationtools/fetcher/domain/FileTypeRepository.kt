package org.bibletranslationtools.fetcher.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.FileType

class FileTypeRepository {
    private val fileTypesResourceName = "audio-file-types.json"

    fun getFileTypes(): List<FileType> {
        val mapper = jacksonObjectMapper()
        val pathToFileType = javaClass.classLoader.getResource(fileTypesResourceName).path
        val jsonData: String = try {
            File(pathToFileType).readText()
        } catch (e: FileNotFoundException) {
            println(e.message)
            return listOf()
        }

        return mapper.readValue(jsonData, jacksonTypeRef<List<FileType>>())
    }
}
