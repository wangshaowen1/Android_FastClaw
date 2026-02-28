package com.kgmedia.fastfilm.feature.creator.data

class DramaGenerateConstants {
    companion object {
        /**
         * 继续创作(灵感)
         */
        const val MODEL_CONTINUE_INSPIRATION = 33

        /**
         * 继续创作(小说)
         */
        const val MODEL_CONTINUE_STORY = 31

        /**
         * 继续创作(小说)-帮我扩写
         */
        const val MODEL_CONTINUE_STORY_EXPAND = 32

        /**
         * 第一集创作(灵感)
         */
        const val MODEL_INSPIRATION = 30

        /**
         * 第一集创作(小说)
         * todo 注意这里不是对应的 bizType值，这里是对应的业务类型
         */
        const val MODEL_STORY = 29

        /**
         * 第一集创作(小说文档模式)
         */
        const val MODEL_STORY_URL = 35


        /**
         * 继续创作(小说文档模式)
         */
        const val CONTINUE_WRITING_NOVEL_DOCUMENTS = 35


        /**
         * 剧集梗概
         */
        const val MODEL_EPISODE_SYNOPSIS = 20

        /**
         * 优化作品简介
         */
        const val MODEL_OPTIMIZE_DRAMA_DESC = 34


    }
}