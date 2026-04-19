
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



public interface ClearActiveChorebooMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      ClearActiveChorebooMutation.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user_update: UserKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "ClearActiveChoreboo"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun ClearActiveChorebooMutation.ref(
  
): com.google.firebase.dataconnect.MutationRef<
    ClearActiveChorebooMutation.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun ClearActiveChorebooMutation.execute(

  

  ): com.google.firebase.dataconnect.MutationResult<
    ClearActiveChorebooMutation.Data,
    Unit
  > =
  ref(
    
  ).execute()


