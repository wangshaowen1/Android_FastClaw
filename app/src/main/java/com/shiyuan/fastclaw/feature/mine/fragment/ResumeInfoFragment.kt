package com.shiyuan.fastclaw.feature.mine.fragment

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldTree
import com.shiyuan.fastclaw.databinding.FragmentResumeInfoBinding
import com.shiyuan.fastclaw.databinding.ItemCertificateSkillsAwardsBinding
import com.shiyuan.fastclaw.databinding.ItemCertificateSkillsAwardsReBinding
import com.shiyuan.fastclaw.databinding.ItemEducationExperienceBinding
import com.shiyuan.fastclaw.databinding.ItemEducationExperienceReBinding
import com.shiyuan.fastclaw.databinding.ItemProjectExperienceBinding
import com.shiyuan.fastclaw.databinding.ItemProjectExperienceReBinding
import com.shiyuan.fastclaw.databinding.ItemResumeInfoBinding
import com.shiyuan.fastclaw.databinding.ItemWorkExperienceBinding
import com.shiyuan.fastclaw.databinding.ItemWorkExperienceReBinding
import com.shiyuan.fastclaw.feature.information.AwardActivity
import com.shiyuan.fastclaw.feature.information.CertificateActivity
import com.shiyuan.fastclaw.feature.information.EducationalActivity
import com.shiyuan.fastclaw.feature.information.PersonalInformationActivity
import com.shiyuan.fastclaw.feature.information.ProjectActivity
import com.shiyuan.fastclaw.feature.information.PublicDescriptionActivity
import com.shiyuan.fastclaw.feature.information.SkillActivity
import com.shiyuan.fastclaw.feature.information.WorkActivity
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration

class ResumeInfoFragment : BaseViewModelFragment<FragmentResumeInfoBinding, UnderstandsViewModel>() {

    companion object {
        fun newInstance(): ResumeInfoFragment = ResumeInfoFragment()

        const val INCOMPLETE = "INCOMPLETE"
        private const val GROUP_SELF_EVALUATION = "selfEvaluation"
        private const val GROUP_WORK_EXPERIENCE = "workExperience"
        private const val GROUP_PROJECT_EXPERIENCE = "projectExperience"
        private const val GROUP_EDUCATION_INFO = "educationInfo"
        private const val GROUP_EDUCATION_EXPERIENCE = "educationExperience"
        private const val GROUP_SKILLS = "skills"
        private const val GROUP_SKILL = "skill"
        private const val GROUP_AWARDS = "awards"
        private const val GROUP_AWARD = "award"
        private const val GROUP_CERTIFICATES = "certificates"
        private const val GROUP_CERTIFICATE = "certificate"

        private const val FIELD_START_DATE = "startDate"
        private const val FIELD_END_DATE = "endDate"
        private const val FIELD_POSITION = "position"
        private const val FIELD_COMPANY_NAME = "companyName"
        private const val FIELD_WORK_CONTENT = "workContent"
        private const val FIELD_RESPONSIBILITIES = "responsibilities"
        private const val FIELD_PROJECT_NAME = "projectName"
        private const val FIELD_SCHOOL_NAME = "schoolName"
        private const val FIELD_MAJOR = "major"
        private const val FIELD_SKILL_NAME = "skillName"
        private const val FIELD_CERTIFICATE_NAME = "certificateName"
        private const val FIELD_AWARD_NAME = "awardName"
    }

    private val resumeInfoAdapter by lazy {
        SingleBindingAdapter<ItemResumeInfoBinding, AttributeArrayQueryData>(
            R.layout.item_resume_info,
            emptyList(),
            onBind = { itemBinding, _, item ->
                bindResumeInfoItem(itemBinding, item)
            }
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
        mRealVM.queryOnlineResumeInfo()
    }

    private fun initView() {
        binding.rvResumeInfo.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            adapter = resumeInfoAdapter
        }
        updateResumeInfo(emptyList())
        binding.ivProfileEdit.setOnSingleClickListener {
            PersonalInformationActivity.start(requireActivity())
        }
    }

    private fun initObservers() {
        mRealVM.onlineResumeBaseInfo.observe(this) { data ->
            updateBaseResumeInfo(data.orEmpty())
        }
        mRealVM.onlineResumeInfo.observe(this) { data ->
            updateResumeInfo(data.orEmpty())
        }
    }

