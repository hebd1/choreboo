
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


public interface GetMyHouseholdChoreboosQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetMyHouseholdChoreboosQuery.Data,
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
    val choreboo_on_owner: ChorebooOnOwner?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class ChorebooOnOwner(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String,
    val stage: String,
    val level: Int,
    val xp: Int,
    val hunger: Int,
    val happiness: Int,
    val energy: Int,
    val petType: String,
    val lastInteractionAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val sleepUntil: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp?,
    val backgroundId: String?
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetMyHouseholdChoreboos"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun GetMyHouseholdChoreboosQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    GetMyHouseholdChoreboosQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun GetMyHouseholdChoreboosQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetMyHouseholdChoreboosQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun GetMyHouseholdChoreboosQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<GetMyHouseholdChoreboosQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

