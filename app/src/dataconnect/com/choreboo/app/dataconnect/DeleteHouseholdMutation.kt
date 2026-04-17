
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



public interface DeleteHouseholdMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      DeleteHouseholdMutation.Data,
      DeleteHouseholdMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val householdId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habit_updateMany: Int,
    val user_updateMany: Int,
    val household_deleteMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteHousehold"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteHouseholdMutation.ref(
  
    householdId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteHouseholdMutation.Data,
    DeleteHouseholdMutation.Variables
  > =
  ref(
    
      DeleteHouseholdMutation.Variables(
        householdId=householdId,
  
      )
    
  )

public suspend fun DeleteHouseholdMutation.execute(

  
    
      householdId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteHouseholdMutation.Data,
    DeleteHouseholdMutation.Variables
  > =
  ref(
    
      householdId=householdId,
  
    
  ).execute()


