
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



public interface CreateHabitLogMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      CreateHabitLogMutation.Data,
      CreateHabitLogMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val habitId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val date: String,
    val xpEarned: Int,
    val streakAtCompletion: Int
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habitLog_insert: HabitLogKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "CreateHabitLog"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun CreateHabitLogMutation.ref(
  
    id: java.util.UUID,habitId: java.util.UUID,date: String,xpEarned: Int,streakAtCompletion: Int,

  
  
): com.google.firebase.dataconnect.MutationRef<
    CreateHabitLogMutation.Data,
    CreateHabitLogMutation.Variables
  > =
  ref(
    
      CreateHabitLogMutation.Variables(
        id=id,habitId=habitId,date=date,xpEarned=xpEarned,streakAtCompletion=streakAtCompletion,
  
      )
    
  )

public suspend fun CreateHabitLogMutation.execute(

  
    
      id: java.util.UUID,habitId: java.util.UUID,date: String,xpEarned: Int,streakAtCompletion: Int,

  

  ): com.google.firebase.dataconnect.MutationResult<
    CreateHabitLogMutation.Data,
    CreateHabitLogMutation.Variables
  > =
  ref(
    
      id=id,habitId=habitId,date=date,xpEarned=xpEarned,streakAtCompletion=streakAtCompletion,
  
    
  ).execute()


