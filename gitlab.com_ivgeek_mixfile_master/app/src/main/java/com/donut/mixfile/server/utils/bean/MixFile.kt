package com.donut.mixfile.server.utils.bean


import com.donut.mixfile.server.Uploader
import com.donut.mixfile.server.accessKey
import com.donut.mixfile.server.uploadClient
import com.donut.mixfile.ui.routes.home.getLocalServerAddress
import com.donut.mixfile.ui.routes.home.serverAddress
import com.donut.mixfile.util.basen.BigIntBaseN
import com.donut.mixfile.util.compressGzip
import com.donut.mixfile.util.decompressGzip
import com.donut.mixfile.util.decryptAES
import com.donut.mixfile.util.encryptAES
import com.donut.mixfile.util.hashMD5
import com.donut.mixfile.util.parseFileMimeType
import com.donut.mixmessage.util.encode.basen.Alphabet
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import java.net.URLEncoder


data class MixShareInfo(
    @SerializedName("f") val fileName: String,
    @SerializedName("s") val fileSize: Long,
    @SerializedName("h") val headSize: Int,
    @SerializedName("u") val url: String,
    @SerializedName("k") val key: String,
    @SerializedName("r") val referer: String,
) {


    companion object {

        val ENCODER =
            BigIntBaseN(Alphabet.fromString("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"))

        fun fromString(string: String) = fromJson(dec(string))

        fun tryFromString(string: String) = try {
            fromString(string)
        } catch (e: Exception) {
            null
        }

        private fun fromJson(json: String): MixShareInfo =
            Gson().fromJson(json, MixShareInfo::class.java)

        private fun enc(input: String): String {
            val bytes = input.encodeToByteArray()
            val result = encryptAES(bytes, "123".hashMD5(), iv = "123".hashMD5().copyOf(12))
            return ENCODER.encode(result)
        }

        private fun dec(input: String): String {
            val bytes = ENCODER.decode(input)
            val result = decryptAES(bytes, "123".hashMD5())
            return result!!.decodeToString()
        }
    }

    val downloadUrl: String
        get() {
            return "${getLocalServerAddress()}/api/download?s=${
                URLEncoder.encode(
                    this.toString(),
                    "UTF-8"
                )
            }&accessKey=${accessKey}"
        }

    val lanUrl: String
        get() {
            return "$serverAddress/api/download?s=${
                URLEncoder.encode(
                    this.toString(),
                    "UTF-8"
                )
            }"
        }

    override fun toString(): String {
        return enc(toJson())
    }

    private fun toJson(): String = Gson().toJson(this)

    suspend fun fetchFile(url: String, referer: String = this.referer): ByteArray? {
        val transformedUrl = Uploader.transformUrl(url)
        val transformedReferer = Uploader.transformReferer(url, referer)
        val result: ByteArray? = uploadClient.prepareGet(transformedUrl) {
            if (transformedReferer.trim().isNotEmpty()) {
                header("Referer", transformedReferer)
            }
        }.execute {
            val channel = it.bodyAsChannel()
            channel.discard(headSize.toLong())
            decryptAES(channel, ENCODER.decode(key))
        }
        return result
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is MixShareInfo) {
            return url == other.url
        }
        return false
    }

    fun contentType(): String = fileName.parseFileMimeType()

    suspend fun fetchMixFile(): MixFile? {
        val decryptedBytes = fetchFile(url) ?: return null
        return MixFile.fromBytes(decryptedBytes)
    }

}


data class MixFile(
    @SerializedName("chunk_size") val chunkSize: Long,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("file_list") val fileList: List<String>,
) {

    companion object {
        fun fromBytes(data: ByteArray) = Gson().fromJson(decompressGzip(data), MixFile::class.java)
    }

    fun getFileListByStartRange(startRange: Long): List<Pair<String, Int>> {
        val start = startRange / chunkSize
        val rangeFileList = fileList.subList(start.toInt(), fileList.size)
        val startOffset = startRange % chunkSize
        val result = mutableListOf<Pair<String, Int>>()
        for (i in rangeFileList.indices) {
            val file = rangeFileList[i]
            val offset = if (i == 0) startOffset else 0
            result.add(Pair(file, offset.toInt()))
        }
        return result
    }


    fun toBytes() = compressGzip(Gson().toJson(this))

}