    private fun updateBaseResumeInfo(data: List<AttributeArrayQueryData>) {



    }

    private fun updateResumeInfo(data: List<AttributeArrayQueryData>) {
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.rvResumeInfo.visibility = if (showEmpty) View.GONE else View.VISIBLE
        resumeInfoAdapter.submitList(data)
    }

    private fun bindResumeInfoItem(
        itemBinding: ItemResumeInfoBinding,
        item: AttributeArrayQueryData
    ) {
        itemBinding.tvTitle.text = item.displayTitle()
        itemBinding.ivEdit.setOnSingleClickListener {
            handleEditIconClick(item)
        }
        val selfEvaluationText = item.selfEvaluationText()
        val showSelfEvaluation = selfEvaluationText.isNotBlank()
        itemBinding.tvName.text = selfEvaluationText
        itemBinding.tvName.visibility = if (showSelfEvaluation) View.VISIBLE else View.GONE
        itemBinding.layoutMissing.visibility = if (item.fieldTree == null && !showSelfEvaluation) View.VISIBLE else View.GONE
        itemBinding.ivEdit.setImageResource(item.editIconRes())
        bindFieldTreeList(itemBinding, item)
    }

    private fun AttributeArrayQueryData.displayTitle(): String {
        return attributeGroupName?.takeIf { it.isNotBlank() }
            ?: fieldTree?.firstOrNull()?.fieldName?.takeIf { it.isNotBlank() }
            ?: attributeGroupCode.orEmpty()
    }

    private fun mockResumeInfoData(): List<AttributeArrayQueryData> {
        return listOf(
            mockResumeInfoItem(
                GROUP_SELF_EVALUATION,
                "自我评价",
                mutableListOf(
                    AttributeFieldTree(
                        displayValue = "具备产品体验设计和移动端页面落地经验，关注细节与协作效率。"
                    )
                )
            ),
            mockResumeInfoItem(
                GROUP_WORK_EXPERIENCE,
                "工作经历",
                mockTree(
                    titleCode = FIELD_COMPANY_NAME,
                    titleValue = "深圳市乘海科技有限公司",
                    subtitleCode = FIELD_POSITION,
                    subtitleValue = "UI设计师",
                    descCode = FIELD_WORK_CONTENT,
                    descValue = "负责产品体验设计、视觉规范维护与核心页面改版。",
                    statusCode = INCOMPLETE
                )
            ),
            mockResumeInfoItem(
                GROUP_PROJECT_EXPERIENCE,
                "项目经历",
                mockTree(
                    titleCode = FIELD_PROJECT_NAME,
                    titleValue = "快龙虾在线简历",
                    subtitleCode = FIELD_POSITION,
                    subtitleValue = "产品设计",
                    descCode = FIELD_RESPONSIBILITIES,
                    descValue = "搭建简历信息结构，优化编辑与补充路径。"
                )
            ),
            mockResumeInfoItem(
                GROUP_EDUCATION_INFO,
                "教育经历",
                mockTree(
                    titleCode = FIELD_SCHOOL_NAME,
                    titleValue = "北京大学",
                    subtitleCode = FIELD_MAJOR,
                    subtitleValue = "计算机科学"
                )
            ),
            mockResumeInfoItem(
                GROUP_SKILLS,
                "技能",
                mockTree(FIELD_SKILL_NAME, "Figma / Android XML / Kotlin")
            ),
            mockResumeInfoItem(
                GROUP_AWARDS,
                "奖项",
                mockTree(FIELD_AWARD_NAME, "优秀项目奖")
            ),
            mockResumeInfoItem(
                GROUP_CERTIFICATES,
                "证书",
                mockTree(FIELD_CERTIFICATE_NAME, "产品设计认证")
            )
        )
    }

    private fun mockResumeInfoItem(
        code: String,
        title: String,
        fieldTree: MutableList<AttributeFieldTree>? = null
    ): AttributeArrayQueryData {
        return AttributeArrayQueryData(
            attributeGroupCode = code,
            attributeGroupName = title,
            fieldTree = fieldTree
        )
    }

