package cz.prague.cvut.fit.steuejan.amtelapp.view_models.activities

import android.text.Editable
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.prague.cvut.fit.steuejan.amtelapp.App.Companion.context
import cz.prague.cvut.fit.steuejan.amtelapp.App.Companion.toast
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.business.helpers.SingleLiveEvent
import cz.prague.cvut.fit.steuejan.amtelapp.business.AuthManager
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.MatchRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.MessageRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.TeamRepository
import cz.prague.cvut.fit.steuejan.amtelapp.business.util.*
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Match
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Message
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Team
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.MatchResult
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.UserRole
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.toDayInWeek
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.toRole
import cz.prague.cvut.fit.steuejan.amtelapp.states.ValidTeam
import cz.prague.cvut.fit.steuejan.amtelapp.states.ValidWeek
import cz.prague.cvut.fit.steuejan.amtelapp.states.WeekState
import kotlinx.coroutines.launch
import java.util.*

class MatchArrangementActivityVM : ViewModel()
{
    private val _teams = SingleLiveEvent<Pair<Team, Team>>()
    val teams: LiveData<Pair<Team, Team>> = _teams

    /*---------------------------------------------------*/

    private val _date = MutableLiveData<Date?>()
    val date: LiveData<Date?> = _date

    /*---------------------------------------------------*/

    private val _match = MutableLiveData<Match>()
    val match: LiveData<Match> = _match

    fun setMatch(match: Match)
    {
        _match.value = match
    }

    /*---------------------------------------------------*/

    fun getTeams(week: WeekState)
    {
        viewModelScope.launch {
            val home = TeamRepository.findTeam(match.value?.homeId)
            val away = TeamRepository.findTeam(match.value?.awayId)

            if(home is ValidTeam && away is ValidTeam)
            {
                _teams.value = Pair(home.self, away.self)
                match.value?.dateAndTime?.let { _date.value = it }
                    ?: findBestDate(home.self, away.self, week, match.value!!)
            }
        }
    }

    private fun findBestDate(homeTeam: Team, awayTeam: Team, week: WeekState, match: Match)
    {
        viewModelScope.launch {
            if(week is ValidWeek)
            {
                val homeDays = homeTeam.playingDays.map { it.toDayInWeek() }
                val awayDays = awayTeam.playingDays.map { it.toDayInWeek() }

                _date.value = DateUtil.findDate(homeDays, awayDays, week.range)?.setTime(16, 0)
                _date.value?.let { dateAndTime ->
                    MatchRepository.updateMatch(match.id!!, mapOf("dateAndTime" to dateAndTime))
                    _match.value?.dateAndTime = dateAndTime
                    sendEmail(dateAndTime = dateAndTime, place = match.place?.let { it } ?: homeTeam.place)
                }
            }
        }
    }

    fun sendMessage(fullname: String, messageText: String, matchId: String?)
    {
        viewModelScope.launch {
            MessageRepository.addMessage(Message(fullname, messageText, AuthManager.currentUser?.uid ?: ""), matchId, true)
        }
    }

    fun setDialogDate(date: Editable): Calendar?
    {
        return if(date.isEmpty()) null
        else date.toString().toCalendar()
    }

    fun updateDateTime(date: Calendar)
    {
        _date.value = date.time
        viewModelScope.launch {
            if(MatchRepository.updateMatch(match.value?.id, mapOf("dateAndTime" to date.time)))
            {
                toast(context.getString(R.string.match_dateTime_change_success), length = Toast.LENGTH_LONG)
                _match.value?.dateAndTime = date.time
                sendEmail(dateAndTime = date.time)
            }
        }
    }

    fun updatePlace(place: String)
    {
        if(place != match.value?.place ?: "")
        {
            viewModelScope.launch {
                if(MatchRepository.updateMatch(match.value?.id, mapOf("place" to place)))
                {
                    toast(context.getString(R.string.match_place_change_success), length = Toast.LENGTH_LONG)
                    _match.value?.place = place
                    sendEmail(place = place)
                }
            }
        }
    }

    fun initPlace()
    {
        match.value?.place ?: viewModelScope.launch {
            MatchRepository.updateMatch(match.value?.id, mapOf("place" to teams.value?.first?.place))
        }
    }

