package com.shiyuan.fastclaw.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.LayoutPagingFooterBinding
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 轻量 Paging 追加状态；首屏 loading/empty/error 由页面状态容器负责。
 */
open class FooterAdapter(
    private val hostAdapter: RecyclerView.Adapter<*>,
    private val endStateMinItemCount: Int = 1,
    private val endText: CharSequence = R.string.no_more_data.getResString(),
    private val retry: () -> Unit,
) : LoadStateAdapter<FooterAdapter.ViewHolder>() {

    init {
        require(endStateMinItemCount > 0) {
            "endStateMinItemCount must be greater than 0"
        }
    }

    class ViewHolder(
        val binding: LayoutPagingFooterBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {
        holder.binding.apply {
            pagingFooterLoadingView.visibility =
                if (loadState is LoadState.Loading) View.VISIBLE else View.GONE
            pagingFooterErrorView.visibility =
                if (loadState is LoadState.Error) View.VISIBLE else View.GONE
            pagingFooterEndView.visibility = if (
                loadState is LoadState.NotLoading &&
                loadState.endOfPaginationReached &&
                shouldShowEndState()
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
            pagingFooterEndText.text = endText
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        val holder = ViewHolder(
            LayoutPagingFooterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
        holder.binding.pagingFooterErrorView.setOnSingleClickListener {
            retry()
        }
        return holder
    }

    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return when (loadState) {
            is LoadState.Loading,
            is LoadState.Error,
            -> hostAdapter.itemCount > 0

            is LoadState.NotLoading -> {
                loadState.endOfPaginationReached && shouldShowEndState()
            }
        }
    }

    private fun shouldShowEndState(): Boolean {
        return hostAdapter.itemCount >= endStateMinItemCount
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = true
        }
    }
}
