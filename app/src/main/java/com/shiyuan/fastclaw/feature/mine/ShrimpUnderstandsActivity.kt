package com.shiyuan.fastclaw.feature.mine

import FieldItems
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.isNotNull
import com.nhsd.basic.ext.isNull
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.databinding.ActivityShrimpUnderstandsBinding
import com.shiyuan.fastclaw.databinding.ItemJobPreference1Binding
import com.shiyuan.fastclaw.databinding.ItemJobPreference1SonBinding
import com.shiyuan.fastclaw.databinding.ItemJobPreference2Binding
import com.shiyuan.fastclaw.databinding.ItemJobPreference2SonBinding
import com.shiyuan.fastclaw.feature.main.MainActivity
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import formatValueToString
import kotlin.collections.component1
import kotlin.collections.iterator

/**
 * @Introduce: 小虾理解
 */

// 在 ShrimpUnderstandsActivity 类外或内部定义（但需在调用前）
fun FieldItems.maxDepth(): Int = 1 + (fieldItems.maxOfOrNull { it.maxDepth() } ?: 0)
class ShrimpUnderstandsActivity :
    BaseViewModelActivity<ActivityShrimpUnderstandsBinding, UnderstandsViewModel>() {

    companion object {
        private const val EXTRA_PROGRESS = "extra_progress"
        fun start(activity: Activity?, progress: Int = 0) {
            activity ?: return
            val intent = Intent(activity, ShrimpUnderstandsActivity::class.java).apply {
                putExtra(EXTRA_PROGRESS, progress)
            }
            activity.startActivity(intent)
        }
    }

    private val progress: Int by lazy { intent.getIntExtra(EXTRA_PROGRESS, 0) }


    private val mAdapter2 by lazy {
        SingleBindingAdapter<ItemJobPreference1Binding, FieldItems>(
            R.layout.item_job_preference1,
            emptyList(),
            onBind = { binding, position, item ->
                binding.tvTile.text = item.fieldName ?: ""
                if (!item.fieldItems.isNullOrEmpty() && item.fieldItems.size > 1) {
                    binding.tvConnect.visibility = View.GONE
                    binding.rcConnect.visibility = View.VISIBLE
                    val subAdapter = SingleBindingAdapter<ItemJobPreference1SonBinding, FieldItems>(
                        R.layout.item_job_preference1_son,
                        item.fieldItems,
                        onBind = { b, _, sub -> b.tvTag.text = sub.fieldValue.toString() ?: "" })
                    binding.rcConnect.apply {
                        val layoutManager =
                            FlexboxLayoutManager(this@ShrimpUnderstandsActivity).apply {
                                flexDirection = FlexDirection.ROW           // 水平方向排列
                                flexWrap = FlexWrap.WRAP                    // 自动换行
                                justifyContent =
                                    JustifyContent.FLEX_START  // 起始对齐（可根据需要改为 CENTER / SPACE_BETWEEN）
                                alignItems =
                                    AlignItems.STRETCH             // 拉伸以保持行高一致（或改为 FLEX_START）
                            }
                        addItemDecoration(
                            SpaceDecoration(
                                10.dp,
                                SpaceDecoration.Direction.MIDDLE_HORIZONTAL
                            )
                        )
                        this.layoutManager = layoutManager
                        adapter = subAdapter
                    }
                } else if (!item.fieldItems.isNullOrEmpty() && item.fieldItems.size == 1) {
                    binding.rcConnect.visibility = View.GONE
                    binding.tvConnect.visibility = View.VISIBLE
                    binding.tvConnect.text = item.fieldItems[0]?.fieldValue.toString() ?: ""
                } else {
                    binding.rcConnect.visibility = View.GONE
                    binding.tvConnect.visibility = View.VISIBLE
                    binding.tvConnect.text = item.fieldValue?.toString() ?: ""
                }
            }
        )
    }

    private val expandState = mutableMapOf<Int, Boolean>()
    private val mAdapter1: SingleBindingAdapter<ItemJobPreference2Binding, FieldItems> by lazy {
        SingleBindingAdapter<ItemJobPreference2Binding, FieldItems>(
            R.layout.item_job_preference2,
            emptyList(),
            onBind = { binding, position, item ->
                binding.tvTile.text = item.fieldName ?: ""
                if (item.fieldItems.isNullOrEmpty()) {
                    binding.ll.visibility = View.VISIBLE
                    binding.rcConnect.visibility = View.GONE
                    binding.tvLlTile.text = item.fieldName ?: ""
                    binding.tvLlConnect.text = item.fieldValue?.toString() ?: ""

                    // 展开按钮不可见
                    binding.llExpand.visibility = View.GONE
                } else {
                    binding.ll.visibility = View.GONE
                    binding.rcConnect.visibility = View.VISIBLE

                    val totalSize = item.fieldItems.size
                    val isExpanded = expandState[position] == true
                    // 决定显示哪些子项
                    val displayList = if (isExpanded) {
                        item.fieldItems
                    } else {
                        item.fieldItems.take(4)
                    }

                    val subAdapter = SingleBindingAdapter<ItemJobPreference2SonBinding, FieldItems>(
                        R.layout.item_job_preference2_son,
                        displayList,
                        onBind = { sonBinding, _, sonItem ->
                            val hasExtra = item.fieldItems.any { it.extra != null }
                            if (hasExtra) {
                                if (sonItem.extra.isNullOrEmpty()) {
                                    sonBinding.tvParentTile.visibility = View.VISIBLE
                                    sonBinding.llSon.visibility = View.GONE
                                    sonBinding.tvParentTile.text =
                                        sonItem.fieldValue?.toString() ?: ""
                                } else {
                                    sonBinding.tvParentTile.visibility = View.GONE
                                    sonBinding.llSon.visibility = View.VISIBLE
                                    sonBinding.tvTile.text = sonItem.fieldName ?: ""
                                    sonBinding.tvConnect.text = formatValueToString(sonItem.fieldValue)
                                }
                            } else {
                                sonBinding.tvParentTile.visibility = View.GONE
                                sonBinding.llSon.visibility = View.VISIBLE
                                sonBinding.tvTile.text = sonItem.fieldName ?: ""
                                sonBinding.tvConnect.text = formatValueToString(sonItem.fieldValue)
                            }

                        })
                    binding.rcConnect.apply {
                        layoutManager = LinearLayoutManager(
                            this@ShrimpUnderstandsActivity,
                            RecyclerView.VERTICAL,
                            false
                        )
                        adapter = subAdapter
                    }

                    // 处理展开/收起按钮
                    if (totalSize > 4) {
                        binding.llExpand.visibility = View.VISIBLE
                        // 设置按钮文字和箭头方向
                        binding.tvExpand.text = if (isExpanded) "收起全部" else "展开全部"
                        binding.ivArrow.rotation = if (isExpanded) 180f else 0f

                        binding.llExpand.setOnSingleClickListener {
                            // 切换状态
                            expandState[position] = !isExpanded
                            // 刷新当前 item，重新执行 onBind
                            mAdapter1.notifyItemChanged(position)
                        }
                    } else {
                        binding.llExpand.visibility = View.GONE
                    }

                }
            }
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
        mRealVM.understandingScore()
          mRealVM.understandingDetail()
        //mRealVM.setMockData()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.rvUnderstands1.apply {
            layoutManager =
                LinearLayoutManager(this@ShrimpUnderstandsActivity, RecyclerView.VERTICAL, false)
            addItemDecoration(SpaceDecoration(20.dp, SpaceDecoration.Direction.MIDDLE))
            binding.rvUnderstands1.adapter = mAdapter1
        }

        binding.rvUnderstands2.apply {
            layoutManager =
                LinearLayoutManager(this@ShrimpUnderstandsActivity, RecyclerView.VERTICAL, false)
            addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
            binding.rvUnderstands2.adapter = mAdapter2
        }
        binding.tvTable1.setOnSingleClickListener { mRealVM.tableIndex.postValue(0) }
        binding.tvTable2.setOnSingleClickListener { mRealVM.tableIndex.postValue(1) }
        binding.tvBt.setOnSingleClickListener {
            finish()
            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
        }
    }

    fun setConnectVisibility() {
        if (mRealVM.tableIndex.value == 0) {
            binding.flUnderstands2.visibility = View.GONE
            if (mRealVM.resumeInfo.value.isNullOrEmpty()) {
                binding.viewUnderstands.root.visibility = View.VISIBLE
                binding.flUnderstands1.visibility = View.GONE
            } else {
                binding.viewUnderstands.root.visibility = View.GONE
                binding.flUnderstands1.visibility = View.VISIBLE
            }
            val merged = mergeFieldItems(mRealVM.resumeInfo.value ?: emptyList())
            val data = convertToTwoLevels(merged)
            mAdapter1.submitList(data)
        } else {
            binding.flUnderstands1.visibility = View.GONE
            if (mRealVM.jobPreference.value.isNullOrEmpty()) {
                binding.viewUnderstands.root.visibility = View.VISIBLE
                binding.flUnderstands2.visibility = View.GONE
            } else {
                binding.viewUnderstands.root.visibility = View.GONE
                binding.flUnderstands2.visibility = View.VISIBLE
            }
            mAdapter2.submitList(mRealVM.jobPreference.value)
        }
    }

    private fun initObservers() {
        mRealVM.tableIndex.observe(this) {
            if (it == 0) {
                binding.tvTable1.setBackgroundResource(R.drawable.gradient_top_11c4cb_bottom_efcdaf)
                binding.tvTable1.setTextColor(resources.getColor(R.color.white))
                binding.ivTable1.visibility = View.VISIBLE

                binding.tvTable2.background = null
                binding.tvTable2.setTextColor(resources.getColor(R.color.color_666666))
                binding.ivTable2.visibility = View.GONE

                setConnectVisibility()
            } else {
                binding.tvTable1.background = null
                binding.tvTable1.setTextColor(resources.getColor(R.color.color_666666))
                binding.ivTable1.visibility = View.GONE

                binding.tvTable2.setBackgroundResource(R.drawable.gradient_top_11c4cb_bottom_efcdaf)
                binding.tvTable2.setTextColor(resources.getColor(R.color.white))
                binding.ivTable2.visibility = View.VISIBLE

                setConnectVisibility()
            }
        }


        mRealVM.resumeInfo.observe(this) { data ->
            setConnectVisibility()
        }
        mRealVM.jobPreference.observe(this) { data ->
            setConnectVisibility()
        }
        mRealVM.titleInfo.observe(this) { data ->
            data ?: return@observe
            // 安全获取 jobStatus，若为 null 则显示“暂无”
            binding.tvTip.text = data.firstOrNull { it.fieldCode == "jobStatus" }
                ?.fieldValue?.toString() ?: "求职状态"
            // 安全获取 name，若为 null 则显示“暂无”
            binding.tvName.text = data.firstOrNull { it.fieldCode == "name" }
                ?.fieldValue?.toString() ?: "暂无"
        }
        mRealVM.underStandingScore.observe(this) {
            it?.understandingScore?.let {
                if (it == 0 || it.isNull()) {
                    binding.waveView.progress = 10
                    binding.waveView.text = "${0}%"
                } else {
                    binding.waveView.progress = it
                    binding.waveView.text = "${it}%"
                }
            }
        }
    }

    /**
     * 将整个简历信息列表转换为两层结构（顶层节点下的 fieldItems 不再包含嵌套）
     */
    fun convertToTwoLevels(items: List<FieldItems>): List<FieldItems> {
        return items.map { convertTopLevelItem(it) }
    }

    /**
     * 转换单个顶层节点（例如 workExperience, educationInfo 等）
     */
    private fun convertTopLevelItem(item: FieldItems): FieldItems {
        // 判断是否有需要展平的嵌套（即存在子节点且子节点中还有子节点）
        val hasNestedChildren = item.fieldItems.any { it.fieldItems.isNotEmpty() }
        if (!hasNestedChildren) {
            // 没有嵌套，直接返回原对象（注意：为避免外部修改，可浅拷贝，但原对象可复用）
            return item
        }

        // 新的 fieldItems 列表
        val newFieldItems = mutableListOf<FieldItems>()

        for (groupItem in item.fieldItems) {
            // 1) 分组节点本身：清空 fieldItems，保留其他属性，但需要重新构造（无 copy）
            val groupNodeWithoutChildren = FieldItems(
                fieldCode = groupItem.fieldCode,
                fieldName = groupItem.fieldName,
                fieldType = groupItem.fieldType,
                fieldValue = groupItem.fieldValue,
                fieldItems = emptyList(),          // 清空子节点
                extra = groupItem.extra            // 保留原有 extra（如果有）
            )
            newFieldItems.add(groupNodeWithoutChildren)

            // 确定用于标记的 parentTag：优先使用 fieldValue（如公司名/学校名），否则使用 fieldName
            val parentTag = groupItem.fieldValue ?: groupItem.fieldName ?: groupItem.fieldCode ?: ""

            // 2) 将分组下的所有子节点提升，并标记 parent
            for (child in groupItem.fieldItems) {
                // 构造新的 extra，包含 parent 标记（合并原有的 extra）
                val mergedExtra = (child.extra ?: emptyMap()).toMutableMap()
                mergedExtra["parent"] = parentTag

                val liftedChild = FieldItems(
                    fieldCode = child.fieldCode,
                    fieldName = child.fieldName,
                    fieldType = child.fieldType,
                    fieldValue = child.fieldValue,
                    fieldItems = child.fieldItems,  // 子节点本身可能还有更深嵌套？按需递归展平？按你的需求，只展平一层即可，但为安全可递归调用
                    extra = mergedExtra
                )
                newFieldItems.add(liftedChild)
            }
        }

        // 返回新的顶层节点
        return FieldItems(
            fieldCode = item.fieldCode,
            fieldName = item.fieldName,
            fieldType = item.fieldType,
            fieldValue = item.fieldValue,
            fieldItems = newFieldItems,
            extra = item.extra
        )
    }

    fun mergeFieldItems(items: List<FieldItems>): List<FieldItems> {
        if (items.isEmpty()) return emptyList()

        // 1. 先递归处理每个节点的子项，得到新的节点列表
        val processed = items.map { item ->
            FieldItems(
                fieldCode = item.fieldCode,
                fieldName = item.fieldName,
                fieldType = item.fieldType,
                fieldValue = item.fieldValue,
                fieldItems = mergeFieldItems(item.fieldItems), // 递归处理子项
                extra = item.extra
            )
        }

        // 2. 按 fieldCode 分组（过滤掉 fieldCode 为空的项）
        val grouped = processed.filter { it.fieldCode != null }.groupBy { it.fieldCode!! }
        val result = mutableListOf<FieldItems>()

        for ((code, group) in grouped) {
            // 检查该组中所有节点的 fieldItems 是否都为空（即都是叶子节点）
            val allLeaf = group.all { it.fieldItems.isEmpty() }
            if (allLeaf && group.size > 1) {
                // 合并：拼接 fieldValue
                val mergedValue = group.mapNotNull { it.fieldValue?.toString() }
                    .filter { it.isNotBlank() }
                    .joinToString(",")
                    .takeIf { it.isNotEmpty() }
                val first = group.first()
                result.add(
                    FieldItems(
                        fieldCode = code,
                        fieldName = first.fieldName,
                        fieldType = first.fieldType,
                        fieldValue = mergedValue,
                        fieldItems = emptyList(),
                        extra = first.extra
                    )
                )
            } else {
                // 不合并，直接添加所有节点
                result.addAll(group)
            }
        }

        // 3. 添加 fieldCode 为 null 的节点（原样保留）
        result.addAll(processed.filter { it.fieldCode == null })

        return result
    }
}
