package org.bibletranslationtools.fetcher.repository.implementations

import java.io.File
import org.bibletranslationtools.fetcher.repository.DirectoryProvider

class DirectoryProviderImpl : DirectoryProvider {
    override fun getSourceFileRootDir(): File {
        return File("/")
    }
}
