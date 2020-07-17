package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.usecase.viewdata.ProductViewData

class FetchProductViewData(productRepo: ProductCatalog) {
    private val products: List<Product> = productRepo.getAll()

    fun getListViewData(currentPath: String): List<ProductViewData> = products.map {
        ProductViewData(
            slug = it.slug,
            titleKey = it.titleKey,
            descriptionKey = it.descriptionKey,
            iconUrl = it.iconUrl,
            url = "$currentPath/${it.slug}"
        )
    }
}