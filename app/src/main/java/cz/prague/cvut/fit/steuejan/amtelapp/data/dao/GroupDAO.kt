package cz.prague.cvut.fit.steuejan.amtelapp.data.dao

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Group
import kotlinx.coroutines.tasks.await

class GroupDAO : DAO
{
    override val collection = "groups"

    override suspend fun <T> insert(entity: T)
    {
        if(entity is Group) insert(collection, entity)
        else throw IllegalArgumentException("GroupDAO::insert(): entity is not type of Group and should be")
    }

    suspend fun <T> insert(entity: T, merge: Boolean)
    {
        if(entity is Group) insert(collection, entity, merge)
        else throw IllegalArgumentException("GroupDAO::insert(): entity is not type of Group and should be")
    }

    suspend fun <T> insertPlayOff(entity: T)
    {
        if(entity is Group)
        {
            Firebase.firestore
                .collection(collection)
                .document(entity.name)
                .set(entity)
                .await()
        }
        else throw IllegalArgumentException("GroupDAO::insertPlayOff(): entity is not type of Group and should be")
    }

    override suspend fun findById(id: String): DocumentSnapshot
            = findById(collection, id)

    override suspend fun <T> find(field: String, value: T?): QuerySnapshot
            = find(collection, field, value)

    override suspend fun update(documentId: String, mapOfFieldsAndValues: Map<String, Any?>): Unit
            = update(collection, documentId, mapOfFieldsAndValues)

    override suspend fun delete(documentId: String): Unit
            = delete(collection, documentId)

    fun retrieveAllGroupsExceptPlayoff(orderBy: String): Query
    {
        return Firebase.firestore
            .collection(collection)
            .whereEqualTo("playOff", false)
            .orderBy(orderBy, Query.Direction.ASCENDING)
    }

    suspend fun retrieveAllGroupsPlayingPlayoff(orderBy: String): QuerySnapshot
    {
        return Firebase.firestore
            .collection(collection)
            .whereEqualTo("playingPlayOff", true)
            .orderBy(orderBy, Query.Direction.ASCENDING)
            .get()
            .await()
    }

    suspend fun retrieveAllGroupsNotPlayingPlayoff(orderBy: String): QuerySnapshot
    {
        return Firebase.firestore
            .collection(collection)
            .whereEqualTo("playingPlayOff", false)
            .whereEqualTo("playOff", false)
            .orderBy(orderBy, Query.Direction.ASCENDING)
            .get()
            .await()
    }

    suspend fun retrieveAll(): QuerySnapshot
            = retrieveAll(collection)

    suspend fun retrieveTeamsInGroup(groupId: String): QuerySnapshot
    {
        return Firebase.firestore
            .collection(TeamDAO().collection)
            .whereEqualTo("groupId", groupId)
            .get()
            .await()
    }
}