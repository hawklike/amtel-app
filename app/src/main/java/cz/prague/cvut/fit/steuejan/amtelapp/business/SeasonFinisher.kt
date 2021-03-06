package cz.prague.cvut.fit.steuejan.amtelapp.business

import cz.prague.cvut.fit.steuejan.amtelapp.App.Companion.context
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.GroupRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.LeagueRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.MatchRepository
import cz.prague.cvut.fit.steuejan.amtelapp.data.repository.TeamRepository
import cz.prague.cvut.fit.steuejan.amtelapp.business.util.DateUtil
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Group
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Match
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Team
import cz.prague.cvut.fit.steuejan.amtelapp.states.ValidGroup
import cz.prague.cvut.fit.steuejan.amtelapp.states.ValidGroups
import cz.prague.cvut.fit.steuejan.amtelapp.states.ValidMatch
import cz.prague.cvut.fit.steuejan.amtelapp.states.ValidTeams

class SeasonFinisher(private val groups: MutableList<Group>)
{
    //teams are sorted by their final position in a season
    private val groupsWithSortedTeams = mutableMapOf<Group, List<Team>>()

    private val groupsPlayingPlayoff by lazy { groups.filter { it.playingPlayOff } }

    private val playoff = Group("playoff", context.getString(R.string.playOff), playingPlayOff = false, playOff = true, rank = Int.MAX_VALUE)

    private val resolvedTeamIds = mutableSetOf<String>()
    private val finalGroups = mutableListOf<Group>()
    private val teamIdsInPlayoff = mutableListOf<String>()

    private var actualSeason = DateUtil.actualSeason.toInt()

    suspend fun createPlayoff(): Boolean
            = GroupRepository.setPlayoff(playoff) is ValidGroup

    /*
    Updates team's final position in a group in a season.
     */
    suspend fun updateTeamRanks()
    {
        getGroupsIf { groups.isEmpty() }
        prepareFinalGroupsIf { finalGroups.isEmpty() }

        groups.forEach { group ->
            group.teamIds[(actualSeason + 1).toString()] = mutableListOf()
            with(GroupRepository.retrieveTeamsInGroup(group.id)) {
                if(this is ValidTeams)
                {
                    val rankedTeams = RankingSolver(
                        self,
                        actualSeason
                    ).sort()
                    groupsWithSortedTeams[group] = rankedTeams
                    for(i in rankedTeams.indices)
                    {
                        rankedTeams[i].results.add(i+1)
                        TeamRepository.setTeam(rankedTeams[i])
                    }
                }
            }
        }
    }

    private inline fun prepareFinalGroupsIf(predicate: () -> Boolean)
    {
        if(predicate.invoke())
        {
            finalGroups.addAll(groups.map {
                val copy = it.deepCopy()
                copy.teamIds[(actualSeason + 1).toString()] = mutableListOf()
                copy
            }.sorted())
        }
    }

    /*
    Transfers best teams to better groups and vice versa (updates team fields
    groupId and groupName in the database, updates map of team IDs in the groups),
    creates playoff matches between second and last but one teams, updates
    groups with other teams for the next season, changes actual season to a next one.
     */
    suspend fun transferTeams()
    {
        getGroupsIf { groups.isEmpty() }
        prepareFinalGroupsIf { finalGroups.isEmpty() }
        getGroupsWithSortedTeamsIf { groupsWithSortedTeams.isEmpty() }

        transferBottomTeams()
        transferTopTeams()

        createPlayoffMatches()

        addOtherTeamsToNextSeason()
        updateGroups()
        LeagueRepository.incrementSeason()
    }

    private fun addOtherTeamsToNextSeason()
    {
        val sortedResolvedTeams = resolvedTeamIds.toSortedSet()

        groups.forEach { group ->
            group.teamIds[actualSeason.toString()]?.forEach { teamId ->
                //teams not resolved yet
                if(!sortedResolvedTeams.contains(teamId))
                    updateSortedGroups(group, teamId, actualSeason + 1)
            }
        }
    }

    private suspend fun updateGroups()
    {
        finalGroups.forEach {
            GroupRepository.setGroup(it)
        }
        playoff.teamIds[(DateUtil.actualSeason.toInt() + 1).toString()] = teamIdsInPlayoff
        GroupRepository.setPlayoff(playoff)
    }

