
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



public interface DeleteAllMyPurchasedBackgroundsMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      DeleteAllMyPurchasedBackgroundsMutation.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val purchasedBackground_deleteMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteAllMyPurchasedBackgrounds"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteAllMyPurchasedBackgroundsMutation.ref(
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteAllMyPurchasedBackgroundsMutation.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun DeleteAllMyPurchasedBackgroundsMutation.execute(

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteAllMyPurchasedBackgroundsMutation.Data,
    Unit
  > =
  ref(
    
  ).execute()


