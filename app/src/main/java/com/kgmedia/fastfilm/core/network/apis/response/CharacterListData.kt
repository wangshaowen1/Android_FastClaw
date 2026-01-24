package com.kgmedia.fastfilm.core.network.apis.response

import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo

data class CharacterListData(val dramasId: String?, val dramasName: String?, val roleList: List<CharacterInfo>?)