    /*
    Creates match between second and last but one team in two sequent
    groups (according to a group's rank - the lower the better group is).
     */
    private suspend fun createPlayoffMatches()
    {
        for(i in 0 until groupsPlayingPlayoff.lastIndex)
        {
            val betterGroup = groupsPlayingPlayoff[i]
            val worseGroup = groupsPlayingPlayoff[i+1]

            val betterTeams = groupsWithSortedTeams[betterGroup]
            val worseTeams = groupsWithSortedTeams[worseGroup]

            //has at least 4 teams or 3 teams if it's the first group
            if((betterTeams?.size ?: 0 > 2 && i == 0) || (betterTeams?.size ?: 0 > 3 ))
            {
                //has at least 4 teams or 3 teams if it's the last group
                if((worseTeams?.size ?: 0 > 2 && i + 1 == groupsPlayingPlayoff.lastIndex) || (worseTeams?.size ?: 0 > 3))
                {
                    if(betterTeams != null && worseTeams != null)
                    {
                        val betterTeam = betterTeams[betterTeams.lastIndex - 1]
                        val worseTeam = worseTeams[1]
                        resolvedTeamIds.addAll(listOf(betterTeam.id!!, worseTeam.id!!))
                        teamIdsInPlayoff.addAll(listOf(betterTeam.id!!, worseTeam.id!!))
                        //a home team is always the team in a better group
                        setMatches(listOf(betterTeam, worseTeam))
                    }
                }
            }
        }
    }

    private suspend fun setMatches(teams: List<Team>)
    {
        val tournament = RobinRoundTournament()
        tournament.setTeams(teams)
        //should always retrieve one match
        tournament.createMatches(playoff).forEach {
            it.betterGroup = teams.first().groupName
            it.worseGroup = teams.last().groupName
            with(MatchRepository.setMatch(it)) {
                if(this is ValidMatch) addMatchToTeams(self, teams)
            }
        }
    }

    private suspend fun addMatchToTeams(match: Match, teams: List<Team>)
    {
        val homeTeam = teams.find { it.id == match.homeId }
        val awayTeam = teams.find { it.id == match.awayId }

        homeTeam?.let { TeamRepository.setTeam(it) }
        awayTeam?.let { TeamRepository.setTeam(it) }
    }

    /*
    Transfers the worst teams to worse groups right after the teams current groups.
     */
    private suspend fun transferBottomTeams()
    {
        for(i in groupsPlayingPlayoff.indices)
        {
            if(i == groupsPlayingPlayoff.lastIndex) break
            val nextYear = actualSeason + 1
            val bottomTeam = groupsWithSortedTeams[groupsPlayingPlayoff[i]]?.last()
            bottomTeam?.let { team ->
                val worseGroup = groupsPlayingPlayoff[i+1]
                updateSortedGroups(worseGroup, team.id!!, nextYear)
                resolvedTeamIds.add(team.id!!)
                TeamRepository.updateTeam(team.id, mapOf(TeamRepository.groupId to worseGroup.id!!, TeamRepository.groupName to worseGroup.name))
            }
        }
    }

    /*
    Transfers the best teams to better groups right before the teams current groups.
     */
    private suspend fun transferTopTeams()
    {
        for(i in groupsPlayingPlayoff.indices)
        {
            if(i == 0) continue
            val nextYear = actualSeason + 1
            val topTeam = groupsWithSortedTeams[groupsPlayingPlayoff[i]]?.first()
            topTeam?.let { team ->
                val betterGroup = groupsPlayingPlayoff[i-1]
                updateSortedGroups(betterGroup, team.id!!, nextYear)
                resolvedTeamIds.add(team.id!!)
                TeamRepository.updateTeam(team.id, mapOf(TeamRepository.groupId to betterGroup.id!!, TeamRepository.groupName to betterGroup.name))
            }
        }
    }

    private fun updateSortedGroups(group: Group, teamId: String, nextYear: Int)
    {
        with(finalGroups) {
            val found: Group = this[binarySearch(group)]
            found.teamIds[nextYear.toString()]?.add(teamId)
        }
    }

    private suspend inline fun getGroupsWithSortedTeamsIf(predicate: () -> Boolean)
    {
        if(predicate.invoke())
        {
            groups.forEach { group ->
                group.teamIds[(actualSeason + 1).toString()] = mutableListOf()
                with(GroupRepository.retrieveTeamsInGroup(group.id)) {
                    if(this is ValidTeams)
                    {
                        val rankedTeams = RankingSolver(
                            self,
                            actualSeason
                        ).sort()
                        groupsWithSortedTeams[group] = rankedTeams
                    }
                }
            }
        }
    }

    private suspend inline fun getGroupsIf(predicate: () -> Boolean)
    {
        if(predicate.invoke())
        {
            //already sorted by rank (the lower, the better the group is)
            with(GroupRepository.retrieveAllGroupsExceptPlayoff()) {
                if(this is ValidGroups) groups.addAll(self)
            }
        }
    }
}