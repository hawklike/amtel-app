package cz.prague.cvut.fit.steuejan.amtelapp.fragments.miscellaneous

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.Query
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.activities.TeamInfoActivity
import cz.prague.cvut.fit.steuejan.amtelapp.adapters.paging.ShowTeamsPagingAdapter
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Team
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.TeamRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.TeamOrderBy
import cz.prague.cvut.fit.steuejan.amtelapp.databinding.FragmentTeamsBinding
import cz.prague.cvut.fit.steuejan.amtelapp.fragments.abstracts.AbstractMainActivityFragment
import cz.prague.cvut.fit.steuejan.amtelapp.view_models.fragments.TeamsFragmentVM

class TeamsFragment : AbstractMainActivityFragment(), ShowTeamsPagingAdapter.DataLoadedListener
{
    private var _binding: FragmentTeamsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<TeamsFragmentVM>()

    private var adapter: ShowTeamsPagingAdapter? = null

    companion object
    {
        fun newInstance(): TeamsFragment =
            TeamsFragment()
    }

    override fun getName(): String = "TeamsFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        _binding = FragmentTeamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        binding.refreshLayout.setOnRefreshListener(null)

        adapter?.onClick = null
        adapter?.dataLoadedListener = null
        binding.teams.adapter = null
        adapter = null

        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
        setToolbarTitle(getString(R.string.teams))
        setLogoutIconVisibility(false)
        refreshTeams()
        showTeams()
        sortTeams()
        searchTeam()
    }

    private fun refreshTeams()
    {
        binding.refreshLayout.setColorSchemeResources(R.color.blue)
        binding.refreshLayout.setOnRefreshListener {
            adapter?.refresh()
        }
    }

    private fun showTeams()
    {
        binding.teams.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
        }

        val options =  setQueryOptions(viewModel.orderBy)

        adapter = ShowTeamsPagingAdapter(options)
        adapter?.dataLoadedListener = this
        adapter?.onClick = { team ->
            team?.let {
                val intent = Intent(activity, TeamInfoActivity::class.java).apply {
                    putExtra(TeamInfoActivity.TEAM, it)
                }
                startActivity(intent)
            }
        }

        binding.teams.adapter = adapter
    }

    private fun sortTeams()
    {
        binding.sortBy.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId)
            {
                binding.sortByName.id -> {
                    adapter?.updateOptions(setQueryOptions(TeamOrderBy.NAME))
                    viewModel.orderBy = TeamOrderBy.NAME
                }
                binding.sortByGroup.id -> {
                    adapter?.updateOptions(setQueryOptions(TeamOrderBy.GROUP))
                    viewModel.orderBy = TeamOrderBy.GROUP
                }
            }
        }
    }

    private fun searchTeam()
    {
        binding.search.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(text: Editable)
            {
                if(text.isNotEmpty())
                {
                    val result = TeamRepository.retrieveTeamsByPrefix(text.toString())
                    val isCompleteSearch = result.second
                    val query = result.first

                    viewModel.query =
                        if(isCompleteSearch) query.orderBy(TeamRepository.searchNameComplete)
                        else query.orderBy(TeamRepository.searchName)

                    adapter?.updateOptions(setQueryOptions(viewModel.orderBy))
                }
                else
                {
                    viewModel.query = TeamRepository.retrieveAllTeamsQuery()
                    adapter?.updateOptions(setQueryOptions(viewModel.orderBy))
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setQueryOptions(orderBy: TeamOrderBy): FirestorePagingOptions<Team>
    {
        val query = viewModel.query.orderBy(orderBy.toString())
        return setQueryOptions(query)
    }

    private fun setQueryOptions(query: Query): FirestorePagingOptions<Team>
    {
        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPrefetchDistance(5)
            .setPageSize(5)
            .build()

        return FirestorePagingOptions.Builder<Team>()
            .setQuery(query, config, Team::class.java)
            .setLifecycleOwner(this)
            .build()
    }

    override fun onLoaded()
    {
        binding.refreshLayout.isRefreshing = false
    }

    override fun onLoading()
    {
        binding.refreshLayout.isRefreshing = true
    }
}