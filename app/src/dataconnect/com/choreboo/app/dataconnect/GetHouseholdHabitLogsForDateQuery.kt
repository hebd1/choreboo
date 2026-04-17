
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


public interface GetHouseholdHabitLogsForDateQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetHouseholdHabitLogsForDateQuery.Data,
      GetHouseholdHabitLogsForDateQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val date: String
  ) {
    
    
  }
  

  
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
  
    val habits_on_household: List<HabitsOnHouseholdItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class HabitsOnHouseholdItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val habitLogs_on_habit: List<HabitLogsOnHabitItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class HabitLogsOnHabitItem(
  
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
      
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetHouseholdHabitLogsForDate"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetHouseholdHabitLogsForDateQuery.ref(
  
    date: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    GetHouseholdHabitLogsForDateQuery.Data,
    GetHouseholdHabitLogsForDateQuery.Variables
  > =
  ref(
    
      GetHouseholdHabitLogsForDateQuery.Variables(
        date=date,
  
      )
    
  )

public suspend fun GetHouseholdHabitLogsForDateQuery.execute(

  
    
      date: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetHouseholdHabitLogsForDateQuery.Data,
    GetHouseholdHabitLogsForDateQuery.Variables
  > =
  ref(
    
      date=date,
  
    
  ).execute()


  public fun GetHouseholdHabitLogsForDateQuery.flow(
    
      date: String,

  
    
    ): kotlinx.coroutines.flow.Flow<GetHouseholdHabitLogsForDateQuery.Data> =
    ref(
        
          date=date,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

