
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



public interface SetActiveChorebooMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      SetActiveChorebooMutation.Data,
      SetActiveChorebooMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val chorebooId: com.google.firebase.dataconnect.OptionalVariable<@kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var chorebooId: java.util.UUID?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          
          block_: Builder.() -> Unit
        ): Variables {
          var chorebooId: com.google.firebase.dataconnect.OptionalVariable<java.util.UUID?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var chorebooId: java.util.UUID?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { chorebooId = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              chorebooId=chorebooId,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user_update: UserKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "SetActiveChoreboo"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun SetActiveChorebooMutation.ref(
  
    

  
    block_: SetActiveChorebooMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    SetActiveChorebooMutation.Data,
    SetActiveChorebooMutation.Variables
  > =
  ref(
    
      SetActiveChorebooMutation.Variables.build(
        
  
    block_
      )
    
  )

public suspend fun SetActiveChorebooMutation.execute(

  
    
      

  
    block_: SetActiveChorebooMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    SetActiveChorebooMutation.Data,
    SetActiveChorebooMutation.Variables
  > =
  ref(
    
      
  
    block_
    
  ).execute()


