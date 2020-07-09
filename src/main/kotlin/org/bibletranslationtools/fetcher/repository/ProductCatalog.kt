package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Product

interface ProductCatalog {
    fun getAll(): List<Product>
}
