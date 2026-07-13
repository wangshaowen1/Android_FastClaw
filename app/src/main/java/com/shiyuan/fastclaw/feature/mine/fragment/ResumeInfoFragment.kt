package com.shiyuan.fastclaw.feature.mine.fragment

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.common.ext.load
import com.shiyuan.fastclaw.core.Events
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
import com.shiyuan.fastclaw.feature.information.PersonaDescriptionActivity
import com.shiyuan.fastclaw.feature.information.PersonalInformationActivity
import com.shiyuan.fastclaw.feature.information.ProjectActivity
import com.shiyuan.fastclaw.feature.information.PublicDescriptionActivity
import com.shiyuan.fastclaw.feature.information.SkillActivity
import com.shiyuan.fastclaw.feature.information.WorkActivity
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import com.shiyuan.fastclaw.feature.login.flash.PhoneLoginDialogActivity.Companion.PHONE
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import java.util.UUID

class ResumeInfoFragment : BaseViewModelFragment<FragmentResumeInfoBinding, InformationViewModel>(true) {

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
        private const val FIELD_AVATAR_URL = "avatarUrl"
        private const val FIELD_NAME = "name"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_AGE = "age"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_WECHAT_ID = "wechatId"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_POSITION = "position"
        private const val FIELD_COMPANY_NAME = "companyName"
        private const val FIELD_WORK_CONTENT = "workContent"
        private const val FIELD_RESPONSIBILITIES = "responsibilities"
        private const val FIELD_PROJECT_NAME = "projectName"
        private const val FIELD_SCHOOL_NAME = "schoolName"
        private const val FIELD_MAJOR = "major"
        private const val FIELD_DEGREE = "degree"
        private const val FIELD_SKILL_NAME = "skillName"
        private const val FIELD_CERTIFICATE_NAME = "certificateName"
        private const val FIELD_AWARD_NAME = "awardName"

        private const val FIELD_PROJECT_ROLE = "projectRole"

