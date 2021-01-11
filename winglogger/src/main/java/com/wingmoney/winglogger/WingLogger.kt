package com.wingmoney.winglogger

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.wingmoney.winglogger.exception.WingException
import com.wingmoney.winglogger.exception.WingLoggerException
import com.wingmoney.winglogger.model.LogParam
import com.wingmoney.winglogger.utils.ErrorUtil
import com.wingmoney.winglogger.utils.OutputStringUtil.getPrettyHeader
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.io.*
import java.util.*
import javax.servlet.http.HttpServletRequest

class WingLogger {
    var wingLogger: WingLogger? = null
    var log: Log? = null
    var isWriteRequest = true
    var logHeader = "New Request Connection"
    var request: String? = null
        private set
    private var updatedRequest: String? = null
    private var response: Any? = null
    var logParam: LinkedHashMap<String, String>? = null
        private set
    private var lstLogParam: MutableList<LogParam>? = null
    private var exception: WingLoggerException? = null
    private var contextObject: Any? = null
    protected var objectMapper = ObjectMapper()
    protected var gson = Gson()
    private val jsonParser = JsonParser()
    var isLogPrinted = false
    private val isDefaultInit: Boolean
    private var wingAccount: String? = null
    private var masterAccount: String? = null
    var optionsResult: String? = null
        private set
    private var abstractOptions: Any? = null
    var xmlToCore: String? = null
    var coreResponse: String? = null
    private val REMOTE_ADDR_KEY = "Remote Address"
    private val CONTEXT_PATH_KEY = "Context Path"
    private val ENDPOINT_KEY = "Endpoint"
    private val SERVLET_PATH_KEY = "Servlet Path"
    private val METHOD_KEY = "Method"
    private val REMOTE_USER_KEY = "Remote User"
    private val REQUEST_BODY_KEY = "Json Request"
    private val JSON_RESP_KEY = "Json Response"
    private val MASTER_ACCOUNT_KEY = "Master Account"
    private val WING_ACCOUNT_KEY = "Wing Account"
    private val ERROR_RESP_KEY = "Error Response"
    private val ERROR_STACKTRACE_KEY = "Error StackTrace"
    private val XML_TO_CORE_KEY = "XML to Core"
    private val CORE_RESP_KEY = "Core Response"
    private val DEFAULT_ERROR_CODE = "000030"
    private val HIDDEN_JSON_REQ = arrayOf(
        "passcode",
        "pin",
        "strPin",
        "sender_passcode",
        "customer_pin",
        "merchant_passcode",
        "wcx_pin",
        "otp",
        "password"
    )
    private val HIDDEN_JSON_RESP = arrayOf(
        "balance",
        "customerBalance",
        "passcode",
        "withdrawal_code",
        "payerSms",
        "payerSMS"
    )
    private val HIDDEN_XML_REQ = arrayOf(
        "PIN",
        "NEWPIN",
        "WCXPIN"
    )
    private val HIDDEN_XML_RESP = arrayOf(
        "BALANCE",
        "PASSCODE",
        "PAYERSMS"
    )

    protected constructor() {
        isDefaultInit = true
        init(this, null, true)
    }

    constructor(contextObject: Any) {
        isDefaultInit = false
        init(contextObject, null, true)
    }

    constructor(httpServletRequest: HttpServletRequest?) {
        isDefaultInit = false
        init(this, httpServletRequest, true)
    }

    constructor(contextObject: Any, httpServletRequest: HttpServletRequest?) {
        isDefaultInit = false
        init(contextObject, httpServletRequest, true)
    }

    constructor(
        contextObject: Any,
        httpServletRequest: HttpServletRequest?,
        isWriteRequestBody: Boolean
    ) {
        isDefaultInit = false
        init(contextObject, httpServletRequest, isWriteRequestBody)
    }

    private fun init(
        contextObject: Any,
        httpServletRequest: HttpServletRequest?,
        isWriteRequestBody: Boolean
    ) {
        this.contextObject = contextObject
        log =
            LogFactory.getLog(if (contextObject is Class<*>) contextObject else contextObject.javaClass)
        isWriteRequest = isWriteRequestBody
        isLogPrinted = false
        logParam = LinkedHashMap()
        lstLogParam = ArrayList()
        httpServletRequest?.let { updateHttpServletRequest(it) }
    }

