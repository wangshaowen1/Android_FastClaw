package com.shiyuan.fastclaw.feature.resume

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
import com.shiyuan.fastclaw.feature.resume.experience.AwardActivity
import com.shiyuan.fastclaw.feature.resume.experience.CertificateActivity
import com.shiyuan.fastclaw.feature.resume.experience.EducationalActivity
import com.shiyuan.fastclaw.feature.resume.profile.PersonaDescriptionActivity
import com.shiyuan.fastclaw.feature.resume.profile.PersonalInformationActivity
import com.shiyuan.fastclaw.feature.resume.experience.ProjectActivity
import com.shiyuan.fastclaw.feature.resume.profile.PublicDescriptionActivity
import com.shiyuan.fastclaw.feature.resume.experience.SkillActivity
import com.shiyuan.fastclaw.feature.resume.experience.WorkActivity
import com.shiyuan.fastclaw.feature.login.flash.PhoneLoginDialogActivity.Companion.PHONE
import com.shiyuan.fastclaw.feature.shrimp.UnderstandsViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import java.util.UUID

/**
 * 在线简历信息页。
 *
 * 负责展示基础资料及各类简历经历，并根据服务端返回的属性分组跳转到对应编辑页面。
 */
class ResumeInfoFragment : BaseViewModelFragment<FragmentResumeInfoBinding, InformationViewModel>(true) {

    companion object {
        fun newInstance(): ResumeInfoFragment = ResumeInfoFragment()

        // 服务端用该状态标识当前经历字段仍未填写完整。
        const val INCOMPLETE = "INCOMPLETE"

        // 简历属性分组编码，用于识别分组类型并选择对应的展示布局和编辑页面。
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

        // 字段元数据编码，用于从服务端下发的动态字段树中查找指定业务字段。
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
        private const val FIELD_JOBIDENTITY = "jobIdentity"
        private const val FIELD_INTERNSHIP = "internship"

        // 可折叠分组默认最多展示三条，剩余内容由用户手动展开。
        private const val COLLAPSED_ITEM_COUNT = 3
    }

    // 记录当前已展开的分组；列表数据刷新后会重置为默认折叠状态。
    private val expandedGroupCodes = mutableSetOf<String>()

    // 外层列表的每一项代表一个简历分组，具体子列表由 bindResumeInfoItem 再按类型绑定。
    private val resumeInfoAdapter by lazy {
        SingleBindingAdapter<ItemResumeInfoBinding, AttributeArrayQueryData>(
            R.layout.item_resume_info,
            emptyList(),
            onBind = { itemBinding, position, item ->
                bindResumeInfoItem(itemBinding, position, item)
            }
        )
    }

