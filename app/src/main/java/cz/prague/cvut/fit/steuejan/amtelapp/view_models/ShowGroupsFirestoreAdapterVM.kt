package cz.prague.cvut.fit.steuejan.amtelapp.view_models

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.prague.cvut.fit.steuejan.amtelapp.App.Companion.context
import cz.prague.cvut.fit.steuejan.amtelapp.business.helpers.RobinRoundTournament
import cz.prague.cvut.fit.steuejan.amtelapp.business.managers.MatchManager
import cz.prague.cvut.fit.steuejan.amtelapp.business.managers.TeamManager
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Team
import cz.prague.cvut.fit.steuejan.amtelapp.states.ValidTeams
import kotlinx.coroutines.launch

class ShowGroupsFirestoreAdapterVM : ViewModel()
{
    fun confirmInput(text: String, rounds: Int): Boolean
    {
        val n: Int

        try { n = text.toInt() }
        catch(ex: NumberFormatException) { return false }

        if(n % 2 == 0 || n < rounds) return false

        return true
    }

    fun generateMatches(group: String, rounds: Int)
    {
        viewModelScope.launch {
            with(TeamManager.findTeam("group", group)) {
                if(this is ValidTeams) createMatches(this.self, rounds, group)
            }
        }
    }

    private suspend fun createMatches(teams: List<Team>, rounds: Int, group: String)
    {
        val tournament = RobinRoundTournament()
        tournament.setTeams(teams)
        tournament.setRounds(rounds)
        tournament.createMatches(group).forEach {
            MatchManager.addMatch(it)
        }
        Toast.makeText(context, "Skupina $group vygenerována", Toast.LENGTH_SHORT).show()
    }
}