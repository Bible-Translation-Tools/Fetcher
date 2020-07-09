package org.bibletranslationtools.fetcher.repository.implementations

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ProductCatalog

class ProductCatalogImpl : ProductCatalog {
    private val productCatalogFileName = "product_catalog.json"

    private data class ProductSchema(
        val slug: String,
        val titleKey: String,
        val descriptionKey: String,
        val iconLink: String
    )

    private val products: List<Product> = parseCatalog()

    override fun getAll(): List<Product> = this.products

    private fun parseCatalog(): List<Product> {
        val jsonFileTypes: String = try {
            val productsFile = getProductCatalogFile()
            productsFile.readText()
        } catch (e: FileNotFoundException) {
            return listOf()
        }

        val productsFromSchema: List<ProductSchema> = jacksonObjectMapper().readValue(jsonFileTypes)
        return productsFromSchema.map {
            Product(
                it.slug,
                it.titleKey,
                it.descriptionKey,
                File(it.iconLink)
            )
        }
    }

    @Throws(FileNotFoundException::class)
    private fun getProductCatalogFile(): File {
        val resourceFileURL = javaClass.classLoader.getResource(productCatalogFileName)
            ?: throw FileNotFoundException()

        return File(resourceFileURL.path)
    }
}
