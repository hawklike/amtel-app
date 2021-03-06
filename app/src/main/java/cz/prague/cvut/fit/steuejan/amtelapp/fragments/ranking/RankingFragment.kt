package cz.prague.cvut.fit.steuejan.amtelapp.fragments.ranking

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.prague.cvut.fit.steuejan.amtelapp.App
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.activities.TeamInfoActivity
import cz.prague.cvut.fit.steuejan.amtelapp.adapters.normal.ShowTeamsRankingAdapter
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Group
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Team
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.RankingOrderBy
import cz.prague.cvut.fit.steuejan.amtelapp.fragments.abstracts.AbstractBaseFragment
import cz.prague.cvut.fit.steuejan.amtelapp.view_models.fragments.RankingFragmentVM

//TODO: resolve data when screen rotates
class RankingFragment : AbstractBaseFragment()
{
    private val viewModel by viewModels<RankingFragmentVM>()

    private var year = 0
    private var group = Group()

    private val teams: MutableList<Team> = mutableListOf()

    private lateinit var actualSortOption: TextView

    private lateinit var sortByMatches: TextView
    private lateinit var sortByWins: TextView
    private lateinit var sortByLosses: TextView
    private lateinit var sortByPositiveSets: TextView
    private lateinit var sortByNegativeSets: TextView
    private lateinit var sortByPoints: TextView

    private lateinit var progressBar: FrameLayout

    private var recyclerView: RecyclerView? = null
    private var adapter: ShowTeamsRankingAdapter? = null

    companion object
    {
        private const val GROUP = "group"
        private const val YEAR = "year"

        fun newInstance(group: Group, year: Int): RankingFragment
        {
            val fragment = RankingFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(GROUP, group)
                putInt(YEAR, year)
            }
            return fragment
        }
    }

    override fun getName(): String = "RankingFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        arguments?.getParcelable<Group>(GROUP)?.let { group = it }
        arguments?.getInt(YEAR)?.let { year = it }
        return inflater.inflate(R.layout.ranking_fragment, container, false)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        adapter?.onClick = null
        recyclerView?.adapter = null
        recyclerView = null
        adapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        sortByMatches = view.findViewById(R.id.ranking_matches)
        sortByWins = view.findViewById(R.id.ranking_wins)
        sortByLosses = view.findViewById(R.id.ranking_losses)
        sortByPositiveSets = view.findViewById(R.id.ranking_sets_positive)
        sortByNegativeSets = view.findViewById(R.id.ranking_sets_negative)
        sortByPoints = view.findViewById(R.id.ranking_points)
        recyclerView = view.findViewById(R.id.ranking_recyclerView)
        progressBar = view.findViewById(R.id.ranking_progressBar)
        actualSortOption = sortByPoints
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
        setupRecycler()
        loadData()
        setBoldStyle(sortByPoints)
        setListeners()
    }

    private fun loadData()
    {
        progressBar.visibility = VISIBLE
        viewModel.loadTeams(group.id!!, year, viewModel.orderBy)
    }

    private fun setupRecycler()
    {
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(activity!!)
        adapter = ShowTeamsRankingAdapter(
            teams,
            year.toString(),
            viewModel.orderBy
        )
        adapter?.onClick = { team, _ ->
            val intent = Intent(activity!!, TeamInfoActivity::class.java).apply {
                putExtra(TeamInfoActivity.TEAM, team)
                putExtra(TeamInfoActivity.RANKING, viewModel.getPosition(team))
            }
            startActivity(intent)
        }
        recyclerView?.adapter = adapter
        populateAdapter()
    }

    private fun populateAdapter()
    {
        viewModel.teams.observe(viewLifecycleOwner) { orderedTeams ->
            progressBar.visibility = GONE
            teams.clear()
            teams.addAll(orderedTeams)
            adapter?.orderBy = viewModel.orderBy
            adapter?.notifyDataSetChanged()
        }
    }

    private fun setListeners()
    {
        sortByMatches.setOnClickListener {
            setOption(it as TextView, RankingOrderBy.MATCHES)
        }

        sortByWins.setOnClickListener {
            setOption(it as TextView, RankingOrderBy.WINS)
        }

        sortByLosses.setOnClickListener {
            setOption(it as TextView, RankingOrderBy.LOSSES)
        }

        sortByPositiveSets.setOnClickListener {
            setOption(it as TextView, RankingOrderBy.POSITIVE_SETS)
        }

        sortByNegativeSets.setOnClickListener {
            setOption(it as TextView, RankingOrderBy.NEGATIVE_SETS)
        }

        sortByPoints.setOnClickListener {
            setOption(it as TextView, RankingOrderBy.POINTS)
        }
    }

    private fun setOption(view: TextView, orderBy: RankingOrderBy)
    {
        progressBar.visibility = VISIBLE
        setNormalStyle(actualSortOption)
        setBoldStyle(view)
        actualSortOption = view
        viewModel.reverseOrder = if(orderBy == viewModel.orderBy) !viewModel.reverseOrder else false
        viewModel.orderBy = orderBy
        viewModel.loadTeams(group.id!!, year, orderBy, viewModel.reverseOrder)
    }

    private fun setNormalStyle(view: TextView)
    {
        view.setTextColor(App.getColor(R.color.darkGrey))
    }

    private fun setBoldStyle(view: TextView)
    {
        view.setTextColor(App.getColor(R.color.blue))
    }

}