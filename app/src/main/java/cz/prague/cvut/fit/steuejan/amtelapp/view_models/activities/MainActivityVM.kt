package cz.prague.cvut.fit.steuejan.amtelapp.view_models.activities

import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.*
import cz.prague.cvut.fit.steuejan.amtelapp.App.Companion.context
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.activities.AbstractBaseActivity
import cz.prague.cvut.fit.steuejan.amtelapp.business.helpers.SingleLiveEvent
import cz.prague.cvut.fit.steuejan.amtelapp.business.util.DateUtil
import cz.prague.cvut.fit.steuejan.amtelapp.business.util.EmailSender
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.User
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.EmailRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.LeagueRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.UserRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.UserRole
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.UserRole.PLAYER
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.toRole
import cz.prague.cvut.fit.steuejan.amtelapp.states.SignedUser
import cz.prague.cvut.fit.steuejan.amtelapp.states.TeamState
import cz.prague.cvut.fit.steuejan.amtelapp.states.UserState
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class MainActivityVM(private val state: SavedStateHandle) : ViewModel()
{
    fun setTitle(title: String)
    {
        state.set(TITLE, title)
    }

    fun getTitle(): LiveData<String> = state.getLiveData(TITLE)

    /*---------------------------------------------------*/

    fun setDrawerSelectedPosition(position: Int)
    {
        state.set(DRAWER_POSITION, position)
    }

    fun getDrawerSelectedPosition(): LiveData<Int> = state.getLiveData(DRAWER_POSITION)

    /*---------------------------------------------------*/

    private val userState = SingleLiveEvent<UserState>()

    fun isUserLoggedIn(userState: UserState)
    {
        this.userState.value = userState
    }

    fun isUserLoggedIn(): LiveData<UserState> = userState

    /*---------------------------------------------------*/

    var isDeadlineAlertShown: Boolean = false
        set(value)
        {
            state.set(DEADLINE_ALERT_SHOWN, value)
            field = value
        }
        get() = state.get<Boolean>(DEADLINE_ALERT_SHOWN) ?: field

    /*---------------------------------------------------*/

    private val user = SingleLiveEvent<User?>()

    fun setUser(user: User?)
    {
        this.user.value = user
        Log.i(TAG, "setUser(): $user is set")
    }

    fun getUser(): LiveData<User?> = user

    /*---------------------------------------------------*/

    private val team = SingleLiveEvent<TeamState>()

    fun setTeam(team: TeamState)
    {
        this.team.value = team
        Log.i(TAG, "setTeam(): $team is set")
    }

    fun getTeam(): LiveData<TeamState> = team

    /*---------------------------------------------------*/

    private val accountPage = MutableLiveData<Int>()

    fun setAccountPage(page: Int)
    {
        accountPage.value = page
    }

    fun getAccountPage(): LiveData<Int> = accountPage

    /*---------------------------------------------------*/

    private val _progressBar = SingleLiveEvent<Boolean>()
    val progressBar: LiveData<Boolean> = _progressBar
    fun setProgressBar(on: Boolean)
    {
        _progressBar.value = on
    }

    /*---------------------------------------------------*/

    private val _connection = MutableLiveData<Boolean>()
    val connection: LiveData<Boolean> = _connection

    /*---------------------------------------------------*/

    private val _headOfLeague = MutableLiveData<User>()
    val headOfLeague: LiveData<User> = _headOfLeague

    /*---------------------------------------------------*/

    private val _userAccountDeleted = SingleLiveEvent<Boolean>()
    val userAccountDeleted: LiveData<Boolean> = _userAccountDeleted

    /*---------------------------------------------------*/

    fun prepareUser(uid: String)
    {
        viewModelScope.launch {
            val user = UserRepository.findUser(uid)
            if(user != null)
            {
                //current user role is a player, therefore shouldn't have an access to the administration part of the app
                if(user.role.toRole() == PLAYER)
                {
                    _userAccountDeleted.value = true
                    return@launch
                }
                //everything is ok, emit live data
                isUserLoggedIn(SignedUser(user))
                setUser(user)
                Log.d(AbstractBaseActivity.TAG, "displayAccount(): $user currently logged in")
            }
            else _userAccountDeleted.value = true
        }
    }

    fun initEmailPassword()
    {
        val email = PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.email_password_key), null)

        if(email == null)
        {
            viewModelScope.launch {
                EmailRepository.getPassword()?.let {
                    EmailSender.hasPassword = true
                    PreferenceManager.
                        getDefaultSharedPreferences(context)
                        .edit()
                        .putString(context.getString(R.string.email_password_key), it)
                        .apply()
                }
            }
        }
        else EmailSender.hasPassword = true
    }

    fun getActualSeason()
    {
        if(DateUtil.actualSeason.toInt() == 0)
        {
            viewModelScope.launch {
                LeagueRepository.getActualSeason()?.let {
                    DateUtil.actualSeason = it.toString()
                }
            }
        }
    }

    //https://stackoverflow.com/a/27312494/9723204
    //could (should) be implemented with a broadcast receiver
    fun checkInternetConnection()
    {
        viewModelScope.launch {
            withContext(IO) {
                try
                {
                    val timeoutMs = 3000
                    val socket = Socket()
                    val socketAddress = InetSocketAddress("8.8.8.8", 53)

                    socket.connect(socketAddress, timeoutMs)
                    socket.close()

                    withContext(Main) {
                        _connection.value = true
                    }
                }
                catch(ex: IOException) {
                    withContext(Main) {
                        _connection.value = false
                    }
                }
            }
        }
    }

    fun initHeadOfLeague(repeat: Int = 10)
    {
        viewModelScope.launch {
            if(EmailSender.headOfLeagueEmail == null)
            {
                UserRepository.findUsers("role", UserRole.HEAD_OF_LEAGUE.toString())?.let { users ->
                    if(users.isNotEmpty())
                    {
                        users.toMutableList().let { mutableUsers ->
                            mutableUsers.find { it.email == context.getString(R.string.adminEmail) } ?.let { admin ->
                                mutableUsers.remove(admin)
                            }

                            if(mutableUsers.isNotEmpty())
                            {
                                val headOfLeague = mutableUsers.first()
                                LeagueRepository.headOfLeague = headOfLeague
                                EmailSender.headOfLeagueEmail = headOfLeague.email
                                _headOfLeague.value = headOfLeague
                            }
                        }
                    }
                    else if(repeat != 0)
                    {
                        delay(5000)
                        initHeadOfLeague(repeat - 1)
                    }
                }
            }
        }
    }

    companion object
    {
        const val TITLE = "title"
        const val DRAWER_POSITION = "position"
        const val DEADLINE_ALERT_SHOWN = "shown"
    }

    private val TAG = "MainActivityVM"
}