package org.bibletranslationtools.fetcher.io

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import okhttp3.ResponseBody
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class DownloadClient : IDownloadClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun downloadFromUrl(url: String, destFile: File): File? {
        val fileFromUrl = File(url)
        val retrofitService = Retrofit.Builder()
            .baseUrl(fileFromUrl.parentFile.invariantSeparatorsPath + "/")
            .build()
        val client: RetrofitDownloadClient = retrofitService.create(
            RetrofitDownloadClient::class.java
        )

        val call = client.downloadFile(fileFromUrl.name)
        val response = call.execute()

        if (response.isSuccessful) {
            val body = response.body()
            if (body == null) {
                logger.error("No response body found. ${response.message()}")
            } else {
                writeTempDownload(body, destFile)
            }
        }

        return if (destFile.isFile) destFile else null
    }

    private fun writeTempDownload(body: ResponseBody, outputFile: File): File {
        BufferedInputStream(body.byteStream()).use { inputStream ->
            val bytes = inputStream.readBytes()

            FileOutputStream(outputFile).buffered().use { outputStream ->
                outputStream.write(bytes)
            }
        }
        return outputFile
    }
}

private interface RetrofitDownloadClient { // implemented by retrofit service
    @GET
    fun downloadFile(@Url url: String): Call<ResponseBody>
}
