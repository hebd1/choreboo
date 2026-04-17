
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


public interface GetMyLogsForDateRangeQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetMyLogsForDateRangeQuery.Data,
      GetMyLogsForDateRangeQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val startDate: String,
    val endDate: String
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habitLogs: List<HabitLogsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class HabitLogsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val habitId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val completedAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val date: String,
    val xpEarned: Int,
    val streakAtCompletion: Int,
    val habit: Habit
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Habit(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val title: String,
    val iconName: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetMyLogsForDateRange"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetMyLogsForDateRangeQuery.ref(
  
    startDate: String,endDate: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    GetMyLogsForDateRangeQuery.Data,
    GetMyLogsForDateRangeQuery.Variables
  > =
  ref(
    
      GetMyLogsForDateRangeQuery.Variables(
        startDate=startDate,endDate=endDate,
  
      )
    
  )

public suspend fun GetMyLogsForDateRangeQuery.execute(

  
    
      startDate: String,endDate: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetMyLogsForDateRangeQuery.Data,
    GetMyLogsForDateRangeQuery.Variables
  > =
  ref(
    
      startDate=startDate,endDate=endDate,
  
    
  ).execute()


  public fun GetMyLogsForDateRangeQuery.flow(
    
      startDate: String,endDate: String,

  
    
    ): kotlinx.coroutines.flow.Flow<GetMyLogsForDateRangeQuery.Data> =
    ref(
        
          startDate=startDate,endDate=endDate,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

