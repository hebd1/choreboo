
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



public interface DeleteMyUserMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      DeleteMyUserMutation.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user_delete: UserKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteMyUser"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteMyUserMutation.ref(
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteMyUserMutation.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun DeleteMyUserMutation.execute(

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteMyUserMutation.Data,
    Unit
  > =
  ref(
    
  ).execute()


