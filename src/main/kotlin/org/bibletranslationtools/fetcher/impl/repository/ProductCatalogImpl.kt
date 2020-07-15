package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ProductCatalog

class ProductCatalogImpl : ProductCatalog {
    private val productCatalogFileName = "product_catalog.json"
    private val products: List<Product> = parseCatalog()

    override fun getAll(): List<Product> = this.products

    private fun parseCatalog(): List<Product> {
        val jsonProducts: String = try {
            val productsFile = getProductCatalogFile()
            productsFile.readText()
        } catch (e: FileNotFoundException) {
            return listOf()
        }

        return jacksonObjectMapper().readValue(jsonProducts)
    }

    @Throws(FileNotFoundException::class)
    private fun getProductCatalogFile(): File {
        val resourceFileURL = javaClass.classLoader.getResource(productCatalogFileName)
            ?: throw FileNotFoundException()

        return File(resourceFileURL.path)
    }
}