    private fun mockTree(
        titleCode: String,
        titleValue: String,
        subtitleCode: String? = null,
        subtitleValue: String? = null,
        descCode: String? = null,
        descValue: String? = null,
        statusCode: String? = null
    ): MutableList<AttributeFieldTree> {
        val fieldItems = mutableListOf(
            AttributeFieldItem(fieldCode = titleCode, displayValue = titleValue),
            AttributeFieldItem(fieldCode = FIELD_START_DATE, displayValue = "2021.09"),
            AttributeFieldItem(fieldCode = FIELD_END_DATE, displayValue = "2024.05")
        )
        if (subtitleCode != null) {
            fieldItems.add(AttributeFieldItem(fieldCode = subtitleCode, displayValue = subtitleValue))
        }
        if (descCode != null) {
            fieldItems.add(AttributeFieldItem(fieldCode = descCode, displayValue = descValue))
        }
        return mutableListOf(
            AttributeFieldTree(
                statusCode = statusCode,
                fieldItems = fieldItems
            )
        )
    }

    private fun handleEditIconClick(item: AttributeArrayQueryData) {
        if (item.isSelfEvaluation()) {
            PublicDescriptionActivity.start(
                requireActivity(),
                true,
                item.selfEvaluationFieldItem(),
                item.displayTitle()
            )
            return
        }

        item.insertEmptyFieldTreeAtStart()
        when {
            item.isWorkExperience() -> WorkActivity.start(
                requireActivity(),
                false,
                item,
                0,
                item.clientRequestId
            )
            item.isProjectExperience() -> ProjectActivity.start(
                requireActivity(),
                false,
                item,
                0,
                item.clientRequestId
            )
            item.isEducationExperience() -> EducationalActivity.start(
                requireActivity(),
                false,
                item,
                0,
                item.clientRequestId
            )
            item.isSkill() -> SkillActivity.start(
                requireActivity(),
                false,
                item,
                0,
                item.clientRequestId
            )
            item.isAward() -> AwardActivity.start(
                requireActivity(),
                false,
                item,
                0,
                item.clientRequestId
            )
            item.isCertificate() -> CertificateActivity.start(
                requireActivity(),
                false,
                item,
                0,
                item.clientRequestId
            )
        }
    }

    private fun AttributeArrayQueryData.insertEmptyFieldTreeAtStart() {
        val treeList = fieldTree ?: return
        val original = treeList.getOrNull(0) ?: return
        val copy = original.copy(
            fieldItems = original.fieldItems?.map { item ->
                item.copy(
                    fieldValue = null,
                    displayValue = null
                )
            }?.toMutableList()
        )
        treeList.add(0, copy)
    }

    private fun AttributeArrayQueryData.selfEvaluationFieldItem(): AttributeFieldItem {
        findFirstFieldItem(fieldTree?.firstOrNull()?.fieldItems)?.let { return it }
        val text = selfEvaluationText()
        val fieldValue = text.takeIf { it.isNotBlank() }?.let {
            mapOf(
                "value" to it,
                "displayText" to it
            )
        }
        return AttributeFieldItem(
            fieldCode = fieldTree?.firstOrNull()?.fieldCode ?: GROUP_SELF_EVALUATION,
            fieldName = fieldTree?.firstOrNull()?.fieldName ?: displayTitle(),
            fieldValue = fieldValue,
            displayValue = text
        )
    }

    private fun findFirstFieldItem(items: List<AttributeFieldItem>?): AttributeFieldItem? {
        items.orEmpty().forEach { item ->
            if (!item.displayValue.isNullOrBlank() || item.fieldValue != null || item.fieldItems.isNullOrEmpty()) {
                return item
            }
            findFirstFieldItem(item.fieldItems)?.let { return it }
        }
        return null
    }

    private fun AttributeArrayQueryData.editIconRes(): Int {
        return if (attributeGroupCode == GROUP_SELF_EVALUATION) {
            R.drawable.ic_online_resume_profile_edit
        } else {
            R.drawable.ic_online_resume_add
        }
    }

    private fun AttributeArrayQueryData.selfEvaluationText(): String {
        if (attributeGroupCode != GROUP_SELF_EVALUATION) return ""
        displayValue?.takeIf { it.isNotBlank() }?.let { return it }
        value?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        fieldTree.orEmpty().forEach { tree ->
            tree.displayValue?.takeIf { it.isNotBlank() }?.let { return it }
            tree.fieldValue?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            findFirstDisplayText(tree.fieldItems)?.let { return it }
        }
        return ""
    }

