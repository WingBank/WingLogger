package com.wingmoney.winglogger.exception

import org.apache.commons.logging.LogFactory

class WingException : Throwable {
    private var strNewErrorCode = ""
    var errorMessage: String? = null
    var errorMessageKh: String? = null
    var errorMessageCh: String? = null
    var param: Map<String, String>? = null

    constructor(strException: String?) : super(strException) {
        //if (Config.PRINT_APP_EXCEPTIONS_IN_LOGS)
        log.error(strException, this)
    }

    constructor(strException: String, t: Throwable?) : super(t) {
        strNewErrorCode = strException
        // if (Config.PRINT_APP_EXCEPTIONS_IN_LOGS)
        log.error(strException, this)
    }

    constructor(errorCode: String?, errorMessage: String) : super(errorCode) {
        this.errorMessage = errorMessage
    }

    val errorCode: String?
        get() = if (strNewErrorCode == "") {
            super.message
        } else strNewErrorCode

    fun setErrorCode(strErrorCode: String) {
        log.info("Error message changed from: $errorCode > $strErrorCode")
        strNewErrorCode = strErrorCode
    }

    companion object {
        private val log = LogFactory.getLog(
            WingException::class.java
        )
    }
}