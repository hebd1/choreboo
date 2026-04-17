
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


public interface GetMyHouseholdHabitsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetMyHouseholdHabitsQuery.Data,
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
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val habits_on_household: List<HabitsOnHouseholdItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class HabitsOnHouseholdItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val title: String,
    val description: String?,
    val iconName: String,
    val customDays: String,
    val difficulty: Int,
    val baseXp: Int,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val isHouseholdHabit: Boolean,
    val isArchived: Boolean,
    val createdAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val owner: Owner,
    val assignedTo: AssignedTo?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Owner(
  
    val id: String,
    val displayName: String
  ) {
    
    
  }
      
        @kotlinx.serialization.Serializable
  public data class AssignedTo(
  
    val id: String,
    val displayName: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetMyHouseholdHabits"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun GetMyHouseholdHabitsQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    GetMyHouseholdHabitsQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun GetMyHouseholdHabitsQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetMyHouseholdHabitsQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun GetMyHouseholdHabitsQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<GetMyHouseholdHabitsQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

