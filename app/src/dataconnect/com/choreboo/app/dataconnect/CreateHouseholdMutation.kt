
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



public interface CreateHouseholdMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      CreateHouseholdMutation.Data,
      CreateHouseholdMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val name: String,
    val inviteCode: String
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val household_insert: HouseholdKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "CreateHousehold"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun CreateHouseholdMutation.ref(
  
    name: String,inviteCode: String,

  
  
): com.google.firebase.dataconnect.MutationRef<
    CreateHouseholdMutation.Data,
    CreateHouseholdMutation.Variables
  > =
  ref(
    
      CreateHouseholdMutation.Variables(
        name=name,inviteCode=inviteCode,
  
      )
    
  )

public suspend fun CreateHouseholdMutation.execute(

  
    
      name: String,inviteCode: String,

  

  ): com.google.firebase.dataconnect.MutationResult<
    CreateHouseholdMutation.Data,
    CreateHouseholdMutation.Variables
  > =
  ref(
    
      name=name,inviteCode=inviteCode,
  
    
  ).execute()