    fun countTotalScore(match: Match): MatchResult
    {
        var homeScore = 0
        var awayScore = 0

        match.rounds.forEach {
            if(it.homeWinner == true) homeScore++
            else if(it.homeWinner == false) awayScore++
        }

        return MatchResult(homeScore, awayScore)
    }

    private fun sendEmail(dateAndTime: Date? = null, place: String? = null)
    {
        val homeManagerEmail = teams.value?.first?.users?.find { it.role.toRole() == UserRole.TEAM_MANAGER }?.email
        val awayManagerEmail = teams.value?.second?.users?.find {it.role.toRole() == UserRole.TEAM_MANAGER}?.email

        val match = _match.value ?: return

        val subject = "Bylo nastaveno ${place?.let { "místo " } ?: ""}${if(dateAndTime != null && place != null) "a " else ""}${dateAndTime?.let { "datum " } ?: ""}utkání ${match.home}–${match.away} ve skupině ${match.groupName}"

        val message = """
        Dobrý den,
        
        právě bylo v aplikaci nastaveno ${place?.let { "místo " } ?: ""}${if(dateAndTime != null && place != null) "a " else ""}${dateAndTime?.let { "datum " } ?: ""}utkání ${match.home}–${match.away} ve skupině ${match.groupName}.
        
        Místo: ${match.place} ${place?.let { "<---" } ?: ""}
        Datum a čas: ${match.dateAndTime?.toMyString("dd.MM.yyyy 'v' HH:mm") ?: "nespecifikováno"} ${dateAndTime?.let { "<---" } ?: ""}
        
        Na tento email prosím neodpovídejte.
                            
        Administrátor aplikace AMTEL Opava
        """.trimIndent()

        homeManagerEmail?.let { EmailSender.sendEmail(it, subject, message) }
        awayManagerEmail?.let { EmailSender.sendEmail(it, subject, message) }
    }

    fun defaultEndGame(match: Match, isHomeWinner: Boolean, homeTeam: Team, awayTeam: Team): Match
    {
        if(isHomeWinner) setDefaultResult(match, 6, 0)
        else setDefaultResult(match, 0, 6)

        match.rounds.forEach {
            it.homePlayers = mutableListOf()
            it.awayPlayers = mutableListOf()
        }

        viewModelScope.launch {
            sendDefaultResultEmail(match, homeTeam, awayTeam)
        }

        return match
    }

    private fun sendDefaultResultEmail(match: Match, homeTeam: Team, awayTeam: Team)
    {
        val subject = "Byla zvolena kontumační prohra/výhra v utkání ${homeTeam.name}–${awayTeam.name} (skupina ${match.groupName})"

        val message = """
                    Dobrý den,
                    
                    vedoucí týmu ${homeTeam.name} právě zvolil kontumační prohru/výhru v utkání ${homeTeam.name}–${awayTeam.name} ze dne ${match.dateAndTime?.toMyString() ?: "nespecifikováno"}.
                    
                    Kontumačně vyhrál tým: ${if(match.homeScore!! > match.awayScore!!) homeTeam.name else awayTeam.name}
                    Tým ${if(match.homeScore!! < match.awayScore!!) homeTeam.name else awayTeam.name} se rozhodl do utkání nenastoupit a kontumačně prohrál.
                    
                    Na tento email prosím neodpovídejte.
                    
                    Administrátor aplikace AMTEL Opava
                    """.trimIndent()

        val awayManagerEmail = teams.value?.second?.users?.find {it.role.toRole() == UserRole.TEAM_MANAGER}?.email
        awayManagerEmail?.let { EmailSender.sendEmail(it, subject, message) }
        EmailSender.headOfLeagueEmail?.let { EmailSender.sendEmail(it, subject, message) }
    }

    private fun setDefaultResult(match: Match, homePoints: Int, awayPoints: Int)
    {
        match.rounds.forEach {
            it.homeGemsSet1 = homePoints
            it.homeGemsSet2 = homePoints
            it.awayGemsSet1 = awayPoints
            it.awayGemsSet2 = awayPoints

            it.homeGems = 2 * homePoints
            it.awayGems = 2* awayPoints

            if(homePoints > awayPoints)
            {
                it.homeWinner = true
                it.homeSets = 2
                it.awaySets = 0
            }
            else
            {
                it.homeWinner = false
                it.homeSets = 0
                it.awaySets = 2
            }
        }
    }

}