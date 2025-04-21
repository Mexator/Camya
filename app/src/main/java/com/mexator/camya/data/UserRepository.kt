package com.mexator.camya.data

import com.mexator.camya.data.model.User
import com.mexator.camya.data.userinfo.api.UserAPI
import com.mexator.camya.data.userinfo.api.dto.UserDto
import io.reactivex.Single

class UserRepository(
    private val userAPI: UserAPI
) {
    private var cachedUser: User? = null

    fun getUser(token: String): Single<User> {
        if (cachedUser != null) return Single.just(cachedUser)
        return userAPI.getUserInfo("OAuth $token")
            .map(::toUser)
    }

    private fun toUser(dto: UserDto): User {
        return User(
            username = dto.username,
            name = dto.name,
            avatarUrl = "https://avatars.yandex.net/get-yapic/${dto.avatarId}/islands-200"
        )
    }
}