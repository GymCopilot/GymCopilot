package com.zexo.gymcopilot.utils

private var cachedLocalMemberData: LocalMemberData? = null

actual fun saveLocalMemberProfile(data: LocalMemberData) {
    cachedLocalMemberData = data
}

actual fun readLocalMemberProfile(): LocalMemberData? {
    return cachedLocalMemberData
}
