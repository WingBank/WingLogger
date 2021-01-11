package com.wingmoney.winglogger.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
class BaseResponse : Serializable {
    @SerializedName("error_code")
    @JsonProperty("error_code")
    var errorCode: String? = null

    @SerializedName("message")
    @JsonProperty("message")
    var message: String? = null

    @SerializedName("message_kh")
    @JsonProperty("message_kh")
    var messageKh: String? = null

    @SerializedName("message_ch")
    @JsonProperty("message_ch")
    var messageCh: String? = null

    companion object {
        private const val serialVersionUID = 1L
    }
}