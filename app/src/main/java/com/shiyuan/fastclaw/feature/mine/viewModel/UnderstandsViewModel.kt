package com.shiyuan.fastclaw.feature.mine.viewModel

import FieldItems
import UnderstandingDetail
import UnderstandingScore
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

    fun setMockData() {
        // titleInfo - 顶部标题信息
        val titleInfo = listOf(
            FieldItems("name", "姓名", fieldValue = "王芝芝"),
            FieldItems("jobStatus", "求职紧迫度", fieldValue = "在职-月内到岗")
        )

        // basicInfo - 基础信息（注意：如果你需要用到 basicInfo，需在 UnderstandingDetail 中添加该字段）
        val basicInfo = listOf(
            FieldItems(
                "basicInfo", "基础信息", "BASIC_INFO",
                fieldItems = listOf(
                    FieldItems("age", "年龄", fieldValue = "25"),
                    FieldItems("location", "所在地", fieldValue = "北京"),
                    FieldItems("gender", "性别", fieldValue = "女"),
                    FieldItems("email", "邮箱", fieldValue = "wangzhizhi@example.com"),
                    FieldItems("phone", "手机号", fieldValue = "13800138000"),
                    FieldItems("politicalStatus", "政治面貌", fieldValue = "群众"),
                    FieldItems("maritalStatus", "婚姻状况", fieldValue = "未婚"),
                    FieldItems("birthday", "出生日期", fieldValue = "1999-05-20"),
                    FieldItems("currentAddress", "现居住地址", fieldValue = "北京市海淀区"),
                    FieldItems("registeredAddress", "户籍地址", fieldValue = "湖北省武汉市"),
                    FieldItems("wechatId", "微信号", fieldValue = "wangzhizhi_dev"),
                    FieldItems(
                        "avatarUrl",
                        "头像链接",
                        fieldValue = "https://oss.example.com/avatar/wangzhizhi.png"
                    ),
                    FieldItems("ethnicity", "民族", fieldValue = "汉族"),
                    FieldItems(
                        "resumeUpdatedAt",
                        "简历更新时间",
                        fieldValue = "2026-05-20T10:30:00+08:00"
                    ),
                    FieldItems(
                        "selfEvaluation",
                        "自我评价",
                        fieldValue = "熟悉高并发后端服务建设，关注代码质量和业务结果。"
                    )
                )
            )
        )

        // resumeInfo - 简历信息
        val resumeInfo = listOf(
            // 教育经历（注意嵌套：学校 -> 学位/专业等）
            FieldItems(
                "educationInfo", "教育经历", "EDUCATION_INFO",
                fieldItems = listOf(
                    FieldItems(
                        "schoolName", "学校名称", fieldValue = "北京理工大学",
                        fieldItems = listOf(
                            FieldItems("degree", "学历", fieldValue = "本科"),
                            FieldItems("major", "专业", fieldValue = "计算机科学与技术"),
                            FieldItems("fullTime", "是否全日制", fieldValue = "true"),
                            FieldItems("schoolType", "院校类型", fieldValue = "211")
                        )
                    )
                )
            ),
            // 工作经历（包含多条，每条内嵌套岗位、时间等）
            FieldItems(
                "workExperience", "工作经历", "WORK_EXPERIENCE",
                fieldItems = listOf(
                    FieldItems(
                        "companyName", "公司名称", fieldValue = "字节跳动",
                        fieldItems = listOf(
                            FieldItems(
                                "positionName",
                                "岗位名称",
                                fieldValue = "Java后端开发工程师"
                            ),
                            FieldItems("startDate", "开始时间", fieldValue = "2023-07"),
                            FieldItems("endDate", "结束时间", fieldValue = "至今"),
                            FieldItems("industry", "行业", fieldValue = "互联网"),
                            FieldItems(
                                "workContent",
                                "工作内容",
                                fieldValue = "负责招聘业务核心服务、候选人画像和岗位推荐链路开发。"
                            ),
                            FieldItems(
                                "achievement",
                                "工作成果",
                                fieldValue = "完成推荐召回链路改造，核心接口平均耗时下降 30%。"
                            )
                        )
                    ),
                    FieldItems(
                        "companyName", "公司名称", fieldValue = "腾讯科技",
                        fieldItems = listOf(
                            FieldItems(
                                "positionName",
                                "岗位名称",
                                fieldValue = "高级Java开发工程师"
                            ),
                            FieldItems("startDate", "开始时间", fieldValue = "2021-06"),
                            FieldItems("endDate", "结束时间", fieldValue = "2023-06"),
                            FieldItems("industry", "行业", fieldValue = "互联网"),
                            FieldItems(
                                "workContent",
                                "工作内容",
                                fieldValue = "负责内容审核平台后台服务、任务调度和权限模块开发。"
                            ),
                            FieldItems(
                                "achievement",
                                "工作成果",
                                fieldValue = "支撑日均百万级审核任务稳定流转。"
                            )
                        )
                    )
                )
            ),
            // 项目经历
            FieldItems(
                "projectExperience", "项目经历", "PROJECT_EXPERIENCE",
                fieldItems = listOf(
                    FieldItems(
                        "projectName", "项目名称", fieldValue = "候选人画像推荐系统",
                        fieldItems = listOf(
                            FieldItems("projectRole", "项目角色", fieldValue = "核心开发"),
                            FieldItems(
                                "projectDescription",
                                "项目描述",
                                fieldValue = "基于简历解析、画像标签和岗位画像构建候选人推荐链路。"
                            )
                        )
                    )
                )
            ),
            // 精通技能（扁平列表，每个技能作为一个 FieldItems，无嵌套）
            FieldItems("expertSkills", "精通技能", "SKILL_INFO", fieldValue = "Java"),
            FieldItems("expertSkills", "精通技能", "SKILL_INFO", fieldValue = "Spring Boot"),
            FieldItems("expertSkills", "精通技能", "SKILL_INFO", fieldValue = "MySQL"),
            FieldItems("masterSkills", "熟练技能", "SKILL_INFO", fieldValue = "RocketMQ"),
            FieldItems("masterSkills", "熟练技能", "SKILL_INFO", fieldValue = "Redis"),
            FieldItems("beginnerSkills", "入门技能", "SKILL_INFO", fieldValue = "Milvus"),
            // 语言能力（嵌套：语言 -> 等级）
            FieldItems(
                "languageSkills", "语言能力", "SKILL_INFO",
                fieldItems = listOf(
                    FieldItems(
                        "languageName", "语言名称", fieldValue = "英语",
                        fieldItems = listOf(
                            FieldItems("level", "等级", fieldValue = "CET-6")
                        )
                    )
                )
            ),
            // 证书
            FieldItems(
                "certificates", "证书", "CERTIFICATE_INFO",
                fieldItems = listOf(
                    FieldItems(
                        "certificateName", "证书名称", fieldValue = "系统架构设计师",
                        fieldItems = listOf(
                            FieldItems(
                                "issueOrg",
                                "颁发机构",
                                fieldValue = "人力资源和社会保障部"
                            ),
                            FieldItems("issueTime", "颁发时间", fieldValue = "2024-11")
                        )
                    )
                )
            ),
            // 奖项
            FieldItems(
                "awards", "奖项", "CERTIFICATE_INFO",
                fieldItems = listOf(
                    FieldItems(
                        "awardName", "奖项名称", fieldValue = "校级优秀毕业设计",
                        fieldItems = listOf(
                            FieldItems("awardLevel", "奖项级别", fieldValue = "校级"),
                            FieldItems("awardTime", "获奖时间", fieldValue = "2021-06")
                        )
                    )
                )
            )
        )

        // jobPreference - 求职偏好
        val jobPreference = listOf(
            FieldItems(
                "expectedCity", "意向城市", "JOB_EXPECTATION",
                fieldItems = listOf(
                    FieldItems("expectedCity", "意向城市", fieldValue = "北京"),
                    FieldItems("expectedCity", "意向城市", fieldValue = "深圳")
                )
            ),
            FieldItems(
                "jobIntention", "意向岗位", "JOB_EXPECTATION",
                fieldItems = listOf(
                    FieldItems("jobIntention", "意向岗位", fieldValue = "Java后端开发")
                )
            ),
            FieldItems(
                "expectedSalary", "期望薪资", "JOB_EXPECTATION",
                fieldItems = listOf(
                    FieldItems("expectedSalary", "期望薪资", fieldValue = "25")
                )
            ),
            // 其他 SOFT_ATTRIBUTES 类型（扁平，无嵌套）
            FieldItems("jobType", "工作类型", "SOFT_ATTRIBUTES", fieldValue = "全职"),
            FieldItems(
                "acceptRelocation",
                "是否接受外派",
                "SOFT_ATTRIBUTES",
                fieldValue = "看情况"
            ),
            FieldItems(
                "latestLeavingReason",
                "离职原因",
                "SOFT_ATTRIBUTES",
                fieldValue = "希望寻找更贴近推荐系统和平台工程方向的机会。"
            ),
            FieldItems(
                "expectedIndustries",
                "期望行业",
                "SOFT_ATTRIBUTES",
                fieldValue = "互联网、企业服务"
            ),
            FieldItems(
                "companySize",
                "公司规模偏好",
                "SOFT_ATTRIBUTES",
                fieldValue = "中型公司"
            ),
            FieldItems("careerDirection", "职业方向", "SOFT_ATTRIBUTES", fieldValue = "专业"),
            FieldItems("workMode", "办公模式", "SOFT_ATTRIBUTES", fieldValue = "混合办公"),
            FieldItems(
                "workSchedule",
                "工作制度偏好",
                "SOFT_ATTRIBUTES",
                fieldValue = "双休优先"
            ),
            FieldItems(
                "acceptOvertime",
                "是否接受加班",
                "SOFT_ATTRIBUTES",
                fieldValue = "可接受项目节点阶段性加班"
            ),
            FieldItems(
                "salaryStructure",
                "薪资结构偏好",
                "SOFT_ATTRIBUTES",
                fieldValue = "固定薪资为主，绩效奖金可接受"
            ),
            FieldItems(
                "teamAtmosphere",
                "团队氛围偏好",
                "SOFT_ATTRIBUTES",
                fieldValue = "技术氛围好，协作直接"
            ),
            FieldItems(
                "benefits",
                "福利偏好",
                "SOFT_ATTRIBUTES",
                fieldValue = "补充医疗、年度体检、学习预算"
            )
        )
        this.resumeInfo.postValue(resumeInfo)
        this.jobPreference.postValue(jobPreference)
        this. titleInfo.postValue(titleInfo)
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
}