    private fun findFirstDisplayText(items: List<AttributeFieldItem>?): String? {
        items.orEmpty().forEach { item ->
            item.displayValue?.takeIf { it.isNotBlank() }?.let { return it }
            item.fieldValue?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            findFirstDisplayText(item.fieldItems)?.let { return it }
        }
        return null
    }

    private fun bindFieldTreeList(
        itemBinding: ItemResumeInfoBinding,
        attributeData: AttributeArrayQueryData
    ) {
        val fieldTree = attributeData.fieldTree
        val adapter = createFieldTreeAdapter(attributeData)
        if (fieldTree.isNullOrEmpty() || adapter == null) {
            itemBinding.rcFieldTree.visibility = View.GONE
            itemBinding.rcFieldTree.adapter = null
            return
        }
        itemBinding.rcFieldTree.visibility = View.VISIBLE
        itemBinding.rcFieldTree.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        itemBinding.rcFieldTree.isNestedScrollingEnabled = false
        if (itemBinding.rcFieldTree.itemDecorationCount == 0) {
            itemBinding.rcFieldTree.addItemDecoration(SpaceDecoration(12.dp))
        }
        itemBinding.rcFieldTree.adapter = adapter
    }

    private fun createFieldTreeAdapter(
        attributeData: AttributeArrayQueryData
    ): RecyclerView.Adapter<*>? {
        val fieldTree = attributeData.fieldTree.orEmpty()
        return when {
            attributeData.isWorkExperience() -> createWorkExperienceAdapter(attributeData, fieldTree)
            attributeData.isProjectExperience() -> createProjectExperienceAdapter(attributeData, fieldTree)
            attributeData.isEducationExperience() -> createEducationExperienceAdapter(attributeData, fieldTree)
            attributeData.isSkill() -> createCertificateSkillsAwardsAdapter(
                attributeData,
                fieldTree,
                GROUP_SKILLS
            )
            attributeData.isAward() -> createCertificateSkillsAwardsAdapter(
                attributeData,
                fieldTree,
                GROUP_AWARDS
            )
            attributeData.isCertificate() -> createCertificateSkillsAwardsAdapter(
                attributeData,
                fieldTree,
                GROUP_CERTIFICATES
            )
            else -> null
        }
    }

