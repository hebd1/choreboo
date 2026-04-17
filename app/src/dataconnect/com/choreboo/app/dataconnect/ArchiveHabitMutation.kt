
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



public interface ArchiveHabitMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      ArchiveHabitMutation.Data,
      ArchiveHabitMutation.Variables
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
    public val operationName: String = "ArchiveHabit"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun ArchiveHabitMutation.ref(
  
    habitId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    ArchiveHabitMutation.Data,
    ArchiveHabitMutation.Variables
  > =
  ref(
    
      ArchiveHabitMutation.Variables(
        habitId=habitId,
  
      )
    
  )

public suspend fun ArchiveHabitMutation.execute(

  
    
      habitId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    ArchiveHabitMutation.Data,
    ArchiveHabitMutation.Variables
  > =
  ref(
    
      habitId=habitId,
  
    
  ).execute()


