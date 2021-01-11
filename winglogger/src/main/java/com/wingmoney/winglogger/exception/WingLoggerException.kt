package com.wingmoney.winglogger.exception

class WingLoggerException(throwable: Throwable?) : Throwable(throwable) {
    var actualException: Throwable? = null
}