
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


public interface GetMyPurchasedBackgroundsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ChorebooConnector,
      GetMyPurchasedBackgroundsQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val purchasedBackgrounds: List<PurchasedBackgroundsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class PurchasedBackgroundsItem(
  
    val backgroundId: String,
    val purchasedAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetMyPurchasedBackgrounds"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun GetMyPurchasedBackgroundsQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    GetMyPurchasedBackgroundsQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun GetMyPurchasedBackgroundsQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetMyPurchasedBackgroundsQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun GetMyPurchasedBackgroundsQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<GetMyPurchasedBackgroundsQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

