
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


public interface GetLogsForHabitAndDateQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetLogsForHabitAndDateQuery.Data,
      GetLogsForHabitAndDateQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val habitId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val date: String
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habitLogs: List<HabitLogsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class HabitLogsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val completedAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val xpEarned: Int,
    val streakAtCompletion: Int,
    val completedBy: CompletedBy
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class CompletedBy(
  
    val id: String,
    val displayName: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetLogsForHabitAndDate"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetLogsForHabitAndDateQuery.ref(
  
    habitId: java.util.UUID,date: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    GetLogsForHabitAndDateQuery.Data,
    GetLogsForHabitAndDateQuery.Variables
  > =
  ref(
    
      GetLogsForHabitAndDateQuery.Variables(
        habitId=habitId,date=date,
  
      )
    
  )

public suspend fun GetLogsForHabitAndDateQuery.execute(

  
    
      habitId: java.util.UUID,date: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetLogsForHabitAndDateQuery.Data,
    GetLogsForHabitAndDateQuery.Variables
  > =
  ref(
    
      habitId=habitId,date=date,
  
    
  ).execute()


  public fun GetLogsForHabitAndDateQuery.flow(
    
      habitId: java.util.UUID,date: String,

  
    
    ): kotlinx.coroutines.flow.Flow<GetLogsForHabitAndDateQuery.Data> =
    ref(
        
          habitId=habitId,date=date,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