        private const val FIELD_PROFICIENCY = "proficiency"
        private const val COLLAPSED_ITEM_COUNT = 3
    }

    private val expandedGroupCodes = mutableSetOf<String>()

    private val resumeInfoAdapter by lazy {
        SingleBindingAdapter<ItemResumeInfoBinding, AttributeArrayQueryData>(
            R.layout.item_resume_info,
            emptyList(),
            onBind = { itemBinding, position, item ->
                bindResumeInfoItem(itemBinding, position, item)
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
        binding.layoutProfile.setOnSingleClickListener {
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

        observeEvents {
            on<Events.CommonAttributeArrayQueryData> { event ->
               mRealVM.queryOnlineResumeInfo()
            }
        }
    }

    private fun updateBaseResumeInfo(data: List<AttributeArrayQueryData>) {
        val avatarUrl = data.findBaseFieldValue(FIELD_AVATAR_URL)
        if (avatarUrl.isBlank()) {
            binding.ivAvatar.setImageResource(R.drawable.ic_home_header_avatar)
        } else {
            binding.ivAvatar.load(avatarUrl, R.drawable.ic_home_header_avatar)
        }

        binding.tvName.text = data.findBaseFieldValue(FIELD_NAME).ifBlank { "姓名" }

        val ageValue = data.findBaseFieldValue(FIELD_AGE).trim().ifBlank { "0" }
        val ageText = if (ageValue.endsWith("岁")) {
            ageValue
        } else {
            getString(R.string.resume_age_with_unit, ageValue)
        }
        val genderText = data.findBaseFieldValue(FIELD_GENDER)
            .trim()
            .ifBlank { getString(R.string.unknown) }
        binding.tvTip.text = listOf(ageText, genderText).joinToString(" | ")
        binding.tvTip.visibility = View.VISIBLE

        bindBaseContact(
            binding.layoutProfilePhone,
            binding.tvProfilePhone,
            data.findBaseFieldValue(FIELD_PHONE)
        )
        bindBaseContact(
            binding.layoutProfileEmail,
            binding.tvProfileEmail,
            data.findBaseFieldValue(FIELD_EMAIL)
        )
        bindBaseContact(
            binding.layoutProfileWechat,
            binding.tvProfileWechat,
            data.findBaseFieldValue(FIELD_WECHAT_ID)
        )

        binding.layoutProfileContacts.visibility = View.VISIBLE
    }

    private fun bindBaseContact(layout: View, textView: TextView, value: String) {
        layout.visibility = View.VISIBLE
        textView.text = value.ifBlank { getString(R.string.unknown) }
    }

    private fun updateResumeInfo(data: List<AttributeArrayQueryData>) {
        expandedGroupCodes.clear()
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.ll.visibility = if (showEmpty) View.GONE else View.VISIBLE
        resumeInfoAdapter.submitList(data)
    }

    private fun bindResumeInfoItem(
        itemBinding: ItemResumeInfoBinding,
        position: Int,
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

        //展示待补充
        val showMissing = item.passable == false
        itemBinding.layoutMissing.visibility = if (showMissing) View.VISIBLE else View.GONE
        itemBinding.ivEdit.setImageResource(item.editIconRes())

        //是否展示列表
        val hidePlaceholderList = item.fieldTree?.singleOrNull()?.instanceIndex?.let { it == -1L } ?: (item.fieldTree?.size == 1)
        if (hidePlaceholderList) {
            itemBinding.rcFieldTree.visibility = View.GONE
            //隐藏展开收起
            itemBinding.layoutExpand.visibility = View.GONE
            itemBinding.layoutExpand.setOnClickListener(null)
        } else {
            bindFieldTreeList(itemBinding, position, item)
        }
    }

    private fun AttributeArrayQueryData.displayTitle(): String {
        return attributeGroupName?.takeIf { it.isNotBlank() }
            ?: fieldTree?.firstOrNull()?.fieldName?.takeIf { it.isNotBlank() }
            ?: attributeGroupCode.orEmpty()
    }

    private fun List<AttributeArrayQueryData>.findBaseFieldValue(code: String): String {
        return firstOrNull { it.attributeGroupCode == code || it.metaCode == code }
            ?.baseDisplayText()
            .orEmpty()
    }

    private fun AttributeArrayQueryData.baseDisplayText(): String {
        displayValue?.takeIf { it.isNotBlank() }?.let { return it }
        val rawValue = value ?: return ""
        if (rawValue is Map<*, *>) {
            rawValue["displayText"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            rawValue["name"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            rawValue["value"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return rawValue.toString()
    }


    private fun handleEditIconClick(item: AttributeArrayQueryData) {
        if (item.isSelfEvaluation()) {
            PersonaDescriptionActivity.start(
                requireActivity(),
                true,
                item,
                "介绍个人又是，突出你的亮点"
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
                UUID.randomUUID().toString(),
                type = WorkActivity.TYPE_ONLINE_RESUME
            )
            item.isProjectExperience() -> ProjectActivity.start(
                requireActivity(),
                false,
                item,
                0,
                UUID.randomUUID().toString(),
                type = WorkActivity.TYPE_ONLINE_RESUME
            )
            item.isEducationExperience() -> EducationalActivity.start(
                requireActivity(),
                false,
                item,
                0,
                UUID.randomUUID().toString(),
                type = WorkActivity.TYPE_ONLINE_RESUME
            )
            item.isSkill() -> SkillActivity.start(
                requireActivity(),
                false,
                item,
                0,
                UUID.randomUUID().toString(),
                type = WorkActivity.TYPE_ONLINE_RESUME
            )
            item.isAward() -> AwardActivity.start(
                requireActivity(),
                false,
                item,
                0,
                UUID.randomUUID().toString(),
                type = WorkActivity.TYPE_ONLINE_RESUME
            )
            item.isCertificate() -> CertificateActivity.start(
                requireActivity(),
                false,
                item,
                0,
                UUID.randomUUID().toString(),
                type = WorkActivity.TYPE_ONLINE_RESUME
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
        position: Int,
        attributeData: AttributeArrayQueryData
    ) {
        val fieldTree = attributeData.fieldTree
        //技能 证书和奖项 判断是显示展示隐藏
        val canExpand = attributeData.isCollapsibleGroup() && fieldTree.orEmpty().size > COLLAPSED_ITEM_COUNT
        val groupCode = attributeData.attributeGroupCode.orEmpty()
        val isExpanded = canExpand && groupCode in expandedGroupCodes
        val displayFieldTree = if (canExpand && !isExpanded) {
            fieldTree.orEmpty().take(COLLAPSED_ITEM_COUNT)
        } else {
            fieldTree.orEmpty()
        }
        val adapter = createFieldTreeAdapter(attributeData, displayFieldTree)
        if (fieldTree.isNullOrEmpty() || adapter == null) {
            itemBinding.rcFieldTree.visibility = View.GONE
            itemBinding.rcFieldTree.adapter = null
            itemBinding.layoutExpand.visibility = View.GONE
            itemBinding.layoutExpand.setOnClickListener(null)
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

        itemBinding.layoutExpand.visibility = if (canExpand) View.VISIBLE else View.GONE
        if (canExpand) {
            itemBinding.tvExpand.text = getString(
                if (isExpanded) R.string.collapse_all else R.string.expand_all
            )
            itemBinding.ivExpand.rotation = if (isExpanded) 0f else 180f
            itemBinding.layoutExpand.setOnSingleClickListener {
                if (isExpanded) {
                    expandedGroupCodes.remove(groupCode)
                } else {
                    expandedGroupCodes.add(groupCode)
                }
                resumeInfoAdapter.notifyItemChanged(position)
            }
        } else {
            itemBinding.layoutExpand.setOnClickListener(null)
        }
    }

    private fun createFieldTreeAdapter(
        attributeData: AttributeArrayQueryData,
        fieldTree: List<AttributeFieldTree>
    ): RecyclerView.Adapter<*>? {
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
            binding.tvCompany.text = companyName?.displayValue ?: ""
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
                if(item.instanceIndex == -1L || item.instanceIndex == null){
                    handleEditIconClick(attributeData)
                    return@setOnClickListener
                }
                WorkActivity.start(
                    requireActivity(),
                    true,
                    attributeData,
                    position,
                    UUID.randomUUID().toString(),
                    type = WorkActivity.TYPE_ONLINE_RESUME
                )
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
            binding.tvCompany.text = projectName?.displayValue ?: ""
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE

            val startDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_START_DATE)
            val endDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_END_DATE)
            val projectRoleItem = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_PROJECT_ROLE)
            val workContent = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_RESPONSIBILITIES)
            binding.tvTime.text =
                (startDate?.displayValue ?: "") + " - " + (endDate?.displayValue ?: "")
            binding.tvJob.text = projectRoleItem?.displayValue ?: ""
            binding.tvDesc.text = workContent?.displayValue?.let { "内容:$it" }.orEmpty()

            bindWorkLikeVisibility(binding.tvTime.text.toString(), binding.tvJob.text.toString(), binding.v)
            binding.tvTime.visibility = if (binding.tvTime.text == " - ") View.GONE else View.VISIBLE
            binding.tvJob.visibility = if (binding.tvJob.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.tvDesc.visibility =
                if (workContent?.displayValue.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.root.setOnClickListener {
                if(item.instanceIndex == -1L || item.instanceIndex == null){
                    handleEditIconClick(attributeData)
                    return@setOnClickListener
                }
                ProjectActivity.start(
                    requireActivity(),
                    true,
                    attributeData,
                    position,
                    UUID.randomUUID().toString(),
                    type = WorkActivity.TYPE_ONLINE_RESUME
                )
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
            binding.tvCompany.text = schoolName?.displayValue ?: ""
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE

            val startDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_START_DATE)
            val endDate = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_END_DATE)
            val major = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_MAJOR)
            val degree = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_DEGREE)
            binding.tvTime.text = (startDate?.displayValue ?: "") + " - " + (endDate?.displayValue ?: "")

            val majorText = listOfNotNull(
                degree?.displayValue?.takeIf { it.isNotBlank() },
                major?.displayValue?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            binding.tvMajor.text = majorText

            val hasTime = binding.tvTime.text != " - "
            val hasMajor = majorText.isNotBlank()
            binding.tvTime.visibility = if (hasTime) View.VISIBLE else View.GONE
            binding.tvMajor.visibility = if (hasMajor) View.VISIBLE else View.GONE
            binding.v.visibility = if (hasTime && hasMajor) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                if(item.instanceIndex == -1L || item.instanceIndex == null){
                    handleEditIconClick(attributeData)
                    return@setOnClickListener
                }
                EducationalActivity.start(
                    requireActivity(),
                    true,
                    attributeData,
                    position,
                    UUID.randomUUID().toString(),
                    type = WorkActivity.TYPE_ONLINE_RESUME
                )
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
            binding.tvName.text = name?.displayValue ?: ""
            if(type == GROUP_SKILLS){
                val tvValeName = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_PROFICIENCY)
                tvValeName?.displayValue?.let {
                    binding.tvVale.visibility = View.VISIBLE
                    binding.tvVale.text = it
                } ?:run {
                    binding.tvVale.visibility = View.GONE
                }
            }else{
                binding.tvVale.visibility = View.GONE
            }
            binding.root.setOnClickListener {
                if(item.instanceIndex == -1L || item.instanceIndex == null){
                    handleEditIconClick(attributeData)
                    return@setOnClickListener
                }
                when (type) {
                    GROUP_SKILLS -> SkillActivity.start(
                        requireActivity(),
                        true,
                        attributeData,
                        position,
                        UUID.randomUUID().toString(),
                        type = WorkActivity.TYPE_ONLINE_RESUME
                    )
                    GROUP_CERTIFICATES -> CertificateActivity.start(
                        requireActivity(),
                        true,
                        attributeData,
                        position,
                        UUID.randomUUID().toString(),
                        type = WorkActivity.TYPE_ONLINE_RESUME
                    )
                    else -> AwardActivity.start(
                        requireActivity(),
                        true,
                        attributeData,
                        position,
                        UUID.randomUUID().toString(),
                        type = WorkActivity.TYPE_ONLINE_RESUME
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

    private fun AttributeArrayQueryData.isCollapsibleGroup(): Boolean =
        isSkill() || isAward() || isCertificate()

}
