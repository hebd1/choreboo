
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



public interface UpdateChorebooBackgroundMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UpdateChorebooBackgroundMutation.Data,
      UpdateChorebooBackgroundMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val chorebooId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val backgroundId: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var chorebooId: java.util.UUID
        public var backgroundId: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          chorebooId: java.util.UUID,
          block_: Builder.() -> Unit
        ): Variables {
          var chorebooId= chorebooId
            var backgroundId: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var chorebooId: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { chorebooId = value_ }
              
            override var backgroundId: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { backgroundId = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              chorebooId=chorebooId,backgroundId=backgroundId,
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
    public val operationName: String = "UpdateChorebooBackground"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateChorebooBackgroundMutation.ref(
  
    chorebooId: java.util.UUID,

  
    block_: UpdateChorebooBackgroundMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateChorebooBackgroundMutation.Data,
    UpdateChorebooBackgroundMutation.Variables
  > =
  ref(
    
      UpdateChorebooBackgroundMutation.Variables.build(
        chorebooId=chorebooId,
  
    block_
      )
    
  )

public suspend fun UpdateChorebooBackgroundMutation.execute(

  
    
      chorebooId: java.util.UUID,

  
    block_: UpdateChorebooBackgroundMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateChorebooBackgroundMutation.Data,
    UpdateChorebooBackgroundMutation.Variables
  > =
  ref(
    
      chorebooId=chorebooId,
  
    block_
    
  ).execute()


