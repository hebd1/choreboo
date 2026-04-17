
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


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface GetHouseholdByInviteCodeQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetHouseholdByInviteCodeQuery.Data,
      GetHouseholdByInviteCodeQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val inviteCode: String
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val households: List<HouseholdsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class HouseholdsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String,
    val inviteCode: String,
    val createdBy: CreatedBy
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class CreatedBy(
  
    val id: String,
    val displayName: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetHouseholdByInviteCode"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetHouseholdByInviteCodeQuery.ref(
  
    inviteCode: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    GetHouseholdByInviteCodeQuery.Data,
    GetHouseholdByInviteCodeQuery.Variables
  > =
  ref(
    
      GetHouseholdByInviteCodeQuery.Variables(
        inviteCode=inviteCode,
  
      )
    
  )

public suspend fun GetHouseholdByInviteCodeQuery.execute(

  
    
      inviteCode: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetHouseholdByInviteCodeQuery.Data,
    GetHouseholdByInviteCodeQuery.Variables
  > =
  ref(
    
      inviteCode=inviteCode,
  
    
  ).execute()


  public fun GetHouseholdByInviteCodeQuery.flow(
    
      inviteCode: String,

  
    
    ): kotlinx.coroutines.flow.Flow<GetHouseholdByInviteCodeQuery.Data> =
    ref(
        
          inviteCode=inviteCode,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

