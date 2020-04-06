package cz.prague.cvut.fit.steuejan.amtelapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.Query
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.adapters.paging.ShowUsersPagingAdapter
import cz.prague.cvut.fit.steuejan.amtelapp.business.managers.UserManager
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.User
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.UserOrderBy
import cz.prague.cvut.fit.steuejan.amtelapp.databinding.FragmentPlayersBinding
import cz.prague.cvut.fit.steuejan.amtelapp.fragments.abstracts.AbstractMainActivityFragment
import cz.prague.cvut.fit.steuejan.amtelapp.view_models.PlayersFragmentVM

class PlayersFragment : AbstractMainActivityFragment(), ShowUsersPagingAdapter.DataLoadedListener
{
    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<PlayersFragmentVM>()

    private var adapter: ShowUsersPagingAdapter? = null

    companion object
    {
        fun newInstance(): PlayersFragment = PlayersFragment()
    }

    override fun getName(): String = "PlayersFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        adapter?.onDelete = null
        adapter?.onEdit = null
        adapter?.dataLoadedListener = null
        binding.users.adapter = null
        adapter = null

        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
        setToolbarTitle(getString(R.string.players))
        setLogoutIconVisibility(false)
        refreshUsers()
        showUsers()
        sortUsers()
        searchUser()
    }

    private fun refreshUsers()
    {
        binding.refreshLayout.setColorSchemeResources(R.color.blue)
        binding.refreshLayout.setOnRefreshListener {
            adapter?.refresh()
        }
    }

    private fun showUsers()
    {
        binding.users.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
        }

        val options =  setQueryOptions(viewModel.orderBy)

        val currentUser = mainActivityModel.getUser().value
        adapter = ShowUsersPagingAdapter(options, currentUser)
        adapter?.dataLoadedListener = this

        adapter?.onDelete = { user ->
            MaterialDialog(activity!!)
            .title(R.string.delete_user_confirmation_message)
            .show {
                positiveButton(text = "Smazat") { viewModel.deleteUser(user) }
                negativeButton()
            }
        }

        adapter?.onEdit = {
            viewModel.editUser(it)
        }

        binding.users.adapter = adapter
    }

    private fun sortUsers()
    {
        binding.sortBy.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId)
            {
                binding.sortByName.id -> {
                    adapter?.updateOptions(setQueryOptions(UserOrderBy.NAME))
                }

                binding.sortBySurname.id -> {
                    adapter?.updateOptions(setQueryOptions(UserOrderBy.SURNAME))
                }

                binding.sortByTeam.id -> {
                    adapter?.updateOptions(setQueryOptions(UserOrderBy.TEAM))
                }

                binding.sortByRole.id -> {
                    adapter?.updateOptions(setQueryOptions(UserOrderBy.ROLE))
                }

                binding.sortByAge.id -> {
                    adapter?.updateOptions(setQueryOptions(UserOrderBy.AGE))
                }
            }
        }
    }

    private fun searchUser()
    {
        binding.search.addTextChangedListener(object: TextWatcher
        {
            override fun afterTextChanged(text: Editable)
            {
                if(text.isNotEmpty())
                {
                    val query = UserManager.retrieveUsersByPrefix(text.toString())
                    viewModel.query = query.orderBy(UserManager.searchSurname)
                    adapter?.updateOptions(setQueryOptions(viewModel.orderBy))
                }
                else
                {
                    viewModel.query = UserManager.retrieveAllUsers()
                    adapter?.updateOptions(setQueryOptions(viewModel.orderBy))
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setQueryOptions(orderBy: UserOrderBy): FirestorePagingOptions<User>
    {
        viewModel.orderBy = orderBy
        adapter?.orderBy = orderBy
        val query = viewModel.query.orderBy(orderBy.toString())
        return setQueryOptions(query)
    }

    private fun setQueryOptions(query: Query): FirestorePagingOptions<User>
    {
        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPrefetchDistance(2)
            .setPageSize(4)
            .build()

        return FirestorePagingOptions.Builder<User>()
            .setQuery(query, config, User::class.java)
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