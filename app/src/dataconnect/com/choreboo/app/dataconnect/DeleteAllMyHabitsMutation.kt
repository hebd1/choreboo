
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



public interface DeleteAllMyHabitsMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      DeleteAllMyHabitsMutation.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habit_deleteMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteAllMyHabits"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteAllMyHabitsMutation.ref(
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteAllMyHabitsMutation.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun DeleteAllMyHabitsMutation.execute(

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteAllMyHabitsMutation.Data,
    Unit
  > =
  ref(
    
  ).execute()


