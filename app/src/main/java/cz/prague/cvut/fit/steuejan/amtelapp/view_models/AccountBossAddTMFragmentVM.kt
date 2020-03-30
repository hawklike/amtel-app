package cz.prague.cvut.fit.steuejan.amtelapp.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.prague.cvut.fit.steuejan.amtelapp.App
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.business.helpers.SingleLiveEvent
import cz.prague.cvut.fit.steuejan.amtelapp.business.managers.AuthManager
import cz.prague.cvut.fit.steuejan.amtelapp.business.managers.UserManager
import cz.prague.cvut.fit.steuejan.amtelapp.business.util.EmailSender
import cz.prague.cvut.fit.steuejan.amtelapp.business.util.StringUtil
import cz.prague.cvut.fit.steuejan.amtelapp.business.util.firstLetterUpperCase
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.User
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.UserRole
import cz.prague.cvut.fit.steuejan.amtelapp.states.*
import kotlinx.coroutines.launch

class AccountBossAddTMFragmentVM : ViewModel()
{
    private val _registration = SingleLiveEvent<RegistrationState>()
    val registration: LiveData<RegistrationState> = _registration

    /*---------------------------------------------------*/

    private val _credentials = SingleLiveEvent<CredentialsState>()
    val credentials: LiveData<CredentialsState> = _credentials

    /*---------------------------------------------------*/

    fun createUser(credentials: ValidCredentials)
    {
        val password = StringUtil.getRandomString(6)

        viewModelScope.launch {
            val uid = AuthManager.signUpUser(credentials.email, password)

            uid?.let {
                val (name, surname, email) = credentials
                val user = User(uid, name, surname, email, role = UserRole.TEAM_MANAGER.toString())

                UserManager.setUser(user)?.let {
                    EmailSender.sendEmail(
                        email,
                        App.context.getString(R.string.verificationEmail_subject),
                        createVerificationTemplate(email, password))
                    _registration.value = ValidRegistration
                } ?: let { _registration.value = InvalidRegistration }

            } ?: let { _registration.value = InvalidRegistration }
        }
    }

    fun confirmCredentials(name: String, surname: String, email: String)
    {
        var okName = true
        var okSurname = true
        var okEmail = true

        var cName = ""
        var cSurname = ""

        if(name.isNotEmpty()) cName = name.firstLetterUpperCase()
        else okName = false

        if(surname.isNotEmpty()) cSurname = surname.firstLetterUpperCase()
        else okSurname = false

        if(EmailState.validate(email) is InvalidEmail) okEmail = false

        if(okName && okSurname && okEmail) _credentials.value = ValidCredentials(cName, cSurname, email)
        else _credentials.value = InvalidCredentials(okName, okSurname, okEmail)
    }

    private fun createVerificationTemplate(email: String, password: String): String
    {
        val head = App.context.getString(R.string.autoEmail_template_head)
        val body = "email: $email\nheslo: $password\n\n"
        val foot = App.context.getString(R.string.autoEmail_template_foot)
        return "$head$body$foot"
    }
}