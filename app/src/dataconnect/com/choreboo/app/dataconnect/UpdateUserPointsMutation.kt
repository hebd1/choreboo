
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



public interface UpdateUserPointsMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UpdateUserPointsMutation.Data,
      UpdateUserPointsMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val totalPoints: Int,
    val totalLifetimeXp: Int
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user_update: UserKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateUserPoints"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateUserPointsMutation.ref(
  
    totalPoints: Int,totalLifetimeXp: Int,

  
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateUserPointsMutation.Data,
    UpdateUserPointsMutation.Variables
  > =
  ref(
    
      UpdateUserPointsMutation.Variables(
        totalPoints=totalPoints,totalLifetimeXp=totalLifetimeXp,
  
      )
    
  )

public suspend fun UpdateUserPointsMutation.execute(

  
    
      totalPoints: Int,totalLifetimeXp: Int,

  

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateUserPointsMutation.Data,
    UpdateUserPointsMutation.Variables
  > =
  ref(
    
      totalPoints=totalPoints,totalLifetimeXp=totalLifetimeXp,
  
    
  ).execute()


