
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



public interface UpdateChorebooStatsMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UpdateChorebooStatsMutation.Data,
      UpdateChorebooStatsMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val chorebooId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val hunger: Int,
    val happiness: Int,
    val energy: Int,
    val lastInteractionAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val choreboo_updateMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateChorebooStats"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateChorebooStatsMutation.ref(
  
    chorebooId: java.util.UUID,hunger: Int,happiness: Int,energy: Int,lastInteractionAt: com.google.firebase.Timestamp,

  
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateChorebooStatsMutation.Data,
    UpdateChorebooStatsMutation.Variables
  > =
  ref(
    
      UpdateChorebooStatsMutation.Variables(
        chorebooId=chorebooId,hunger=hunger,happiness=happiness,energy=energy,lastInteractionAt=lastInteractionAt,
  
      )
    
  )

public suspend fun UpdateChorebooStatsMutation.execute(

  
    
      chorebooId: java.util.UUID,hunger: Int,happiness: Int,energy: Int,lastInteractionAt: com.google.firebase.Timestamp,

  

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateChorebooStatsMutation.Data,
    UpdateChorebooStatsMutation.Variables
  > =
  ref(
    
      chorebooId=chorebooId,hunger=hunger,happiness=happiness,energy=energy,lastInteractionAt=lastInteractionAt,
  
    
  ).execute()


