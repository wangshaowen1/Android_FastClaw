package com.shiyuan.fastclaw.feature.interview

data class InterviewRoomMessage(
    val id: String,
    val role: Role,
    val content: String,
) {
    enum class Role {
        AI,
        USER,
    }
}

enum class InterviewRoomState {
    STARTING,
    PREPARING,
    WAITING_TARGET_JOB,
    TARGET_JOB_PARSING,
    INTERVIEWING,
    ANSWERING,
    FINISHED,
    ERROR,
}
