package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.data.RCDeliverable
import java.io.File

interface RequestResourceContainer {
    fun getResourceContainer(deliverable: Deliverable): RCDeliverable?
    fun getResourceContainer(languageCode: String, resourceId: String): File?
}