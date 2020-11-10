package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import junit.framework.Assert.assertNotNull
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.InputStream

class ProductFileExtensionTest {
    private val productCatalogFileName = "/product_catalog.json"
    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testProductsHaveFileExtension() {
        val products = parseCatalog()
        products.forEach {
            assertNotNull(
                "${it.slug} does not have matching file extension",
                ProductFileExtension.getType(it.slug)
            )
        }
    }

    @Throws(FileNotFoundException::class)
    private fun parseCatalog(): List<Product> {
        val jsonProducts: InputStream = try {
            getProductCatalogFile()
        } catch (e: FileNotFoundException) {
            logger.error("$productCatalogFileName not found in resources.", e)
            throw e
        }

        return jacksonObjectMapper().readValue(jsonProducts)
    }

    @Throws(FileNotFoundException::class)
    private fun getProductCatalogFile(): InputStream {
        val catalogFileStream = javaClass.getResourceAsStream(productCatalogFileName)
        if (catalogFileStream == null) {
            throw FileNotFoundException()
        }

        return catalogFileStream
    }
}
