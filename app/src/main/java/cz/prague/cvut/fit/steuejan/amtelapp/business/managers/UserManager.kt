package cz.prague.cvut.fit.steuejan.amtelapp.business.managers

import android.util.Log
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import cz.prague.cvut.fit.steuejan.amtelapp.data.dao.UserDAO
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.User
import cz.prague.cvut.fit.steuejan.amtelapp.data.util.UserOrderBy
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

object UserManager
{
    suspend fun addUser(user: User): User? = withContext(IO)
    {
        return@withContext try
        {
            UserDAO().insert(user)
            Log.i(TAG, "addUser(): $user successfully added to database")
            user
        }
        catch(ex: Exception)
        {
            Log.e(TAG, "addUser(): $user not added to database because $ex")
            null
        }
    }

    suspend fun findUser(id: String?): User? = withContext(IO)
    {
        if(id == null) return@withContext null
        return@withContext try
        {
            val user = UserDAO().findById(id).toObject<User>()
            Log.i(TAG, "findUser(): $user found in database")
            user
        }
        catch(ex: Exception)
        {
            Log.e(TAG, "findUser(): user with $id not found in database because ${ex.message}")
            null
        }
    }

    suspend fun updateUser(documentId: String?, mapOfFieldsAndValues: Map<String, Any?>): Boolean = withContext(IO)
    {
        if(documentId == null) return@withContext false
        return@withContext try
        {
            UserDAO().update(documentId, mapOfFieldsAndValues)
            Log.i(TAG, "updateUser(): user with id $documentId successfully updated with $mapOfFieldsAndValues")
            true
        }
        catch(ex: Exception)
        {
            Log.e(TAG, "updateUser(): user with id $documentId not updated because $ex")
            false
        }
    }

    suspend fun deleteUser(userId: String?): Boolean = withContext(IO)
    {
        if(userId == null) return@withContext false
        return@withContext try
        {
            UserDAO().delete(userId)
            Log.i(TAG, "deleteUser(): user with id $userId successfully deleted with")
            true
        }
        catch(ex: Exception)
        {
            Log.e(TAG, "deleteUser(): user with id $userId not deleted because $ex")
            false
        }
    }

    fun retrieveAllUsers(orderBy: UserOrderBy = UserOrderBy.SURNAME): Query
    {
        val dao = UserDAO()
        return when(orderBy)
        {
            UserOrderBy.NAME -> dao.retrieveAllUsers("name")
            UserOrderBy.SURNAME -> dao.retrieveAllUsers("surname")
            UserOrderBy.TEAM -> dao.retrieveAllUsers("teamName")
            UserOrderBy.EMAIL -> dao.retrieveAllUsers("email")
            UserOrderBy.SEX -> dao.retrieveAllUsers("sex")
        }
    }

    private const val TAG = "UserManager"
}