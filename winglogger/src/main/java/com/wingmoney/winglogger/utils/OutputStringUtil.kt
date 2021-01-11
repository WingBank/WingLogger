package com.wingmoney.winglogger.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.wingmoney.winglogger.model.LogParam
import java.util.*

object OutputStringUtil {
    private const val tagFrame = "===================="
    private const val titleFrame = "--------------------"
    private const val space = " "
    private fun getTextWithFrame(text: String, frame: String): String {
        return frame + space + text + space + frame
    }

    // Use without tag
    fun getPrettyLineText(oldString: String): String {
        return """
             
             
             $oldString
             
             """.trimIndent()
    }

    // Use with tag
    fun getPrettyLineText(tag: String, oldString: String): String {
        return """
             
             
             $tagFrame$space$tag$space$tagFrame:
             $oldString
             
             """.trimIndent()
    }

    fun getPrettyHeader(headerText: String): String {
        return """
               
               
               ${getTextWithFrame(headerText, tagFrame)}
               
               """.trimIndent()
    }

    fun getPrettyHeader(header: String, param: LinkedHashMap<String, String>?): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(getPrettyHeader(header))
        getParamContent(param, stringBuilder)
        return stringBuilder.toString()
    }

    private fun getParamContent(
        param: LinkedHashMap<String, String>?,
        stringBuilder: StringBuilder
    ) {
        if (param != null && param.size > 0) {
            val entrySet: Set<*> = param.entries
            val iterator: Iterator<Map.Entry<*, *>> =
                entrySet.iterator() as Iterator<Map.Entry<*, *>>
            var currentIndex = 0
            val compareMaxLength = Comparator.comparingInt { obj: String -> obj.length }
            val longKey = Collections.max(param.keys, compareMaxLength)
            val maxKeyLength = longKey.length
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value != null) {
                    val key = StringBuilder(entry.key.toString())
                    val keyLength = key.length
                    if (keyLength < maxKeyLength) {
                        for (i in 0 until maxKeyLength - keyLength) {
                            key.append(space)
                        }
                    }
                    if (key.toString().isEmpty()) {
                        stringBuilder.append(entry.value.toString())
                    } else {
                        stringBuilder.append("# ").append(key).append(" : ")
                            .append(entry.value.toString())
                    }
                    if (currentIndex < param.size) {
                        stringBuilder.append("\r\n")
                    }
                }
                currentIndex++
            }
        }
    }

    fun getPrettyHeader(
        header: String,
        param: LinkedHashMap<String, String>?,
        lstLogParam: List<LogParam>?
    ): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(getPrettyHeader(header))
        var i = 1
        if (lstLogParam != null && lstLogParam.size > 0) {
            for (logParam in lstLogParam) {
                stringBuilder.append("(").append(i).append("): ").append(logParam.header)
                    .append("\n")
                getParamContent(logParam.logParam, stringBuilder)
                stringBuilder.append("\n")
                i++
            }
        }
        stringBuilder.append("(").append(i).append("): ").append("Base Information from Controller")
            .append("\n")
        getParamContent(param, stringBuilder)
        return stringBuilder.toString()
    }

    fun getPrettyLog(header: String, title: String, content: String?): String {
        val headerLength = header.length
        val titleLength = title.length
        val result: String
        val stringBuilder = StringBuilder()
        stringBuilder.append("\r\n")
        stringBuilder.append(getTextWithFrame(header, tagFrame))
        stringBuilder.append("\r\n")
        val additionalFrameSize = (headerLength - titleLength) / 2
        for (i in 0 until additionalFrameSize) {
            stringBuilder.append("-")
        }
        stringBuilder.append(getTextWithFrame(title, titleFrame))
        for (i in 0 until additionalFrameSize) {
            stringBuilder.append("-")
        }
        stringBuilder.append("\r\n")
        stringBuilder.append(content)
        stringBuilder.append("\r\n")
        result = stringBuilder.toString()
        return result
    }

    fun hidePasscode(request: Any?): JsonElement {
        val gson = Gson()
        var jsonElementReq = gson.toJsonTree(request)
        try {
            if (jsonElementReq.isJsonPrimitive) {
                jsonElementReq =
                    gson.fromJson(jsonElementReq.asJsonPrimitive.asString, JsonObject::class.java)
            }
            if (jsonElementReq.isJsonObject) {
                val jsonObject = jsonElementReq.asJsonObject
                val newDisplay = JsonPrimitive("****")
                if (jsonObject["passcode"] != null) {
                    jsonObject.add("passcode", newDisplay)
                }
                if (jsonObject["pin"] != null) {
                    jsonObject.add("pin", newDisplay)
                }
                if (jsonObject["sender_passcode"] != null) {
                    jsonObject.add("sender_passcode", newDisplay)
                } else if (jsonObject["customer_pin"] != null) {
                    jsonObject.add("customer_pin", newDisplay)
                }
                if (jsonObject["merchant_passcode"] != null) {
                    jsonObject.add("merchant_passcode", newDisplay)
                } else if (jsonObject["wcx_pin"] != null) {
                    jsonObject.add("wcx_pin", newDisplay)
                }
            }
        } catch (ignored: Exception) {
        }
        return jsonElementReq
    }
}