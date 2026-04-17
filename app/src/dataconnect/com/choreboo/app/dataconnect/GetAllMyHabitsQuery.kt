
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


public interface GetAllMyHabitsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetAllMyHabitsQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habits: List<HabitsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class HabitsItem(
  
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
    val household: Household?,
    val assignedTo: AssignedTo?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Household(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String
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
  

  public companion object {
    public val operationName: String = "GetAllMyHabits"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun GetAllMyHabitsQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    GetAllMyHabitsQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun GetAllMyHabitsQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetAllMyHabitsQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun GetAllMyHabitsQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<GetAllMyHabitsQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

