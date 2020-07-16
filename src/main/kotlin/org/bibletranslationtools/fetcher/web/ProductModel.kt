package org.bibletranslationtools.fetcher.web

import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ProductCatalog

class ProductModel(private val productRepo: ProductCatalog) {
    val viewData: List<Product> = productRepo.getAll()
}