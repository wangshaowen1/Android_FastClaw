package com.kgmedia.fastfilm.feature.play.data

import com.kgmedia.fastfilm.core.network.apis.entity.DramaEpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.DramaInfo

object FakeDataSource {

    fun getFakeData(pageNum: Int): List<DramaInfo> {
        if (pageNum > 3) return emptyList()

        return listOf(
            createFakeDrama(
                id = "${pageNum}_1",
                name = "Big Buck Bunny (大雄兔)",
                url = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                cover = "https://picsum.photos/seed/${pageNum}_1/720/1280",
                duration = 596474L
            ),
            createFakeDrama(
                id = "${pageNum}_2",
                name = "Elephant Dream",
                url = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                cover = "https://picsum.photos/seed/${pageNum}_2/720/1280",
                duration = 653622L
            ),
            createFakeDrama(
                id = "${pageNum}_3",
                name = "For Bigger Blazes",
                url = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                cover = "https://picsum.photos/seed/${pageNum}_3/720/1280",
                duration = 15000L
            ),
            createFakeDrama(
                id = "${pageNum}_4",
                name = "For Bigger Escapes",
                url = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                cover = "https://picsum.photos/seed/${pageNum}_4/720/1280",
                duration = 15000L
            ),
            createFakeDrama(
                id = "${pageNum}_5",
                name = "Tears of Steel",
                url = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                cover = "https://picsum.photos/seed/${pageNum}_5/720/1280",
                duration = 734137L
            )
        )
    }

    /**
     * 内部辅助方法，用于快速构建符合新结构的 DramaInfo
     */
    private fun createFakeDrama(
        id: String,
        name: String,
        url: String,
        cover: String,
        duration: Long
    ): DramaInfo {
        return DramaInfo(
            id = id,
            workName = name,
            coverUrl = cover,
            intro = "这是 $name 的作品简介...",
            workRatio = "9:16",
            episodeCount = 1,
            playCount = (1000..10000).random(),
            workStoryItemList = listOf(
                DramaEpisodeInfo(
                    id = "ep_$id",
                    videoUrl = url,
                    duration = duration,
                    workRatio = "9:16",
                    playCount = (100..1000).random()
                )
            )
        )
    }
}