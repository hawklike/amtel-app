package cz.prague.cvut.fit.steuejan.amtelapp.data.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class User(override var id: String? = null,
                var name: String = "",
                var surname: String = "",
                var email: String = "",
                var phone: String? = null,
                var birthdate: Date? = null,
                var sex: Boolean = true,
                var role: String = "",
                var teamId: String? = null,
                var teamName: String? = null,
                var firstSign: Boolean = true,
                var englishName: String = "",
                var englishSurname: String = "",
                var searchSurname: String = "",
                val admin: Boolean = false
                ) : Parcelable, Entity<User>()
{
    override fun toString(): String
    {
        return "$name $surname – $email"
    }
}

fun User.toPlayer(): Player
{
    return Player(this.id ?: "", this.name, this.surname, this.email, this.birthdate, this.sex, null)
}