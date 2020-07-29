package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.slf4j.LoggerFactory

class ProductCatalogImpl : ProductCatalog {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val productCatalogFileName = System.getenv("PRODUCT_CATALOG_FILE")
    private val products: List<Product> = parseCatalog()

    override fun getAll(): List<Product> = this.products

    @Throws(FileNotFoundException::class)
    private fun parseCatalog(): List<Product> {
        val jsonProducts: String = try {
            val productsFile = getProductCatalogFile()
            productsFile.readText()
        } catch (e: FileNotFoundException) {
            logger.error("Product Catalog file not found at $productCatalogFileName", e)
            throw e // crash on fatal exception: critical resource not found
        }

        return jacksonObjectMapper().readValue(jsonProducts)
    }

    @Throws(FileNotFoundException::class)
    private fun getProductCatalogFile(): File {
        val catalogFile = File(productCatalogFileName)
        if(!catalogFile.exists()) {
            throw FileNotFoundException("Product Catalog file not found at $productCatalogFileName")
        }

        return catalogFile
    }
}
