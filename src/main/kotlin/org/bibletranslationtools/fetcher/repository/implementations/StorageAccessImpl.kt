package org.bibletranslationtools.fetcher.repository.implementations

import java.io.File
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.StorageAccess

class StorageAccessImpl(private val directoryProvider: DirectoryProvider) : StorageAccess {
    override fun getLanguageCodes(): List<String> {
        val sourceFileRootDir = directoryProvider.getContentRoot()
        val dirs = sourceFileRootDir.listFiles(File::isDirectory)

        if (dirs.isNullOrEmpty()) return listOf()
        return dirs.map { it.name.toString() }.toList()
    }
}