    fun setHttpServletRequest(httpServletRequest: HttpServletRequest) {
        updateHttpServletRequest(httpServletRequest)
    }

    private fun updateHttpServletRequest(httpServletRequest: HttpServletRequest) {
        setRequest(httpServletRequest)
        setDefaultLogParam(httpServletRequest)
    }

    fun setRequest(request: String) {
        this.request = request
        updatedRequest = hideJsonValue(request, *HIDDEN_JSON_REQ)
    }

    private fun setRequest(httpServletRequest: HttpServletRequest) {
        try {
            when (httpServletRequest.method) {
                "POST", "DELETE" -> try {
                    val `in` = httpServletRequest.inputStream
                    request = getStringFromInputStream(`in`)
                } catch (e: IOException) {
                    throw WingException(DEFAULT_ERROR_CODE, e)
                }
                else -> request = httpServletRequest.getParameter(PARAMS_REQUEST)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            log!!.error("Error", e)
        }
    }

    private fun setDefaultLogParam(httpServletRequest: HttpServletRequest) {
        logParam!![REMOTE_ADDR_KEY] = httpServletRequest.remoteAddr
        logParam!![CONTEXT_PATH_KEY] = httpServletRequest.contextPath
        logParam!![ENDPOINT_KEY] =
            httpServletRequest.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern")
                .toString()
        if (logParam!![ENDPOINT_KEY] != httpServletRequest.servletPath) {
            logParam!![SERVLET_PATH_KEY] = httpServletRequest.servletPath
        }
        if (httpServletRequest.queryString != null) {
            logParam!![SERVLET_PATH_KEY] =
                httpServletRequest.servletPath + "?" + httpServletRequest.queryString
        }
        logParam!![METHOD_KEY] = httpServletRequest.method
        logParam!![REMOTE_USER_KEY] = httpServletRequest.remoteUser
        if (isWriteRequest && request != null) {
            updatedRequest = hideJsonValue(request!!, *HIDDEN_JSON_REQ)
            logParam!![REQUEST_BODY_KEY] = updatedRequest!!
        }
    }

    @Throws(WingException::class)
    private fun getStringFromInputStream(`is`: InputStream): String {
        var br: BufferedReader? = null
        val sb = StringBuilder()
        var line: String?
        try {
            br = BufferedReader(InputStreamReader(`is`, "UTF-8"))
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
        } catch (e: IOException) {
            throw WingException(DEFAULT_ERROR_CODE, e)
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    throw WingException(DEFAULT_ERROR_CODE, e)
                }
            }
        }
        return sb.toString()
    }

    fun setLogParam(key: String, value: String) {
        logParam!![key] = value
    }

    fun getLstLogParam(): List<LogParam>? {
        return lstLogParam
    }

    fun addLogParam(methodHeader: String?, logParam: LinkedHashMap<String, String>) {
        val logParamModel = LogParam(methodHeader!!, logParam)
        lstLogParam!!.add(logParamModel)
    }

    fun getWingAccount(): String? {
        return wingAccount
    }

    fun setWingAccount(wingAccount: String?) {
        this.wingAccount = wingAccount
        if (wingLogger != null) wingLogger!!.setWingAccount(wingAccount)
    }

    fun getMasterAccount(): String? {
        return masterAccount
    }

    fun setMasterAccount(masterAccount: String?) {
        this.masterAccount = masterAccount
        if (wingLogger != null) wingLogger!!.setMasterAccount(masterAccount)
    }

    fun setOptionsResult(abstractOptions: Any?, optionsResult: String?) {
        this.abstractOptions = abstractOptions
        this.optionsResult = optionsResult
    }

    fun getException(): Throwable? {
        return exception
    }

    fun setException(exception: Throwable?) {
        this.exception = WingLoggerException(exception)
        this.exception!!.actualException = exception
    }

    /**
     * Will remove later
     */
    @Deprecated("")
    fun setException(exception: Throwable?, contextObject: Class<*>?) {
        this.exception = WingLoggerException(exception)
        this.exception!!.actualException = exception
        this.contextObject = contextObject
    }

    fun printLog() {
        isLogPrinted = true
        if (!isDefaultInit) {
            logMasterAccount()
            logWingAccount()
            logAbstractOptionsResult()
            logXMLToCore()
            logCoreResponse()
            logRequestBody()
            logResponse()
            logException()
        }
        if (lstLogParam != null && lstLogParam!!.size > 0) {
            log!!.info(getPrettyHeader(logHeader, logParam, lstLogParam))
        } else {
            log!!.info(getPrettyHeader(logHeader, logParam))
        }
    }

    fun printLog(response: Any?) {
        this.response = response
        printLog()
    }

    private fun logMasterAccount() {
        if (masterAccount != null) logParam!![MASTER_ACCOUNT_KEY] = masterAccount!!
    }

    private fun logWingAccount() {
        if (wingAccount != null) logParam!![WING_ACCOUNT_KEY] = wingAccount!!
    }

    private fun logAbstractOptionsResult() {
        if (abstractOptions != null && optionsResult != null) {
            logParam!![abstractOptions!!.javaClass.simpleName] = minifyString(optionsResult)
        }
    }

    private fun logXMLToCore() {
        if (xmlToCore != null) {
            // Hide PIN and minify xml
            logParam!![XML_TO_CORE_KEY] = minifyString(hideXMLValue(xmlToCore!!, *HIDDEN_XML_REQ))
        }
    }

    private fun logCoreResponse() {
        if (coreResponse != null) {
            // 1. Hide BALANCE, PASSCODE, PAYERSMS
            coreResponse = hideXMLValue(coreResponse!!, *HIDDEN_XML_RESP)
            // 2. Minify XML
            coreResponse = minifyString(coreResponse)
            // 3. Write Log
            logParam!![CORE_RESP_KEY] = coreResponse!!
        }
    }

    private fun logRequestBody() {
        if (updatedRequest != null) {
            logParam!!.remove(REQUEST_BODY_KEY)
            logParam!![REQUEST_BODY_KEY] = updatedRequest!!
        }
    }

    private fun logResponse() {
        if (response != null) {
            logParam!![JSON_RESP_KEY] = hideJsonValue(response!!, *HIDDEN_JSON_RESP)
        }
    }

    private fun logException() {
        if (exception != null) {
            try {
                val requestManager = ErrorUtil()
                logParam!![ERROR_RESP_KEY] =
                    requestManager.getErrorMessageNoLog(exception!!.actualException)
                logParam!![ERROR_STACKTRACE_KEY] = getStackTrace(exception!!)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw, true)
        throwable.printStackTrace(pw)
        return sw.buffer.toString()
    }

    private fun minifyString(xml: String?): String {
        return xml!!.replace("[\\r\\n\\t]+".toRegex(), "")
    }

    private fun hideXMLValue(xml: String, vararg xmlTags: String): String {
        var xml = xml
        for (xmlTag in xmlTags) {
            val tagValue = getXMLValue(xml, xmlTag)
            if (tagValue != null && !tagValue.isEmpty()) {
                xml = xml.replace(tagValue, "****")
            }
        }
        return xml
    }

    private fun hideJsonValue(jsonObj: Any, vararg jsonKeys: String): String {
        var jsonElement: JsonElement
        jsonElement = if (jsonObj is String) {
            jsonParser.parse(jsonObj.toString())
        } else {
            gson.toJsonTree(jsonObj)
        }
        if (jsonElement.isJsonPrimitive) {
            jsonElement = jsonParser.parse(jsonElement.asJsonPrimitive.asString)
        }
        for (jsonKey in jsonKeys) {
            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject
                if (jsonObject[jsonKey] != null) {
                    jsonObject.addProperty(jsonKey, "****")
                }
            }
        }
        return gson.toJson(jsonElement)
    }

    private fun getXMLValue(xml: String, xmlTag: String): String? {
        val startTag = "<$xmlTag>"
        val endTag = "</$xmlTag>"
        val intTypeIndex = xml.indexOf(startTag)
        if (intTypeIndex == -1) {
            return null
        }
        val tagLength = xml.indexOf(endTag)
        val intStart = intTypeIndex + startTag.length
        return xml.substring(intStart, tagLength).trim { it <= ' ' }
    }

    companion object {
        private const val PARAMS_REQUEST = "request"
    }
}