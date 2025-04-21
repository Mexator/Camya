package com.mexator.camya.data.userinfo.api.dto

import com.google.gson.annotations.SerializedName

class UserDto(
    @SerializedName("display_name")
    val username: String,
    @SerializedName("real_name")
    val name: String,
    @SerializedName("default_avatar_id")
    val avatarId: String
)