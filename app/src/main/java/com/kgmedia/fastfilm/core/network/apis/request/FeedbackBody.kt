package com.kgmedia.fastfilm.core.network.apis.request


data class FeedbackBody(val feedbackType: Int?,
                        val feedbackTextContent: String?,
                        var feedbackImgs:List<String>?=null,
                        val mobileTel:String?) : UserTokenBody()