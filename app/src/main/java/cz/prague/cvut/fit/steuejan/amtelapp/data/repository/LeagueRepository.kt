package cz.prague.cvut.fit.steuejan.amtelapp.data.repository

import android.util.Log
import com.google.firebase.firestore.ktx.toObject
import cz.prague.cvut.fit.steuejan.amtelapp.business.util.DateUtil
import cz.prague.cvut.fit.steuejan.amtelapp.data.dao.LeagueDAO
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.League
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.User
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.util.*

object LeagueRepository
{
    private const val TAG = "LeagueRepository"

    suspend fun getActualSeason(): Int? = withContext(IO)
    {
        return@withContext try
        {
            val league = LeagueDAO().findById(leagueId).toObject<League>()
            Log.d(TAG, "getActualSeason(): ${league?.actualSeason} successfully retrieved from database")
            league?.actualSeason
        }
        catch(ex: Exception)
        {
            Log.e(TAG, "getActualSeason(): actual season not retrieved from database because ${ex.message}")

            null
        }
    }

    suspend fun incrementSeason(): Boolean = withContext(IO)
    {
        return@withContext try
        {
            LeagueDAO().update(leagueId, mapOf(actualSeason to DateUtil.actualSeason.toInt() + 1))
            DateUtil.actualSeason = (DateUtil.actualSeason.toInt() + 1).toString()
            true
        }
        catch(ex: Exception) { false }
     }

    suspend fun setDeadline(deadline: Date?, from: Boolean): Boolean = withContext(IO)
    {
        return@withContext try
        {
            val dao = LeagueDAO()
            val league = dao.findById(leagueId).toObject<League>()
            if(league != null)
            {
                if(from) league.deadlineFrom[DateUtil.actualSeason] = deadline
                else league.deadlineTo[DateUtil.actualSeason] = deadline
                dao.insert(league)
                return@withContext true
            }
            true
        }
        catch(ex: Exception) { false }
    }

    suspend fun getDeadline(): Pair<Date?, Date?>? = withContext(IO)
    {
        return@withContext try
        {
            val league = LeagueDAO().findById(leagueId).toObject<League>()
            val from = league?.deadlineFrom?.get(DateUtil.actualSeason)
            val to = league?.deadlineTo?.get(DateUtil.actualSeason)
            Pair(from, to)
        }
        catch(ex: Exception) { null }
    }

    suspend fun getServerTime(): Date? = withContext(IO)
    {
        val date: Date?
        return@withContext try
        {
            val dao = LeagueDAO()
            val league = dao.findById(leagueId).toObject<League>()
            league?.let { dao.insert(it) }
            date = dao.findById(leagueId).toObject<League>()?.serverTime
            dao.update(leagueId, mapOf(serverTime to null))
            Log.d(TAG, "successfully retrieved serverTime: $date")
            date
        }
        catch(ex: Exception)
        {
            Log.e(TAG, "serverTime not retrieved")
            null
        }
    }

    var headOfLeague: User? = null

    private const val serverTime = "serverTime"
    private const val actualSeason = "actualSeason"
    internal const val leagueId = "league"
}