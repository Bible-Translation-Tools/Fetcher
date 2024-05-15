package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.ProductViewData

class FetchProductViewData(
    productCatalog: ProductCatalog,
    private val storage: StorageAccess,
    private val languageCode: String
) {
    private val products: List<Product> = productCatalog.getAll()

    fun getListViewData(
        currentPath: String
    ): List<ProductViewData> {
        return products.map {
            val productExtension = ProductFileExtension.getType(it.slug)!!
            val fileExtensions = if (ContainerExtensions.isSupported(productExtension.fileType)) {
                    listOf("tr")
                } else {
                    listOf("mp3", "wav")
                }

            val isAvailable = storage.hasProductContent(languageCode, fileExtensions)

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
