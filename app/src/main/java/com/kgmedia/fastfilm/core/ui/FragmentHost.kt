package com.kgmedia.fastfilm.core.ui

import androidx.fragment.app.FragmentManager

interface FragmentHost {

    fun getHostContainerId(): Int
    fun getHostFragmentManager(): FragmentManager?

}