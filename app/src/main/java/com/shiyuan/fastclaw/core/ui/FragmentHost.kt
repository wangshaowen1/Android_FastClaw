package com.shiyuan.fastclaw.core.ui

import androidx.fragment.app.FragmentManager

interface FragmentHost {

    fun getHostContainerId(): Int
    fun getHostFragmentManager(): FragmentManager?

}