    private fun createWorkExperienceAdapter(
        attributeData: AttributeArrayQueryData,
        fieldTree: List<AttributeFieldTree>
    ): SingleBindingAdapter<ItemWorkExperienceReBinding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_work_experience_re,
            fieldTree
        ) { binding, position, item ->
            val companyName = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_COMPANY_NAME)
            binding.tvCompany.text = companyName?.displayValue ?: "暂无"
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE

            val startDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_START_DATE)
            val endDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_END_DATE)
            val positionItem = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_POSITION)
            val workContent = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_WORK_CONTENT)
            binding.tvTime.text =
                (startDate?.displayValue ?: "") + " - " + (endDate?.displayValue ?: "")
            binding.tvJob.text = positionItem?.displayValue ?: ""
            binding.tvDesc.text = workContent?.displayValue?.let { "内容:$it" }.orEmpty()

            bindWorkLikeVisibility(binding.tvTime.text.toString(), binding.tvJob.text.toString(), binding.v)
            binding.tvTime.visibility = if (binding.tvTime.text == " - ") View.GONE else View.VISIBLE
            binding.tvJob.visibility = if (binding.tvJob.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.tvDesc.visibility =
                if (workContent?.displayValue.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.root.setOnClickListener {
                WorkActivity.start(requireActivity(), true, attributeData, position, attributeData.clientRequestId)
            }
        }
    }

    private fun createProjectExperienceAdapter(
        attributeData: AttributeArrayQueryData,
        fieldTree: List<AttributeFieldTree>
    ): SingleBindingAdapter<ItemProjectExperienceReBinding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_project_experience_re,
            fieldTree
        ) { binding, position, item ->
            val projectName = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_PROJECT_NAME)
            binding.tvCompany.text = projectName?.displayValue ?: "暂无"
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE

            val startDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_START_DATE)
            val endDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_END_DATE)
            val positionItem = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_POSITION)
            val workContent = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_RESPONSIBILITIES)
            binding.tvTime.text =
                (startDate?.displayValue ?: "") + " - " + (endDate?.displayValue ?: "")
            binding.tvJob.text = positionItem?.displayValue ?: ""
            binding.tvDesc.text = workContent?.displayValue?.let { "内容:$it" }.orEmpty()

            bindWorkLikeVisibility(binding.tvTime.text.toString(), binding.tvJob.text.toString(), binding.v)
            binding.tvTime.visibility = if (binding.tvTime.text == " - ") View.GONE else View.VISIBLE
            binding.tvJob.visibility = if (binding.tvJob.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.tvDesc.visibility =
                if (workContent?.displayValue.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.root.setOnClickListener {
                ProjectActivity.start(requireActivity(), true, attributeData, position, attributeData.clientRequestId)
            }
        }
    }

    private fun createEducationExperienceAdapter(
        attributeData: AttributeArrayQueryData,
        fieldTree: List<AttributeFieldTree>
    ): SingleBindingAdapter<ItemEducationExperienceReBinding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_education_experience_re,
            fieldTree
        ) { binding, position, item ->
            val schoolName = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_SCHOOL_NAME)
            binding.tvCompany.text = schoolName?.displayValue ?: "暂无"
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE

            val startDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_START_DATE)
            val endDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_END_DATE)
            val major = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_MAJOR)
            binding.tvTime.text =
                (startDate?.displayValue ?: "") + " - " + (endDate?.displayValue ?: "")
            binding.tvMajor.text = major?.displayValue ?: ""

            val hasTime = binding.tvTime.text != " - "
            val hasMajor = !binding.tvMajor.text.isNullOrEmpty()
            binding.tvTime.visibility = if (hasTime) View.VISIBLE else View.GONE
            binding.tvMajor.visibility = if (hasMajor) View.VISIBLE else View.GONE
            binding.v.visibility = if (hasTime && hasMajor) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                EducationalActivity.start(requireActivity(), true, attributeData, position, attributeData.clientRequestId)
            }
        }
    }

    private fun createCertificateSkillsAwardsAdapter(
        attributeData: AttributeArrayQueryData,
        fieldTree: List<AttributeFieldTree>,
        type: String
    ): SingleBindingAdapter<ItemCertificateSkillsAwardsReBinding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_certificate_skills_awards_re,
            fieldTree
        ) { binding, position, item ->
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE
            val fieldCode = when (type) {
                GROUP_SKILLS -> FIELD_SKILL_NAME
                GROUP_CERTIFICATES -> FIELD_CERTIFICATE_NAME
                else -> FIELD_AWARD_NAME
            }
            val name = AttributeFieldItemFinder.findFirst(item.fieldItems, fieldCode)
            binding.tvName.text = name?.displayValue ?: "暂无"
            binding.root.setOnClickListener {
                when (type) {
                    GROUP_SKILLS -> SkillActivity.start(
                        requireActivity(),
                        true,
                        attributeData,
                        position,
                        attributeData.clientRequestId
                    )
                    GROUP_CERTIFICATES -> CertificateActivity.start(
                        requireActivity(),
                        true,
                        attributeData,
                        position,
                        attributeData.clientRequestId
                    )
                    else -> AwardActivity.start(
                        requireActivity(),
                        true,
                        attributeData,
                        position,
                        attributeData.clientRequestId
                    )
                }
            }
        }
    }

    private fun bindWorkLikeVisibility(time: String, job: String, divider: View) {
        divider.visibility =
            if (time != " - " && job.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun AttributeArrayQueryData.isWorkExperience(): Boolean =
        attributeGroupCode == GROUP_WORK_EXPERIENCE

    private fun AttributeArrayQueryData.isSelfEvaluation(): Boolean =
        attributeGroupCode == GROUP_SELF_EVALUATION

    private fun AttributeArrayQueryData.isProjectExperience(): Boolean =
        attributeGroupCode == GROUP_PROJECT_EXPERIENCE

    private fun AttributeArrayQueryData.isEducationExperience(): Boolean =
        attributeGroupCode == GROUP_EDUCATION_INFO || attributeGroupCode == GROUP_EDUCATION_EXPERIENCE

    private fun AttributeArrayQueryData.isSkill(): Boolean =
        attributeGroupCode == GROUP_SKILLS || attributeGroupCode == GROUP_SKILL

    private fun AttributeArrayQueryData.isAward(): Boolean =
        attributeGroupCode == GROUP_AWARDS || attributeGroupCode == GROUP_AWARD

    private fun AttributeArrayQueryData.isCertificate(): Boolean =
        attributeGroupCode == GROUP_CERTIFICATES || attributeGroupCode == GROUP_CERTIFICATE

}
