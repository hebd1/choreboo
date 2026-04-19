
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


public interface GetCurrentUserQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetCurrentUserQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user: User?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class User(
  
    val id: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val activeChoreboo: ActiveChoreboo?,
    val household: Household?,
    val createdAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val totalPoints: Int,
    val totalLifetimeXp: Int
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class ActiveChoreboo(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val petType: String,
    val name: String
  ) {
    
    
  }
      
        @kotlinx.serialization.Serializable
  public data class Household(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String,
    val inviteCode: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetCurrentUser"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun GetCurrentUserQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    GetCurrentUserQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun GetCurrentUserQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetCurrentUserQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun GetCurrentUserQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<GetCurrentUserQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

