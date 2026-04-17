
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



public interface UpdateChorebooXpMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UpdateChorebooXpMutation.Data,
      UpdateChorebooXpMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val chorebooId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val level: Int,
    val xp: Int,
    val stage: String
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val choreboo_updateMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateChorebooXp"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateChorebooXpMutation.ref(
  
    chorebooId: java.util.UUID,level: Int,xp: Int,stage: String,

  
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateChorebooXpMutation.Data,
    UpdateChorebooXpMutation.Variables
  > =
  ref(
    
      UpdateChorebooXpMutation.Variables(
        chorebooId=chorebooId,level=level,xp=xp,stage=stage,
  
      )
    
  )

public suspend fun UpdateChorebooXpMutation.execute(

  
    
      chorebooId: java.util.UUID,level: Int,xp: Int,stage: String,

  

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateChorebooXpMutation.Data,
    UpdateChorebooXpMutation.Variables
  > =
  ref(
    
      chorebooId=chorebooId,level=level,xp=xp,stage=stage,
  
    
  ).execute()


