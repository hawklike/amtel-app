package cz.prague.cvut.fit.steuejan.amtelapp.fragments.match

import cz.prague.cvut.fit.steuejan.amtelapp.App.Companion.context
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.fragments.abstracts.AbstractMatchActivityFragment

object MatchViewFactory
{
    fun getFragment(title: String, round: Int): AbstractMatchActivityFragment
    {
        return when(title)
        {
            context.getString(R.string.match_input) -> MatchInputResultFragment.newInstance(round)
            context.getString(R.string.match_result) -> MatchResultFragment.newInstance(round)
            else -> throw IllegalArgumentException("Fragment with the title $title doesn't exist.")
        }
    }
}