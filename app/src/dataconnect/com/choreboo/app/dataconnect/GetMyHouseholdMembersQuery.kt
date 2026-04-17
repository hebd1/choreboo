
@file:Suppress(
  "KotlinRedundantDiagnosticSuppress",
  "LocalVariableName",
  "MayBeConstant",
  "RedundantVisibilityModifier",
  "RedundantCompanionReference",
  "RemoveEmptyClassBody",
  "SpellCheckingInspection",
  "LocalVariableName",
  "unused",
)

package com.choreboo.app.dataconnect


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface GetMyHouseholdMembersQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetMyHouseholdMembersQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user: User?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class User(
  
    val household: Household?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Household(
  
    val users_on_household: List<UsersOnHouseholdItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class UsersOnHouseholdItem(
  
    val id: String,
    val displayName: String,
    val photoUrl: String?,
    val email: String?
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetMyHouseholdMembers"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun GetMyHouseholdMembersQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    GetMyHouseholdMembersQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun GetMyHouseholdMembersQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetMyHouseholdMembersQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun GetMyHouseholdMembersQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<GetMyHouseholdMembersQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

