
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



public interface NullifyHouseholdForMembersMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      NullifyHouseholdForMembersMutation.Data,
      NullifyHouseholdForMembersMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val householdId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user_updateMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "NullifyHouseholdForMembers"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun NullifyHouseholdForMembersMutation.ref(
  
    householdId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    NullifyHouseholdForMembersMutation.Data,
    NullifyHouseholdForMembersMutation.Variables
  > =
  ref(
    
      NullifyHouseholdForMembersMutation.Variables(
        householdId=householdId,
  
      )
    
  )

public suspend fun NullifyHouseholdForMembersMutation.execute(

  
    
      householdId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    NullifyHouseholdForMembersMutation.Data,
    NullifyHouseholdForMembersMutation.Variables
  > =
  ref(
    
      householdId=householdId,
  
    
  ).execute()


