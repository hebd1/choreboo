
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


public interface GetMyChorebooQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetMyChorebooQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val choreboos: List<ChoreboosItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class ChoreboosItem(
  
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
    val createdAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val sleepUntil: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp?,
    val backgroundId: String?,
    val owner: Owner
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Owner(
  
    val id: String,
    val displayName: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetMyChoreboo"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun GetMyChorebooQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    GetMyChorebooQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun GetMyChorebooQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetMyChorebooQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun GetMyChorebooQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<GetMyChorebooQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

