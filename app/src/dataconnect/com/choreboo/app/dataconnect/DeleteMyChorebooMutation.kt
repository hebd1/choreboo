
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



public interface DeleteMyChorebooMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      DeleteMyChorebooMutation.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val choreboo_deleteMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteMyChoreboo"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteMyChorebooMutation.ref(
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteMyChorebooMutation.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun DeleteMyChorebooMutation.execute(

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteMyChorebooMutation.Data,
    Unit
  > =
  ref(
    
  ).execute()


