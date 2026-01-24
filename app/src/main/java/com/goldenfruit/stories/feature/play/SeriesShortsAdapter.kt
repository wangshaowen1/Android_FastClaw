package com.kgmedia.fastfilm.feature.play

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.databinding.ItemShortChooserBinding
import com.kgmedia.fastfilm.kit.PlaylistListener
import com.kgmedia.fastfilm.kit.ShortPlayer
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

class SeriesShortsAdapter(private val eventVM: ItemEventsViewModel<ShortClickEvent>,val player: ShortPlayer?) : PagingDataAdapter<ShortDramaInfo, RecyclerView.ViewHolder>(object :
    DiffUtil.ItemCallback<ShortDramaInfo>() {

    override fun areItemsTheSame(
        oldItem: ShortDramaInfo, newItem: ShortDramaInfo
    ): Boolean {
        return oldItem == newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
        oldItem: ShortDramaInfo, newItem: ShortDramaInfo
    ): Boolean {
        return oldItem == newItem
    }
}), PlaylistListener {
    private  var currentPlayingVM: ItemSeriesShortVM?=null
    private val vms by lazy { hashMapOf<String?, ItemSeriesShortVM>() }

    override fun onShortChanged(short: ShortDramaInfo?) {
        super.onShortChanged(short)
        if(currentPlayingVM?.data!=short){
            currentPlayingVM?.playing?.set(false)
            vms.get(short?.dramasId)?.apply {
                playing.set(true)
                currentPlayingVM=this
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemShortChooserBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(position)
        }
    }

    inner class ViewHolder(binding: ItemShortChooserBinding) :
        BindingViewHolder<ItemShortChooserBinding>(binding) {

        override fun bind(position: Int) {
            val item = getItem(position)
            val shortVM =vms.getOrPut(item?.dramasId) { ItemSeriesShortVM(info=item,position=position, eventVm = eventVM) }
            if(player?.currentShort()==item){
                shortVM.playing.set(true)
                currentPlayingVM=shortVM
            }
            binding.item = shortVM
            binding.executePendingBindings()
        }

    }


}