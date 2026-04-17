
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



public interface DeleteLogsForHabitMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      DeleteLogsForHabitMutation.Data,
      DeleteLogsForHabitMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val habitId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habitLog_deleteMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteLogsForHabit"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteLogsForHabitMutation.ref(
  
    habitId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteLogsForHabitMutation.Data,
    DeleteLogsForHabitMutation.Variables
  > =
  ref(
    
      DeleteLogsForHabitMutation.Variables(
        habitId=habitId,
  
      )
    
  )

public suspend fun DeleteLogsForHabitMutation.execute(

  
    
      habitId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteLogsForHabitMutation.Data,
    DeleteLogsForHabitMutation.Variables
  > =
  ref(
    
      habitId=habitId,
  
    
  ).execute()


