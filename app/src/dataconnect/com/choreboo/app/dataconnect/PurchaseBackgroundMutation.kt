
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



public interface PurchaseBackgroundMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      PurchaseBackgroundMutation.Data,
      PurchaseBackgroundMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val backgroundId: String,
    val cost: Int,
    val newTotalPoints: Int
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val purchasedBackground_insert: PurchasedBackgroundKey,
    val user_update: UserKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "PurchaseBackground"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun PurchaseBackgroundMutation.ref(
  
    backgroundId: String,cost: Int,newTotalPoints: Int,

  
  
): com.google.firebase.dataconnect.MutationRef<
    PurchaseBackgroundMutation.Data,
    PurchaseBackgroundMutation.Variables
  > =
  ref(
    
      PurchaseBackgroundMutation.Variables(
        backgroundId=backgroundId,cost=cost,newTotalPoints=newTotalPoints,
  
      )
    
  )

public suspend fun PurchaseBackgroundMutation.execute(

  
    
      backgroundId: String,cost: Int,newTotalPoints: Int,

  

  ): com.google.firebase.dataconnect.MutationResult<
    PurchaseBackgroundMutation.Data,
    PurchaseBackgroundMutation.Variables
  > =
  ref(
    
      backgroundId=backgroundId,cost=cost,newTotalPoints=newTotalPoints,
  
    
  ).execute()


