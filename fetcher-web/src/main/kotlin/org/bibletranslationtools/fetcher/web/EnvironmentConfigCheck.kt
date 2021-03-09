package org.bibletranslationtools.fetcher.web

import org.bibletranslationtools.fetcher.impl.repository.DirectoryProviderImpl
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.slf4j.LoggerFactory
import java.lang.AssertionError
import java.lang.NullPointerException

/**
 * This instance ensures that the required runtime configurations are set up.
 * The run() method must be invoked at entry point.
* */
object EnvironmentConfigCheck {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val directoryProvider: DirectoryProvider = DirectoryProviderImpl()

    fun run() {
        val passed = checkContentRoot()
                && checkRCExportDir()
                && checkRCRepoDir()

        if (!passed) {
            throw AssertionError(
                "Some environment checks have failed. " +
                        "Please review the missing/invalid configurations."
            )
        }
    }


    private fun checkContentRoot(): Boolean {
        return try {
            val contentRoot = directoryProvider.getContentRoot()
            contentRoot.exists()
        } catch (ex: NullPointerException) {
            logger.error(
                "Cannot get content root directory. Is the path variable configured properly?",
                ex
            )
            false
        }
    }
    
    private fun checkRCExportDir(): Boolean {
        return try {
            val rcDir = directoryProvider.getRCExportDir()
            rcDir.exists()
        } catch (ex: NullPointerException) {
            logger.error(
                "Cannot get RC export directory. Is the path variable configured properly?",
                ex
            )
            false
        }
    }

    private fun checkRCRepoDir(): Boolean {
        return try {
            val rcDir = directoryProvider.getRCRepositoriesDir()
            rcDir.exists()
        } catch (ex: NullPointerException) {
            logger.error(
                "Cannot get RC repos directory. Is the path variable configured properly?",
                ex
            )
            false
        }
    }
}
