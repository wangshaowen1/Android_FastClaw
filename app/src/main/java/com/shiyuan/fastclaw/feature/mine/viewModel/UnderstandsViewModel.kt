package com.shiyuan.fastclaw.feature.mine.viewModel


import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldTree
import com.shiyuan.fastclaw.core.network.apis.response.AttributeOption
import com.shiyuan.fastclaw.core.network.apis.response.FieldItems
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeInfoSection
import com.shiyuan.fastclaw.core.network.apis.response.UnderstandingScore
import com.shiyuan.fastclaw.core.response
import kotlin.String

/**
 */
class UnderstandsViewModel : AppRepositoryViewModel<AppRepository>() {


    val mToolbarVMUnderstands = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.shrimp_understands.getResString(),
        background = Color.TRANSPARENT,

        )

    var tableIndex = MutableLiveData(0)

    var resumeInfo = MutableLiveData<List<FieldItems>>()
    var jobPreference = MutableLiveData<List<FieldItems>>()
    var titleInfo = MutableLiveData<List<FieldItems>>()
    var onlineResumeBaseInfo = MutableLiveData<List<AttributeArrayQueryData>>()
    var onlineResumeInfo = MutableLiveData<List<AttributeArrayQueryData>>()


    var underStandingScore = MutableLiveData<UnderstandingScore>()
     fun understandingScore() {
            launch(block = {
                response(
                    ApiFactory.apiService.understandingScore(),
                    result = {
                        underStandingScore.postValue(this.data)
                    },
                    error = {
                        this?.msg?.showToast()
                    })
            }, onError = {
                parseMessage(it)?.showToast()
            }, onComplete = {
                setLoading(false)
            })
        }

    fun understandingDetail() {
        setLoading(true)
        launch(block = {
            response(
                ApiFactory.apiService.understandingDetail(),
                result = {
                    resumeInfo.postValue(this.data?.resumeInfo)
                    jobPreference.postValue(this.data?.jobPreference)
                    titleInfo.postValue(this.data?.titleInfo)
                },
                error = {
                    this?.msg?.showToast()
                })
        }, onError = {
            parseMessage(it)?.showToast()
        }, onComplete = {
            setLoading(false)
        })
    }


    fun queryOnlineResumeInfo2() {
        setLoading(true)
        launch(block = {
            response(
                mRepo.api.queryOnlineResumeInfo(),
                result = {
                    val sections = this.data?.sections.orEmpty()
                    val baseInfo = mutableListOf<AttributeArrayQueryData>()
                    val resumeInfo = mutableListOf<AttributeArrayQueryData>()
                    sections.forEach { section ->
                        if (section.sectionCode == SECTION_BASE_INFO) {
                            baseInfo.addAll(section.fields)
                        } else {
                            resumeInfo.addAll(section.fields)
                        }
                    }
                    onlineResumeBaseInfo.postValue(baseInfo)
                    onlineResumeInfo.postValue(resumeInfo)
                },
                error = {
                    this?.msg?.showToast()
                })
        }, onError = {
            parseMessage(it)?.showToast()
        }, onComplete = {
            setLoading(false)
        })
    }

    fun queryOnlineResumeInfo() {
        postOnlineResumeSections(mockOnlineResumeInfoSections())
    }

    private fun postOnlineResumeSections(sections: List<OnlineResumeInfoSection>) {
        val baseInfo = mutableListOf<AttributeArrayQueryData>()
        val resumeInfo = mutableListOf<AttributeArrayQueryData>()
        sections.forEach { section ->
            if (section.sectionCode == SECTION_BASE_INFO) {
                baseInfo.addAll(section.fields)
            } else {
                resumeInfo.addAll(section.fields)
            }
        }
        onlineResumeBaseInfo.postValue(baseInfo)
        onlineResumeInfo.postValue(resumeInfo)
    }

    private fun mockOnlineResumeInfoSections(): List<OnlineResumeInfoSection> {
        return listOf(
            OnlineResumeInfoSection(
                sectionCode = SECTION_BASE_INFO,
                sectionName = "个人基础信息",
                fields = listOf(
                    mockField("avatarUrl", "头像", null, "https://cdn.example.com/avatar/10001.png"),
                    mockField("name", "姓名", "TEXT", "张志源"),
                    mockField(
                        "gender",
                        "性别",
                        "SINGLE_CHOICE",
                        "男",
                        value = choice("MALE", "男"),
                        options = listOf(choiceOption("MALE", "男"), choiceOption("FEMALE", "女"))
                    ),
                    mockField("birthday", "出生年月", "DATE_MONTH", "2004-08"),
                    mockField("age", "年龄", null, "20岁", value = 20),
                    mockField("phone", "手机号", null, "17883749009"),
                    mockField("wechatId", "微信号", "TEXT", "wx_zhangzhiyuan"),
                    mockField("email", "邮箱", "TEXT", "17883749@qq.com"),
                    mockField(
                        "location",
                        "现居住城市",
                        "DICT_SINGLE",
                        "北京",
                        dictType = "REGION",
                        dictApi = "/candidate/app/jobAgent/dict/region/query",
                        value = choice("110100", "北京")
                    ),
                    mockField(
                        "politicalStatus",
                        "政治面貌",
                        "SINGLE_CHOICE",
                        "中共党员",
                        value = choice("CPC_MEMBER", "中共党员"),
                        options = listOf(
                            choiceOption("CPC_MEMBER", "中共党员"),
                            choiceOption("CPC_PROBATIONARY_MEMBER", "中共预备党员"),
                            choiceOption("CYL_MEMBER", "共青团员"),
                            choiceOption("DEMOCRATIC_PARTY", "民主党派"),
                            choiceOption("MASSES", "群众")
                        )
                    ),

                )
            ),
            OnlineResumeInfoSection(
                sectionCode = "selfEvaluation",
                sectionName = "自我评价",
                fields = listOf(
                    mockField(
                        "selfEvaluation",
                        "自我评价",
                        "TEXT",
                        "责任心强，具备良好沟通协调与团队协作意识。"
                    )
                )
            ),
            OnlineResumeInfoSection(
                sectionCode = "workExperience",
                sectionName = "工作经历",
                fields = listOf(
                    mockGroup(
                        groupCode = "workExperience",
                        groupName = "工作经历",
                        displayValue = "1段工作经历",
                        skippable = false,
                        fieldTree = listOf(
                            mockTree(
                                titleCode = "companyName",
                                titleName = "公司名称",
                                titleValue = "深圳市诚海科技有限公司",
                                fieldItems = listOf(
                                    mockItem("companyName", "公司名称", "深圳市诚海科技有限公司", "TEXT", required = true),
                                    mockItem(
                                        "position",
                                        "职位名称",
                                        "UI设计师",
                                        "DICT_SINGLE",
                                        value = choice("1200103", "UI设计师"),
                                        dictType = "POSITION",
                                        dictApi = "/candidate/app/jobAgent/dict/position/query",
                                        required = true
                                    ),
                                    mockItem("startDate", "开始时间", "2021-09", "DATE_MONTH", required = true),
                                    mockItem("endDate", "结束时间", "2024-05", "DATE_MONTH", required = true),
                                    mockItem("workContent", "工作内容", "负责产品视觉设计、组件搭建和日常版本迭代。", "TEXT", required = true),
                                    mockItem("duration", "工作时长", "32个月", "TEXT"),
                                    mockItem("industry", "所属行业", "互联网", "DICT_SINGLE", value = choice("internet", "互联网")),
                                    mockItem("companyScale", "公司规模", "100-499", "SINGLE_CHOICE", value = choice("SIZE_100_499", "100-499")),
                                    mockItem("companyType", "公司类型", "民营企业", "SINGLE_CHOICE", value = choice("PRIVATE", "民营企业")),
                                    mockItem("teamSize", "团队规模", "8人", "TEXT"),
                                    mockItem("leavingReason", "离职原因", "薪资未达预期", "SINGLE_CHOICE", value = choice("SALARY_NOT_EXPECTED", "薪资未达预期"))
                                )
                            )
                        )
                    )
                )
            ),
            OnlineResumeInfoSection(
                sectionCode = "projectExperience",
                sectionName = "项目经历",
                fields = listOf(
                    mockGroup(
                        groupCode = "projectExperience",
                        groupName = "项目经历",
                        displayValue = "1段项目经历",
                        skippable = true,
                        fieldTree = listOf(
                            mockTree(
                                titleCode = "projectName",
                                titleName = "项目名称",
                                titleValue = "社交钱包 web3 海外版",
                                fieldItems = listOf(
                                    mockItem("projectName", "项目名称", "社交钱包 web3 海外版", "TEXT", required = true),
                                    mockItem("projectRole", "项目角色", "UI设计师", "TEXT", required = true),
                                    mockItem("position", "项目角色", "UI设计师", "TEXT", required = true),
                                    mockItem("description", "项目描述", "面向海外用户的钱包产品视觉改版。", "TEXT", required = true),
                                    mockItem("startDate", "开始时间", "2022-01", "DATE_MONTH"),
                                    mockItem("endDate", "结束时间", "2022-12", "DATE_MONTH"),
                                    mockItem("duration", "项目周期", "12个月", "TEXT"),
                                    mockItem("projectScale", "项目规模", "8人项目组", "TEXT"),
                                    mockItem("techStack", "技术栈", "Figma、Axure", "TEXT"),
                                    mockItem("responsibilities", "项目职责", "负责视觉规范、核心页面和组件设计。", "TEXT"),
                                    mockItem("results", "项目成果", "完成海外版核心链路改版并上线。", "TEXT"),
                                    mockItem("collaborators", "协作部门", "产品、研发、测试", "TEXT")
                                )
                            )
                        )
                    )
                )
            ),
            OnlineResumeInfoSection(
                sectionCode = "educationInfo",
                sectionName = "教育经历",
                fields = listOf(
                    mockGroup(
                        groupCode = "educationInfo",
                        groupName = "教育经历",
                        displayValue = "1段教育经历",
                        skippable = false,
                        fieldTree = listOf(
                            mockTree(
                                titleCode = "schoolName",
                                titleName = "院校名称",
                                titleValue = "北京大学",
                                fieldItems = listOf(
                                    mockItem("schoolName", "院校名称", "北京大学", "TEXT", required = true),
                                    mockItem("degree", "学历", "本科", "SINGLE_CHOICE", value = choice("BACHELOR", "本科"), required = true),
                                    mockItem("major", "专业", "视觉传达设计", "TEXT", required = true),
                                    mockItem("startDate", "入学时间", "2021-09", "DATE_MONTH", required = true),
                                    mockItem("endDate", "毕业时间", "2024-05", "DATE_MONTH", required = true),
                                    mockItem("fullTime", "是否全日制", "全日制", "SINGLE_CHOICE", value = choice("true", "全日制")),
                                    mockItem("schoolType", "院校性质", "985", "SINGLE_CHOICE", value = choice("PROJECT_985", "985")),
                                    mockItem("gpa", "GPA成绩", "3.7/4.0", "TEXT"),
                                    mockItem("rankingOrHonors", "在校排名/荣誉", "校级优秀毕业生", "TEXT"),
                                    mockItem("mainCourses", "主修课程", "视觉设计、交互设计", "TEXT"),
                                    mockItem("description", "教育补充描述", "参与多个校内设计实践项目。", "TEXT")
                                )
                            )
                        )
                    )
                )
            ),
            OnlineResumeInfoSection(
                sectionCode = "skills",
                sectionName = "技能",
                fields = listOf(
                    mockGroup(
                        groupCode = "skills",
                        groupName = "技能",
                        displayValue = "1项技能",
                        skippable = false,
                        fieldTree = listOf(
                            mockTree(
                                titleCode = "skillName",
                                titleName = "技能名称",
                                titleValue = "Axure",
                                fieldItems = listOf(
                                    mockItem("skillName", "技能名称", "Axure", "TEXT", required = true),
                                    mockItem("proficiency", "熟练度", "熟练", "SINGLE_CHOICE", value = choice("SKILLED", "熟练"))
                                )
                            )
                        )
                    )
                )
            ),
            OnlineResumeInfoSection(
                sectionCode = "awards",
                sectionName = "奖项",
                fields = listOf(
                    mockGroup(
                        groupCode = "awards",
                        groupName = "奖项",
                        displayValue = "1项奖项",
                        skippable = true,
                        fieldTree = listOf(
                            mockTree(
                                titleCode = "awardName",
                                titleName = "奖项名称",
                                titleValue = "国家奖学金",
                                fieldItems = listOf(
                                    mockItem("awardName", "奖项名称", "国家奖学金", "TEXT", required = true),
                                    mockItem("awardLevel", "奖项等级", "国家级", "SINGLE_CHOICE", value = choice("NATIONAL", "国家级")),
                                    mockItem("awardTime", "获奖时间", "2023-06", "DATE_MONTH")
                                )
                            )
                        )
                    )
                )
            ),
            OnlineResumeInfoSection(
                sectionCode = "certificates",
                sectionName = "证书",
                fields = listOf(
                    mockGroup(
                        groupCode = "certificates",
                        groupName = "证书",
                        displayValue = "1项证书",
                        skippable = true,
                        fieldTree = listOf(
                            mockTree(
                                titleCode = "certificateName",
                                titleName = "证书名称",
                                titleValue = "英语四级",
                                fieldItems = listOf(
                                    mockItem("certificateName", "证书名称", "英语四级", "TEXT", required = true),
                                    mockItem("issueOrg", "颁发机构", "教育部考试中心", "TEXT"),
                                    mockItem("issueTime", "颁发时间", "2022-12", "DATE_MONTH")
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    private fun mockField(
        code: String,
        name: String,
        inputMode: String?,
        displayValue: String,
        dictType: String? = null,
        dictApi: String? = null,
        value: Any? = displayValue,
        options: List<AttributeOption> = emptyList()
    ): AttributeArrayQueryData {
        return AttributeArrayQueryData(
            attributeGroupCode = code,
            attributeGroupName = name,
            metaCode = code,
            inputMode = inputMode,
            dictType = dictType,
            dictApi = dictApi,
            value = value,
            displayValue = displayValue,
            options = options
        )
    }

    private fun mockGroup(
        groupCode: String,
        groupName: String,
        displayValue: String,
        skippable: Boolean,
        fieldTree: List<AttributeFieldTree>
    ): AttributeArrayQueryData {
        return AttributeArrayQueryData(
            attributeGroupCode = groupCode,
            attributeGroupName = groupName,
            metaCode = "_group",
            inputMode = "ARRAY_FORM",
            displayValue = displayValue,
            skippable = skippable,
            complete = true,
            missingMetaCodes = mutableListOf(),
            fieldTree = fieldTree.toMutableList()
        )
    }

    private fun mockTree(
        titleCode: String,
        titleName: String,
        titleValue: String,
        fieldItems: List<AttributeFieldItem>
    ): AttributeFieldTree {
        return AttributeFieldTree(
            instanceIndex = 0,
            fieldCode = titleCode,
            fieldName = titleName,
            fieldValue = fieldValue(titleValue, titleValue),
            displayValue = titleValue,
            inputMode = "TEXT",
            required = true,
            missing = false,
            sortOrder = 10L,
            complete = true,
            statusCode = "FILLED",
            statusText = "已填写",
            missingMetaCodes = emptyList(),
            fieldItems = fieldItems
        )
    }

    private fun mockItem(
        code: String,
        name: String,
        displayValue: String,
        inputMode: String?,
        dictType: String? = null,
        dictApi: String? = null,
        value: Any? = displayValue,
        required: Boolean = false
    ): AttributeFieldItem {
        return AttributeFieldItem(
            fieldCode = code,
            fieldName = name,
            fieldValue = fieldValue(value, displayValue),
            displayValue = displayValue,
            inputMode = inputMode,
            dictType = dictType,
            dictApi = dictApi,
            required = required,
            missing = false,
            fieldItems = emptyList()
        )
    }

    private fun fieldValue(value: Any?, displayText: String): Map<String, Any?> {
        return mapOf(
            "value" to value,
            "displayText" to displayText
        )
    }

    private fun choice(code: String, name: String): Map<String, String> {
        return mapOf(
            "code" to code,
            "name" to name
        )
    }

    private fun choiceOption(code: String, name: String): AttributeOption {
        return AttributeOption(code = code, name = name)
    }

    val mToolbarVMOnlineResume = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )

    var tableOnlineResumeIndex = MutableLiveData(0)

    companion object {
        private const val SECTION_BASE_INFO = "baseInfo"
    }
}
