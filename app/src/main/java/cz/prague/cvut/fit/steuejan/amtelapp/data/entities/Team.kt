package cz.prague.cvut.fit.steuejan.amtelapp.data.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Team(override var id: String? = null,
                var name: String = "",
                var tmId: String = "",
                var playingDays: List<String> = emptyList(),
                var place: String? = null,
                var usersId: MutableList<String> = mutableListOf(),
                var users: MutableList<User> = mutableListOf(),
                var pointsPerMatch: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(), //(year, (id_match, points)),
                var pointsPerYear:  MutableMap<String, Int> = mutableMapOf(), //(year, points),
                var groupName: String? = null,
                var winsPerYear: MutableMap<String, Int> = mutableMapOf(), //(year, #wins)
                var lossesPerYear: MutableMap<String, Int> = mutableMapOf(),
                var matchesPerYear: MutableMap<String, Int> = mutableMapOf(),
                var setsPositivePerMatch: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(), //(year, (id_match, +sets)),
                var setsNegativePerMatch: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(), //(year, (id_match, -sets)),
                var positiveSetsPerYear:  MutableMap<String, Int> = mutableMapOf(),
                var negativeSetsPerYear:  MutableMap<String, Int> = mutableMapOf(),
                var results: MutableList<Int> = mutableListOf(), //season final rank
                var groupId: String? = null,
                var searchNameComplete: String = "",
                var searchName: String = "",
                var englishName: String = ""
                ) : Parcelable, Entity<Team>()
{
    override fun toString(): String = this.name
}