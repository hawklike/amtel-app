package cz.prague.cvut.fit.steuejan.amtelapp.fragments.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.fragments.AbstractBaseFragment

class AccountBossMakeGroupsFragment : AbstractBaseFragment()
{
    companion object
    {
        fun newInstance(): AccountBossMakeGroupsFragment = AccountBossMakeGroupsFragment()
    }

    override fun getName(): String = "AccountBossMakeGroupsFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.dummy_layout, container, false)
    }
}