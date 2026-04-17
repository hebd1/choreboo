
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



public interface UnarchiveHabitMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UnarchiveHabitMutation.Data,
      UnarchiveHabitMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val habitId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habit_updateMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UnarchiveHabit"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UnarchiveHabitMutation.ref(
  
    habitId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    UnarchiveHabitMutation.Data,
    UnarchiveHabitMutation.Variables
  > =
  ref(
    
      UnarchiveHabitMutation.Variables(
        habitId=habitId,
  
      )
    
  )

public suspend fun UnarchiveHabitMutation.execute(

  
    
      habitId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    UnarchiveHabitMutation.Data,
    UnarchiveHabitMutation.Variables
  > =
  ref(
    
      habitId=habitId,
  
    
  ).execute()


