package com.mexator.camya.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("display_name")
    val username: String,
    @SerializedName("real_name")
    val name: String)
