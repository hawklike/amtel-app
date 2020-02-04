package cz.prague.cvut.fit.steuejan.amtelapp.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.commit
import androidx.lifecycle.observe
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.business.managers.AuthManager
import cz.prague.cvut.fit.steuejan.amtelapp.business.managers.UserManager
import cz.prague.cvut.fit.steuejan.amtelapp.fragments.*
import cz.prague.cvut.fit.steuejan.amtelapp.fragments.account.AccountFragment
import cz.prague.cvut.fit.steuejan.amtelapp.states.SignedUser
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AbstractBaseActivity()
{
    override lateinit var job: Job

    private lateinit var drawer: Drawer
    lateinit var progressLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?)
    {
        setContentView(R.layout.activity_main)
        progressLayout = findViewById(R.id.progressBar)
        job = Job()
        super.onCreate(savedInstanceState)

        setObservers(savedInstanceState)
        createNavigationDrawer(savedInstanceState)
    }

    override fun onDestroy()
    {
        job.cancel()
        super.onDestroy()
    }

    private fun setObservers(savedInstanceState: Bundle?)
    {
        setToolbarTitle()
        displayAccount(savedInstanceState)
    }

    private fun setToolbarTitle()
    {
        mainActivityVM.getTitle().observe(this) { title ->
            setToolbarTitle(title)
        }
    }

    private fun displayAccount(savedInstanceState: Bundle?)
    {
        progressLayout.visibility = View.VISIBLE
        AuthManager.currentUser?.let { firebaseUser ->
            if(savedInstanceState == null)
            {
                launch {
                    val user =  UserManager.findUser(firebaseUser.uid)
                    user?.let {
                        mainActivityVM.setUserState(SignedUser(it))
                        mainActivityVM.setUser(it)
                        Log.i(TAG, "displayAccount(): $user currently logged in")
                    }
                }
            }
        }

        mainActivityVM.getUserState().observe(this) { user ->
            if(user is SignedUser)
            {
                Log.i(TAG, "displayAccount(): ${user.self} is signed")
                if(::drawer.isInitialized)
                    drawer.updateName(0, StringHolder(getString(R.string.account)))
                baseActivityVM.setLogoutIconVisibility(true)
                populateFragment(AccountFragment.newInstance())
            }
            else
            {
                Log.i(TAG, "displayAccount(): user unsigned")
                if(::drawer.isInitialized)
                    drawer.updateName(0, StringHolder(getString(R.string.login)))
                populateFragment(LoginFragment.newInstance())
            }
        }
    }

    private fun createNavigationDrawer(savedInstanceState: Bundle?)
    {
        val profileTitle = AuthManager.getProfileDrawerOption(applicationContext)
        val profile = PrimaryDrawerItem().withIdentifier(0).withName(profileTitle).withIcon(FontAwesome.Icon.faw_user_edit)
        val results = PrimaryDrawerItem().withName(getString(R.string.results)).withIcon(FontAwesome.Icon.faw_list_ol)
        val schedule = PrimaryDrawerItem().withName(getString(R.string.schedule)).withIcon(FontAwesome.Icon.faw_calendar_alt)
        val teams = PrimaryDrawerItem().withName(getString(R.string.teams)).withIcon(FontAwesome.Icon.faw_users)
        val players = PrimaryDrawerItem().withName(getString(R.string.players)).withIcon(FontAwesome.Icon.faw_user)

        drawer = DrawerBuilder()
            .withActivity(this)
            .withToolbar(toolbar)
            .withTranslucentStatusBar(false)
            .addDrawerItems(
                profile,
                results,
                schedule,
                teams,
                players
            )
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener
            {
                override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean
                {
                    mainActivityVM.setDrawerSelectedPosition(position)
                    when(drawerItem)
                    {
                        profile -> AuthManager.currentUser?.let {
                            val user = mainActivityVM.getUserState().value
                            if(user is SignedUser) populateFragment(AccountFragment.newInstance())
                        } ?: populateFragment(LoginFragment.newInstance())
                        results -> populateFragment(ResultsFragment.newInstance())
                        schedule -> populateFragment(ScheduleFragment.newInstance())
                        teams -> populateFragment(TeamsFragment.newInstance())
                        players -> populateFragment(PlayersFragment.newInstance())
                    }
                    return false
                }
            }).build()

        drawer.drawerLayout.setStatusBarBackground(R.color.white)
        //restores option before configuration change (i.e rotation...)
        drawer.setSelectionAtPosition(mainActivityVM.getDrawerSelectedPosition(), false)

        if(savedInstanceState == null)
        {
            drawer.setSelection(profile)
            mainActivityVM.setDrawerSelectedPosition(drawer.currentSelectedPosition)
        }
    }

    private fun populateFragment(fragment: AbstractBaseFragment)
    {
        progressLayout.visibility = View.VISIBLE
        Log.i(TAG, "${fragment.getName()} populated")
        supportFragmentManager.commit {
            replace(R.id.main_container, fragment)
        }
    }
}
