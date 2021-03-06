package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.usecase.viewdata.ProductViewData

class FetchProductViewData(
    productCatalog: ProductCatalog,
    private val languageCode: String
) {
    private val products: List<Product> = productCatalog.getAll()

    fun getListViewData(
        currentPath: String,
        cacheAccessor: ContentCacheAccessor,
        isGateway: Boolean
    ): List<ProductViewData> {
        return products.map {
            val isAvailable = if (isGateway) {
                cacheAccessor.isProductAvailable(it.slug, languageCode)
            } else {
                it.slug == ProductFileExtension.MP3.name.toLowerCase()
            }

            ProductViewData(
                slug = it.slug,
                titleKey = it.titleKey,
                descriptionKey = it.descriptionKey,
                iconUrl = it.iconUrl,
                url = if (isAvailable) "$currentPath/${it.slug}" else null
            )
        }
    }
}