    /**
     * 完成页面初始化、数据监听，并发起首次在线简历查询。
     */
    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
        // 基础资料和分组简历数据由 ViewModel 内部统一查询并分别回传。
        mRealVM.queryOnlineResumeInfo()
    }

    /**
     * 初始化外层简历分组列表及基础资料编辑入口。
     */
    private fun initView() {
        binding.rvResumeInfo.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            // 外层页面负责滚动，关闭 RecyclerView 的嵌套滚动避免滑动冲突。
            isNestedScrollingEnabled = false
            adapter = resumeInfoAdapter
        }
        // 请求结果返回前先显示空态，避免残留旧列表内容。
        updateResumeInfo(emptyList())
        // 头像编辑按钮和整个基础资料区域均进入同一个个人信息编辑页。
        binding.ivProfileEdit.setOnSingleClickListener {
            PersonalInformationActivity.start(requireActivity())
        }
        binding.layoutProfile.setOnSingleClickListener {
            PersonalInformationActivity.start(requireActivity())
        }
    }

    /**
     * 监听基础资料、分组经历及跨页面保存事件。
     */
    private fun initObservers() {
        // 基础资料和分组经历分开回传，分别更新页面头部和外层列表。
        mRealVM.onlineResumeBaseInfo.observe(this) { data ->
            updateBaseResumeInfo(data.orEmpty())
        }
        mRealVM.onlineResumeInfo.observe(this) { data ->
            updateResumeInfo(data.orEmpty())
        }

        observeEvents {
            on<Events.CommonAttributeArrayQueryData> { event ->
               // 任一简历编辑页保存成功后，重新拉取整份在线简历以同步各分组状态。
               mRealVM.queryOnlineResumeInfo()
            }
        }
    }

    /**
     * 绑定头像、姓名、年龄、性别及联系方式等基础资料。
     *
     * 服务端可能返回空值，因此页面始终保留字段位置并展示约定的兜底内容。
     */
    private fun updateBaseResumeInfo(data: List<AttributeArrayQueryData>) {
        val avatarUrl = data.findBaseFieldValue(FIELD_AVATAR_URL)
        // 头像地址为空时直接使用默认头像；加载失败时同样由 load 的占位资源兜底。
        if (avatarUrl.isBlank()) {
            binding.ivAvatar.setImageResource(R.drawable.ic_home_header_avatar)
        } else {
            binding.ivAvatar.load(avatarUrl, R.drawable.ic_home_header_avatar)
        }

        binding.tvName.text = data.findBaseFieldValue(FIELD_NAME).ifBlank { "姓名" }

        // 年龄可能已经包含“岁”，仅对纯数值补单位，避免出现“20岁岁”。
        val ageValue = data.findBaseFieldValue(FIELD_AGE).trim().ifBlank { "0" }
        val ageText = if (ageValue.endsWith("岁")) {
            ageValue
        } else {
            getString(R.string.resume_age_with_unit, ageValue)
        }
        val genderText = data.findBaseFieldValue(FIELD_GENDER)
            .trim()
            .ifBlank { getString(R.string.unknown) }
        // 年龄和性别始终按“年龄 | 性别”展示，缺失值使用既定默认文案。
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

        // 联系方式即使为空也保留整块区域，由 bindBaseContact 显示“未知”。
        binding.layoutProfileContacts.visibility = View.VISIBLE

        // 仅学生身份展示身份标签，其他身份不占用标签位置。
        binding.rlTag.visibility =  if(data.findBaseFieldValue(FIELD_JOBIDENTITY) == "学生")  View.VISIBLE else   View.GONE
    }

    /**
     * 绑定单个联系方式。
     *
     * 不隐藏空字段，确保电话、邮箱、微信三行结构固定；空值统一显示“未知”。
     */
    private fun bindBaseContact(layout: View, textView: TextView, value: String) {
        layout.visibility = View.VISIBLE
        textView.text = value.ifBlank { getString(R.string.unknown) }
    }

    /**
     * 更新外层简历分组列表，并在无数据时切换为空态引导。
     */
    private fun updateResumeInfo(data: List<AttributeArrayQueryData>) {
        // 新数据可能已改变分组数量，清空旧的展开状态以免状态错配。
        expandedGroupCodes.clear()
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.ll.visibility = if (showEmpty) View.GONE else View.VISIBLE
        resumeInfoAdapter.submitList(data)
    }

    /**
     * 绑定一个简历分组，并根据分组内容决定展示自我评价、待补充状态或经历列表。
     */
    private fun bindResumeInfoItem(
        itemBinding: ItemResumeInfoBinding,
        position: Int,
        item: AttributeArrayQueryData
    ) {
        // 标题优先使用分组名称，缺失时再从字段树或分组编码中回退。
        itemBinding.tvTitle.text = item.displayTitle()
        // 自我评价使用编辑图标，其余经历分组使用新增图标，但入口统一在此处理。
        itemBinding.ivEdit.setOnSingleClickListener {
            handleEditIconClick(item)
        }
        // 只有自我评价分组直接在外层卡片展示文本，其他分组由内部 RecyclerView 展示。
        val selfEvaluationText = item.selfEvaluationText()
        val showSelfEvaluation = selfEvaluationText.isNotBlank()
        itemBinding.tvName.text = selfEvaluationText
        itemBinding.tvName.visibility = if (showSelfEvaluation) View.VISIBLE else View.GONE

        // passable 明确为 false 时提示“待补充”；null 不视为校验失败。
        val showMissing = item.passable == false
        itemBinding.layoutMissing.visibility = if (showMissing) View.VISIBLE else View.GONE
        itemBinding.ivEdit.setImageResource(item.editIconRes())

        // instanceIndex 为 -1 的唯一数据是服务端下发的新增占位项，不作为真实经历展示。
        val hidePlaceholderList = item.fieldTree?.singleOrNull()?.instanceIndex?.let { it == -1L } ?: (item.fieldTree?.size == 1)
        if (hidePlaceholderList) {
            itemBinding.rcFieldTree.visibility = View.GONE
            // 占位项没有可展示列表，同时清除复用 ViewHolder 可能遗留的展开点击事件。
            itemBinding.layoutExpand.visibility = View.GONE
            itemBinding.layoutExpand.setOnClickListener(null)
        } else {
            bindFieldTreeList(itemBinding, position, item)
        }
        // 最后一组不显示底部分隔线，避免列表末尾出现多余横线。
        itemBinding.viewDivider.visibility = if (position == resumeInfoAdapter.itemCount - 1) View.GONE else View.VISIBLE
    }

    /**
     * 按“分组显示名称 -> 首个字段名称 -> 分组编码”的优先级生成标题。
     */
    private fun AttributeArrayQueryData.displayTitle(): String {
        return attributeGroupName?.takeIf { it.isNotBlank() }
            ?: fieldTree?.firstOrNull()?.fieldName?.takeIf { it.isNotBlank() }
            ?: attributeGroupCode.orEmpty()
    }

    /**
     * 同时兼容 attributeGroupCode 和 metaCode 两种基础字段标识方式。
     */
    private fun List<AttributeArrayQueryData>.findBaseFieldValue(code: String): String {
        return firstOrNull { it.attributeGroupCode == code || it.metaCode == code }
            ?.baseDisplayText()
            .orEmpty()
    }

    /**
     * 兼容基础字段的多种返回结构，优先使用可直接展示的文本。
     */
    private fun AttributeArrayQueryData.baseDisplayText(): String {
        // displayValue 是服务端已格式化的展示文本，存在时优先直接使用。
        displayValue?.takeIf { it.isNotBlank() }?.let { return it }
        val rawValue = value ?: return ""
        if (rawValue is Map<*, *>) {
            // 枚举或对象字段可能包装在 Map 中，按常见展示键逐级回退。
            rawValue["displayText"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            rawValue["name"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            rawValue["value"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return rawValue.toString()
    }


    /**
     * 处理分组右侧的编辑或新增入口，并按分组类型跳转到对应表单。
     */
    private fun handleEditIconClick(item: AttributeArrayQueryData) {
        // 自我评价只有单条内容，直接以编辑模式进入描述页面，不创建空字段树。
        if (item.isSelfEvaluation()) {
            PersonaDescriptionActivity.start(
                requireActivity(),
                true,
                item,
                "介绍个人优势，突出你的亮点"
            )
            return
        }

        // 新增经历时在首位插入空模板，目标表单固定读取第 0 项作为编辑数据。
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

    /**
     * 复制首个字段树作为新增模板，并清空其中的字段值，避免修改服务端原始模板。
     */
    private fun AttributeArrayQueryData.insertEmptyFieldTreeAtStart() {
        val treeList = fieldTree ?: return
        val original = treeList.getOrNull(0) ?: return
        // 保留字段结构和元数据，仅清空业务值，让新增表单仍能按原字段定义渲染。
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

    /**
     * 深度优先查找第一个可用于展示的叶子字段或已有值字段。
     */
    private fun findFirstFieldItem(items: List<AttributeFieldItem>?): AttributeFieldItem? {
        items.orEmpty().forEach { item ->
            // 有展示值、有原始值或已经到叶子节点时，即认为找到了可用字段。
            if (!item.displayValue.isNullOrBlank() || item.fieldValue != null || item.fieldItems.isNullOrEmpty()) {
                return item
            }
            findFirstFieldItem(item.fieldItems)?.let { return it }
        }
        return null
    }

    /**
     * 自我评价只有编辑语义，其他可重复经历使用新增语义图标。
     */
    private fun AttributeArrayQueryData.editIconRes(): Int {
        return if (attributeGroupCode == GROUP_SELF_EVALUATION) {
            R.drawable.ic_online_resume_profile_edit
        } else {
            R.drawable.ic_online_resume_add
        }
    }

    /**
     * 从自我评价分组的不同返回层级中提取首个可展示文本。
     */
    private fun AttributeArrayQueryData.selfEvaluationText(): String {
        if (attributeGroupCode != GROUP_SELF_EVALUATION) return ""
        // 兼容文本直接挂在分组、字段树节点或更深层字段项上的多种接口结构。
        displayValue?.takeIf { it.isNotBlank() }?.let { return it }
        value?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        fieldTree.orEmpty().forEach { tree ->
            tree.displayValue?.takeIf { it.isNotBlank() }?.let { return it }
            tree.fieldValue?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            findFirstDisplayText(tree.fieldItems)?.let { return it }
        }
        return ""
    }

    /**
     * 递归查找嵌套字段中的首个展示值，兼容 displayValue 和原始 fieldValue。
     */
    private fun findFirstDisplayText(items: List<AttributeFieldItem>?): String? {
        items.orEmpty().forEach { item ->
            item.displayValue?.takeIf { it.isNotBlank() }?.let { return it }
            item.fieldValue?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            findFirstDisplayText(item.fieldItems)?.let { return it }
        }
        return null
    }

    /**
     * 绑定分组下的经历列表，并维护技能、证书、奖项分组的展开收起状态。
     */
    private fun bindFieldTreeList(
        itemBinding: ItemResumeInfoBinding,
        position: Int,
        attributeData: AttributeArrayQueryData
    ) {
        val fieldTree = attributeData.fieldTree
        // 技能、证书和奖项超过三项时默认折叠，其余经历始终完整展示。
        val canExpand = attributeData.isCollapsibleGroup() && fieldTree.orEmpty().size > COLLAPSED_ITEM_COUNT
        val groupCode = attributeData.attributeGroupCode.orEmpty()
        val isExpanded = canExpand && groupCode in expandedGroupCodes
        // 折叠时仅把前三条交给子适配器；展开时提交完整字段树。
        val displayFieldTree = if (canExpand && !isExpanded) {
            fieldTree.orEmpty().take(COLLAPSED_ITEM_COUNT)
        } else {
            fieldTree.orEmpty()
        }
        val adapter = createFieldTreeAdapter(attributeData, displayFieldTree)
        // 数据为空或分组类型不受支持时，主动清理复用 ViewHolder 中的旧适配器和点击事件。
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
        // ViewHolder 复用时避免重复添加间距装饰，防止条目间距不断累加。
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
                // 只刷新当前外层分组，由重新绑定逻辑根据集合状态重建子列表。
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

    /**
     * 根据属性分组选择对应的经历布局适配器；未知分组不渲染子列表。
     */
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

    /**
     * 创建工作经历子列表。
     *
     * 每项展示公司、任职时间、职位、工作内容及实习标签；点击真实经历进入编辑，
     * 点击服务端占位项则复用分组新增入口。
     */
    private fun createWorkExperienceAdapter(
        attributeData: AttributeArrayQueryData,
        fieldTree: List<AttributeFieldTree>
    ): SingleBindingAdapter<ItemWorkExperienceReBinding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_work_experience_re,
            fieldTree
        ) { binding, position, item ->
            // 动态字段顺序不固定，必须通过字段编码查找，不能依赖列表下标。
            val companyName = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_COMPANY_NAME)
            binding.tvCompany.text = companyName?.displayValue ?: ""
            // 单条经历状态为 INCOMPLETE 时，在条目内展示待补充提示。
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE

            // 将服务端字段分别映射到工作经历卡片的时间、职位和内容区域。
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

            // “是否实习”明确返回“是”时才显示实习标签。
            val internship = AttributeFieldItemFinder.findFirst(item.fieldItems, FIELD_INTERNSHIP)
            binding.rlTag.visibility  = if (internship?.displayValue == "是") View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                // -1 或 null 表示新增占位项；真实 instanceIndex 才能作为已有经历编辑。
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

    /**
     * 创建项目经历子列表。
     *
     * 展示项目名称、项目时间、担任角色和项目职责，并按 instanceIndex 区分新增与编辑。
     */
    private fun createProjectExperienceAdapter(
        attributeData: AttributeArrayQueryData,
        fieldTree: List<AttributeFieldTree>
    ): SingleBindingAdapter<ItemProjectExperienceReBinding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_project_experience_re,
            fieldTree
        ) { binding, position, item ->
            // 项目经历与工作经历共用相似布局，但字段编码和目标编辑页不同。
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
                // 占位项进入新增流程；已有项目按当前子列表位置进入编辑流程。
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

    /**
     * 创建教育经历子列表。
     *
     * 学历和专业拼接为一行；时间或专业信息缺失时同步隐藏中间分隔符。
     */
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

            // 过滤空值后再拼接，避免只有学历或专业时出现多余分隔符。
            val majorText = listOfNotNull(
                degree?.displayValue?.takeIf { it.isNotBlank() },
                major?.displayValue?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            binding.tvMajor.text = majorText

            val hasTime = binding.tvTime.text != " - "
            val hasMajor = majorText.isNotBlank()
            binding.tvTime.visibility = if (hasTime) View.VISIBLE else View.GONE
            binding.tvMajor.visibility = if (hasMajor) View.VISIBLE else View.GONE
            // 时间和“学历 · 专业”均存在时才显示两者之间的视觉分隔线。
            binding.v.visibility = if (hasTime && hasMajor) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                // 服务端占位项没有可编辑实例，点击时走新增教育经历流程。
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

    /**
     * 创建技能、证书和奖项共用的子列表。
     *
     * 三类数据共享同一布局，通过 [type] 选择名称字段、附加展示内容及目标编辑页面；
     * 只有技能额外展示熟练度。
     */
    private fun createCertificateSkillsAwardsAdapter(
        attributeData: AttributeArrayQueryData,
        fieldTree: List<AttributeFieldTree>,
        type: String
    ): SingleBindingAdapter<ItemCertificateSkillsAwardsReBinding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_certificate_skills_awards_re,
            fieldTree
        ) { binding, position, item ->
            // 每条数据独立判断完整性，不能直接复用外层分组的 passable 状态。
            binding.layoutMissing.visibility =
                if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE
            // 相同布局中的名称字段在三类分组下使用不同的服务端字段编码。
            val fieldCode = when (type) {
                GROUP_SKILLS -> FIELD_SKILL_NAME
                GROUP_CERTIFICATES -> FIELD_CERTIFICATE_NAME
                else -> FIELD_AWARD_NAME
            }
            val name = AttributeFieldItemFinder.findFirst(item.fieldItems, fieldCode)
            binding.tvName.text = name?.displayValue ?: ""
            // 熟练度仅属于技能；绑定证书或奖项时需主动隐藏，避免 ViewHolder 复用残留。
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
                // 占位项统一回到分组新增入口，真实条目再按类型打开相应编辑页。
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

    /**
     * 工作/项目经历的时间和职位均有内容时才显示中间分隔符。
     */
    private fun bindWorkLikeVisibility(time: String, job: String, divider: View) {
        divider.visibility =
            if (time != " - " && job.isNotEmpty()) View.VISIBLE else View.GONE
    }

    // 以下判断统一封装服务端分组编码；部分接口会返回单数或复数形式，需同时兼容。
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

    // 目前只有技能、奖项和证书可能产生较长列表，因此仅这三类支持展开收起。
    private fun AttributeArrayQueryData.isCollapsibleGroup(): Boolean =
        isSkill() || isAward() || isCertificate()

}
