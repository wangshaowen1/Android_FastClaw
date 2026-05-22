package com.shiyuan.fastclaw.feature.mine.viewModel

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:10
 * @Version: 1.0
 * *****************************************
 */
class UnderstandsViewModel : AppRepositoryViewModel<AppRepository>() {


    val mToolbarVMUnderstands = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.shrimp_understands.getResString(),
        background = Color.TRANSPARENT,

        )

    var tableIndex = MutableLiveData(0)

    /**
     * 假数据列表
     */
    val mockDataList = listOf(
        MockUnderstandsData(
            name = "小虾理解",
            tag = arrayOf("经验要求", "专业技能"),
            value = arrayOf("3-5年", "Java, Python")
        ),
        MockUnderstandsData(
            name = "小虾理解",
            tag = arrayOf("学历要求", "语言能力"),
            value = arrayOf("本科", "英语 CET-6")
        ),
        MockUnderstandsData(
            name = "小虾理解",
            tag = arrayOf("薪资范围", "工作地点"),
            value = arrayOf("15K-25K", "北京·朝阳区")
        ),
        MockUnderstandsData(
            name = "小虾理解",
            tag = arrayOf("公司规模", "行业领域"),
            value = arrayOf("500-1000人", "互联网/电商")
        ),
        MockUnderstandsData(
            name = "小虾理解",
            tag = arrayOf("福利待遇", "加班情况"),
            value = arrayOf("五险一金, 年终奖, 带薪年假", "偶尔加班")
        )
    )
}

/**
 * 小虾理解数据模型
 */
data class MockUnderstandsData(
    val name: String,
    val tag: Array<String>,
    val value: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MockUnderstandsData
        if (name != other.name) return false
        if (!tag.contentEquals(other.tag)) return false
        if (!value.contentEquals(other.value)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + tag.contentHashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}