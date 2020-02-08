package cz.prague.cvut.fit.steuejan.amtelapp.data.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Group(var name: String = "",
                 var teamIds: MutableList<String> = mutableListOf()
                 ) : Parcelable, Entity()