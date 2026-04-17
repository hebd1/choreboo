
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



public interface DeleteHabitMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      DeleteHabitMutation.Data,
      DeleteHabitMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val habitId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habit_deleteMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteHabit"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteHabitMutation.ref(
  
    habitId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteHabitMutation.Data,
    DeleteHabitMutation.Variables
  > =
  ref(
    
      DeleteHabitMutation.Variables(
        habitId=habitId,
  
      )
    
  )

public suspend fun DeleteHabitMutation.execute(

  
    
      habitId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteHabitMutation.Data,
    DeleteHabitMutation.Variables
  > =
  ref(
    
      habitId=habitId,
  
    
  ).execute()


