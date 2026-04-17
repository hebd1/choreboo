
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



public interface UpdateChorebooSleepMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UpdateChorebooSleepMutation.Data,
      UpdateChorebooSleepMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val chorebooId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val sleepUntil: com.google.firebase.dataconnect.OptionalVariable<@kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var chorebooId: java.util.UUID
        public var sleepUntil: com.google.firebase.Timestamp?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          chorebooId: java.util.UUID,
          block_: Builder.() -> Unit
        ): Variables {
          var chorebooId= chorebooId
            var sleepUntil: com.google.firebase.dataconnect.OptionalVariable<com.google.firebase.Timestamp?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var chorebooId: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { chorebooId = value_ }
              
            override var sleepUntil: com.google.firebase.Timestamp?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { sleepUntil = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              chorebooId=chorebooId,sleepUntil=sleepUntil,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val choreboo_updateMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateChorebooSleep"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateChorebooSleepMutation.ref(
  
    chorebooId: java.util.UUID,

  
    block_: UpdateChorebooSleepMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateChorebooSleepMutation.Data,
    UpdateChorebooSleepMutation.Variables
  > =
  ref(
    
      UpdateChorebooSleepMutation.Variables.build(
        chorebooId=chorebooId,
  
    block_
      )
    
  )

public suspend fun UpdateChorebooSleepMutation.execute(

  
    
      chorebooId: java.util.UUID,

  
    block_: UpdateChorebooSleepMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateChorebooSleepMutation.Data,
    UpdateChorebooSleepMutation.Variables
  > =
  ref(
    
      chorebooId=chorebooId,
  
    block_
    
  ).execute()


