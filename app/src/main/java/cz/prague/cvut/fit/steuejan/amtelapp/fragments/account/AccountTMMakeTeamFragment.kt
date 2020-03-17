package cz.prague.cvut.fit.steuejan.amtelapp.fragments.account

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import cz.prague.cvut.fit.steuejan.amtelapp.App.Companion.toast
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.activities.AddUserToTeamActivity
import cz.prague.cvut.fit.steuejan.amtelapp.activities.AddUserToTeamActivity.Companion.TEAM
import cz.prague.cvut.fit.steuejan.amtelapp.adapters.ShowTeamPlayersFirestoreAdapter
import cz.prague.cvut.fit.steuejan.amtelapp.business.managers.TeamManager
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Team
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.User
import cz.prague.cvut.fit.steuejan.amtelapp.fragments.abstracts.AbstractMainActivityFragment
import cz.prague.cvut.fit.steuejan.amtelapp.states.*
import cz.prague.cvut.fit.steuejan.amtelapp.view_models.AccountTMMakeTeamFragmentVM

class AccountTMMakeTeamFragment : AbstractMainActivityFragment()
{
    companion object
    {
        fun newInstance(): AccountTMMakeTeamFragment = AccountTMMakeTeamFragment()
        const val NEW_USER_CODE = 1
    }

    private val viewModel by viewModels<AccountTMMakeTeamFragmentVM>()

    //TODO: [REFACTORING] get team from database in the fragment's view model
    private lateinit var team: TeamState
    private lateinit var user: User

    private var createTeamLayout: RelativeLayout? = null

    private lateinit var nameLayout: TextInputLayout
    private lateinit var placeLayout: TextInputLayout
    private lateinit var playingDaysLayout: TextInputLayout
    private lateinit var createTeam: FloatingActionButton

    private lateinit var addPlayer: RelativeLayout

    private var recyclerView: RecyclerView? = null
    private var adapter: ShowTeamPlayersFirestoreAdapter? = null

    override fun getName(): String = "AccountTMMakeTeamFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.account_tm_make_team, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        createTeamLayout = view.findViewById(R.id.account_tm_make_team)

        nameLayout = view.findViewById(R.id.account_tm_make_team_name)
        placeLayout = view.findViewById(R.id.account_tm_make_team_place)
        playingDaysLayout = view.findViewById(R.id.account_tm_make_team_playing_day)
        createTeam = view.findViewById(R.id.account_tm_make_team_create)
        addPlayer = view.findViewById(R.id.account_tm_make_team_add_player)
        recyclerView = view.findViewById(R.id.account_tm_make_team_players_recyclerView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
        getUser()
        getTeam()
        setupRecycler()
        updateFields()
        setObservers()
        setListeners()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        recyclerView = null

        createTeam.setOnClickListener(null)
        addPlayer.setOnClickListener(null)
        playingDaysLayout.editText?.setOnClickListener(null)

        createTeamLayout?.removeAllViews()
        createTeamLayout = null
    }

    override fun onStart()
    {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop()
    {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        adapter = null
    }

    private fun setupRecycler()
    {
        if(team is ValidTeam)
        {
            recyclerView?.setHasFixedSize(true)
            recyclerView?.layoutManager = LinearLayoutManager(context)

            val query = TeamManager.retrieveAllUsers((team as ValidTeam).self.id!!)
            val options = FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User::class.java)
                .build()

            adapter = ShowTeamPlayersFirestoreAdapter(activity!!, options)
            recyclerView?.adapter = adapter
        }
    }

    private fun setListeners()
    {
        playingDaysLayout.editText?.setOnClickListener {
            MaterialDialog(activity!!).show {
                title(R.string.choose_playing_days)

                val indices = playingDaysLayout.editText?.text?.let {
                    viewModel.setDialogDays(it)
                } ?: intArrayOf()

                listItemsMultiChoice(R.array.days, initialSelection = indices) { _, _, items ->
                    val sortedDays = viewModel.getDialogDays(items)
                    playingDaysLayout.editText?.setText(sortedDays.joinToString(", "))
                }
                positiveButton(R.string.ok)
            }
        }

        createTeam.setOnClickListener {
            val name = nameLayout.editText?.text.toString().trim()
            val place = placeLayout.editText?.text.toString().trim()
            val playingDays = playingDaysLayout.editText?.text.toString().trim()

            deleteErrors()

            MaterialDialog(activity!!)
                .title(R.string.create_team_dialog_title)
                .message(R.string.create_team_dialog_message)
                .show {
                    positiveButton(R.string.yes) {
                        viewModel.createTeam(user, name, place, playingDays)
                    }
                    negativeButton(R.string.no)
                }
        }

        addPlayer.setOnClickListener { addPlayer() }
    }

