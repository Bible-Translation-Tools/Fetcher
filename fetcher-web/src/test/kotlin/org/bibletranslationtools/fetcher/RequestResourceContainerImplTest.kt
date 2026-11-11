package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.impl.repository.RequestResourceContainerImpl
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import kotlin.io.path.createTempDirectory

class RequestResourceContainerImplTest {
    private val baseRCUrl = "https://cdn.example.test/rc"

    private lateinit var tempDir: File
    private lateinit var outputDir: File
    private lateinit var templateRC: File
    private lateinit var envConfig: EnvironmentConfig
    private lateinit var rcRepository: ResourceContainerRepository
    private lateinit var storageAccess: StorageAccess

    private val deliverable = Deliverable(
        resourceId = "ulb",
        language = Language("en", "English", "English", true),
        book = Book(56, "tit", "Titus"),
        product = Product("orature", "", "", ""),
        chapter = Chapter(1)
    )
    private val expectedZipName = "en_ulb_tit_c1.orature"

    @Before
    fun setUp() {
        tempDir = createTempDirectory("rcRequestTest").toFile()
        outputDir = tempDir.resolve("output").apply { mkdirs() }
        val contentRoot = tempDir.resolve("content").apply { mkdirs() }
        val exportDir = tempDir.resolve("export").apply { mkdirs() }

        templateRC = tempDir.resolve("template")
        getTestResource("titus_test").copyRecursively(templateRC)

        envConfig = mock(EnvironmentConfig::class.java)
        `when`(envConfig.CDN_BASE_RC_URL).thenReturn(baseRCUrl)
        `when`(envConfig.CONTENT_ROOT_DIR).thenReturn(contentRoot.path)
        `when`(envConfig.RC_OUTPUT_DIR).thenReturn(outputDir.path)

        rcRepository = mock(ResourceContainerRepository::class.java)
        `when`(rcRepository.getRC(anyString(), anyString())).thenReturn(templateRC)

        storageAccess = mock(StorageAccess::class.java)
        `when`(storageAccess.allocateRCFileLocation(anyString())).thenAnswer { invocation ->
            exportDir.resolve(UUID.randomUUID().toString())
                .apply { mkdirs() }
                .resolve(invocation.getArgument<String>(0))
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun missingChapterReturnsNullAndLeavesNoArchive() {
        val requestRC = RequestResourceContainerImpl(
            envConfig,
            rcRepository,
            storageAccess,
            missingMediaClient
        )
        val zipFile = outputDir.resolve(expectedZipName)

        val result = requestRC.getResourceContainer(deliverable)

        assertNull(result)
        assertFalse("Stale archive must not be cached", zipFile.exists())

        // A subsequent request must not resolve to a cached archive URL.
        val secondResult = requestRC.getResourceContainer(deliverable)
        assertNull(secondResult)
        assertFalse(zipFile.exists())
    }

    @Test
    fun successfulBuildCreatesAndReturnsArchive() {
        val requestRC = RequestResourceContainerImpl(
            envConfig,
            rcRepository,
            storageAccess,
            successfulMediaClient
        )
        val zipFile = outputDir.resolve(expectedZipName)

        val result = requestRC.getResourceContainer(deliverable)

        assertNotNull(result)
        assertEquals("$baseRCUrl/$expectedZipName", result!!.url)
        assertTrue("Archive should be created", zipFile.exists())
        assertTrue(zipFile.length() > 0)

        // Cache hit returns the same verified archive.
        val cached = requestRC.getResourceContainer(deliverable)
        assertNotNull(cached)
        assertEquals(result.url, cached!!.url)
    }

    /** Simulates media that does not exist on the server: nothing is downloaded. */
    private val missingMediaClient = object : IDownloadClient {
        override fun downloadFromUrl(url: String, outputDir: File): File? = null
    }

    /** Simulates a successful download by writing a small file named after the url. */
    private val successfulMediaClient = object : IDownloadClient {
        override fun downloadFromUrl(url: String, outputDir: File): File? {
            val file = outputDir.resolve(url.substringAfterLast("/"))
            file.writeText("media")
            return file
        }
    }

    @Throws(FileNotFoundException::class)
    private fun getTestResource(name: String): File {
        val path = javaClass.classLoader.getResource(name)
            ?: throw FileNotFoundException("Test resource not found: $name")
        return File(path.file)
    }
}
