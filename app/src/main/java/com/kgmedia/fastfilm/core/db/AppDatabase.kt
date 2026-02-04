package com.kgmedia.fastfilm.core.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kgmedia.fastfilm.core.db.dao.AIMessageDao
import com.kgmedia.fastfilm.core.db.dao.ChatSceneDao
import com.kgmedia.fastfilm.core.db.dao.SearchHistoryDao
import com.kgmedia.fastfilm.core.db.dao.ShortDramaDao
import com.kgmedia.fastfilm.core.db.dao.WorkInformationDao
import com.kgmedia.fastfilm.core.db.entity.AIMessage
import com.kgmedia.fastfilm.core.db.entity.ChatScene
import com.kgmedia.fastfilm.core.db.entity.SearchHistory
import com.kgmedia.fastfilm.core.db.entity.Tone
import com.kgmedia.fastfilm.core.db.entity.WorkInformationInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.nhsd.basic.BaseApp

@Database(
    entities = [
        AIMessage::class,
        ChatScene::class,
        SearchHistory::class,
        Tone::class,
        ShortDramaInfo::class,
        WorkInformationInfo::class,
    ], version = 4, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract val aiMessageDao: AIMessageDao
    abstract val chatSceneDao: ChatSceneDao
    abstract val searchHistoryDao: SearchHistoryDao
    abstract val shortDramsDao: ShortDramaDao

    abstract fun workInformationDao(): WorkInformationDao

    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance() = instance ?: synchronized(AppDatabase::class.java) {
            instance ?: buildDatabase().also { instance = it }
        }

        private fun buildDatabase(): AppDatabase = Room.databaseBuilder(
            BaseApp.getInstance(),
            AppDatabase::class.java,
            "AiChatDatabase.db"
        )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(false)//数据库更新出错即回滚 会丢失数据
            .addMigrations()//更新数据库的Migrations对象
            .build()
    }
}