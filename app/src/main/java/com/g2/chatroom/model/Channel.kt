package com.g2.chatroom.model

data class Channel(
    val id: String = "",
    val name: String,
//    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()

)