    private fun addPlayer()
    {
        if(team is ValidTeam)
        {
            val intent = Intent(activity!!, AddUserToTeamActivity::class.java).apply {
                putExtra(TEAM, (team as ValidTeam).self)
            }
            startActivityForResult(intent, NEW_USER_CODE)
        }
        else
        {
            toast(getString(R.string.no_team_alert))
            (Log.e(TAG, "Failed to start AddUserToTeamActivity because team is not valid or initialized yet."))
        }
    }

    private fun setObservers()
    {
        confirmName()
        confirmPlace()
        confirmDays()
        isTeamCreated()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == NEW_USER_CODE && resultCode == RESULT_OK)
        {
            val team = data?.getParcelableExtra<Team>(TEAM)
            team?.let {
                mainActivityModel.setTeam(ValidTeam(it))
            }
        }
    }

    private fun updateFields()
    {
        if(team is ValidTeam)
        {
            nameLayout.editText?.setText((team as ValidTeam).self.name)
            placeLayout.editText?.setText((team as ValidTeam).self.place)
            playingDaysLayout.editText?.setText((team as ValidTeam).self.playingDays.joinToString(", "))
            disableName()
        }
        else
        {
            view!!.findViewById<ImageView>(R.id.account_tm_make_team_add_player_button).visibility = View.GONE
        }
    }

    private fun confirmDays()
    {
        viewModel.playingDays.observe(viewLifecycleOwner) { daysState ->
            if(daysState is InvalidPlayingDays)
                playingDaysLayout.error = daysState.errorMessage
        }
    }

    private fun confirmPlace()
    {
        viewModel.place.observe(viewLifecycleOwner) { placeState ->
            if(placeState is InvalidPlace)
                placeLayout.error = placeState.errorMessage
        }
    }

    private fun confirmName()
    {
        viewModel.name.observe(viewLifecycleOwner) { nameState ->
            if(nameState is InvalidName)
                nameLayout.error = nameState.errorMessage
        }
    }

    private fun getTeam()
    {
        team = mainActivityModel.getTeam().value ?: NoTeam
        mainActivityModel.getTeam().observe(viewLifecycleOwner) { observedTeam ->
            team = observedTeam
        }
    }

    private fun getUser()
    {
        user = mainActivityModel.getUser().value ?: User()
        mainActivityModel.getUser().observe(viewLifecycleOwner) { observedUser ->
            user = observedUser?.copy() ?: user
        }
    }

    private fun isTeamCreated()
    {
        viewModel.newTeam.observe(viewLifecycleOwner) { teamState ->
            val title = viewModel.displayAfterDialog(teamState, user).title

            MaterialDialog(activity!!)
                .title(text = title)
                .show {
                    positiveButton(R.string.ok)
                    onDismiss {}
                }

            update(teamState)
        }
    }

    private fun update(team: TeamState)
    {
        if(team is ValidTeam)
        {
            user.teamId = team.self.id
            user.teamName = team.self.name
            viewModel.updateUser(user, team.self)
            mainActivityModel.setUser(user)
            mainActivityModel.setTeam(ValidTeam(team.self))
            disableName()
            view!!.findViewById<ImageView>(R.id.account_tm_make_team_add_player_button).visibility = View.VISIBLE
        }
    }

    private fun disableName()
    {
        nameLayout.editText?.text?.let {
            if(it.isNotEmpty())
            {
                nameLayout.editText?.isEnabled = false
                nameLayout.editText?.setTextColor(ContextCompat.getColor(activity!!, R.color.lightGrey))
            }
        }
    }

    private fun deleteErrors()
    {
        nameLayout.error = null
        placeLayout.error = null
        playingDaysLayout.error = null
    }
}