
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



public interface DeleteAllMyHabitLogsMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      DeleteAllMyHabitLogsMutation.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habitLog_deleteMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteAllMyHabitLogs"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteAllMyHabitLogsMutation.ref(
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteAllMyHabitLogsMutation.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun DeleteAllMyHabitLogsMutation.execute(

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteAllMyHabitLogsMutation.Data,
    Unit
  > =
  ref(
    
  ).execute()


