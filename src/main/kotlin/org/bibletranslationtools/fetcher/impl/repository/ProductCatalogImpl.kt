package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.slf4j.LoggerFactory
import java.io.InputStream

class ProductCatalogImpl : ProductCatalog {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val productCatalogFileName = "/product_catalog.json"
    private val products: List<Product> = parseCatalog()

    override fun getAll(): List<Product> = this.products

    @Throws(FileNotFoundException::class)
    private fun parseCatalog(): List<Product> {
        val jsonProducts: InputStream = try {
            getProductCatalogFile()
        } catch (e: FileNotFoundException) {
            logger.error("$productCatalogFileName not found in resources.", e)
            throw e // crash on fatal exception: critical resource not found
        }

        return jacksonObjectMapper().readValue(jsonProducts)
    }

    @Throws(FileNotFoundException::class)
    private fun getProductCatalogFile(): InputStream {
        val catalogFileStream = javaClass.getResourceAsStream(productCatalogFileName)
        if(catalogFileStream == null) {
            throw FileNotFoundException()
        }

        return catalogFileStream
    }
}
