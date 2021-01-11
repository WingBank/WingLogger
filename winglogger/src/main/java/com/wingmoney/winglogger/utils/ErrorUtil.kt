package com.wingmoney.winglogger.utils

import com.google.gson.Gson
import com.wingmoney.winglogger.exception.WingException
import com.wingmoney.winglogger.model.BaseResponse

class ErrorUtil {
    private val gson = Gson()
    var messageFromCache: BaseResponse? = null
    var messageFromCacheMap: Map<String, String> = HashMap()

    fun getErrorMessageNoLog(ex: Throwable?): String {
        var message = BaseResponse()
        if (ex is WingException) {
            val ae = ex
            if (ae.errorCode == null) ae.setErrorCode(ErrorCode.GENERAL_FAIL)
            message.errorCode = ae.errorCode
            message = getMessageFromCache(message)
            if (ae.errorMessage != null && ae.errorMessage!!.isNotEmpty()) {
                message.message = ae.errorMessage
            }
            if (ae.errorMessageKh != null && ae.errorMessageKh!!.isNotEmpty()) {
                message.messageKh = ae.errorMessageKh
            }
            if (ae.errorMessageCh != null && ae.errorMessageCh!!.isNotEmpty()) {
                message.messageCh = ae.errorMessageCh
            }
            if (message.message == null) {
                message.message =
                    message.errorCode + " : (Error message not yet updated in our system)"
            }
            // Todo: Update message if has param
            replaceParamValue(ae, message)
            return gson.toJson(message)
        }
        message.errorCode = ErrorCode.GENERAL_FAIL
        message = getMessageFromCache(message)
        return gson.toJson(message)
    }

    private fun replaceParamValue(ex: WingException, message: BaseResponse) {
        if (ex.param != null) {
            var messageEn = message.message
            var messageKh = message.messageKh
            var messageCh = message.messageCh
            val entries = ex.param!!.entries
            for ((key, value) in entries) {
                if (messageEn != null) messageEn = messageEn.replace(key, value)
                if (messageKh != null) messageKh = messageKh.replace(key, value)
                if (messageCh != null) messageCh = messageCh.replace(key, value)
            }
            message.message = messageEn
            message.messageKh = messageKh
            message.messageCh = messageCh
        }
    }

    private fun getMessageFromCache(message: BaseResponse): BaseResponse {
        val cacheMessageMap: Map<String, String> = messageFromCacheMap
        if (cacheMessageMap.containsKey("message_en")) message.message =
            cacheMessageMap["message_en"]
        if (cacheMessageMap.containsKey("message_kh")) message.messageKh =
            cacheMessageMap["message_kh"] else message.messageKh = cacheMessageMap["message_en"]
        if (cacheMessageMap.containsKey("message_ch")) message.messageCh =
            cacheMessageMap["message_ch"] else message.messageCh = cacheMessageMap["message_en"]
        return message
    }
}