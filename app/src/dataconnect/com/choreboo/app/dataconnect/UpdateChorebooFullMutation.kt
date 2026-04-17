
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



public interface UpdateChorebooFullMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UpdateChorebooFullMutation.Data,
      UpdateChorebooFullMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val chorebooId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String,
    val stage: String,
    val level: Int,
    val xp: Int,
    val hunger: Int,
    val happiness: Int,
    val energy: Int,
    val petType: String,
    val lastInteractionAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val sleepUntil: com.google.firebase.dataconnect.OptionalVariable<@kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp?>,
    val backgroundId: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var chorebooId: java.util.UUID
        public var name: String
        public var stage: String
        public var level: Int
        public var xp: Int
        public var hunger: Int
        public var happiness: Int
        public var energy: Int
        public var petType: String
        public var lastInteractionAt: com.google.firebase.Timestamp
        public var sleepUntil: com.google.firebase.Timestamp?
        public var backgroundId: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          chorebooId: java.util.UUID,name: String,stage: String,level: Int,xp: Int,hunger: Int,happiness: Int,energy: Int,petType: String,lastInteractionAt: com.google.firebase.Timestamp,
          block_: Builder.() -> Unit
        ): Variables {
          var chorebooId= chorebooId
            var name= name
            var stage= stage
            var level= level
            var xp= xp
            var hunger= hunger
            var happiness= happiness
            var energy= energy
            var petType= petType
            var lastInteractionAt= lastInteractionAt
            var sleepUntil: com.google.firebase.dataconnect.OptionalVariable<com.google.firebase.Timestamp?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var backgroundId: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var chorebooId: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { chorebooId = value_ }
              
            override var name: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { name = value_ }
              
            override var stage: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { stage = value_ }
              
            override var level: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { level = value_ }
              
            override var xp: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { xp = value_ }
              
            override var hunger: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { hunger = value_ }
              
            override var happiness: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { happiness = value_ }
              
            override var energy: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { energy = value_ }
              
            override var petType: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { petType = value_ }
              
            override var lastInteractionAt: com.google.firebase.Timestamp
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { lastInteractionAt = value_ }
              
            override var sleepUntil: com.google.firebase.Timestamp?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { sleepUntil = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var backgroundId: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { backgroundId = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              chorebooId=chorebooId,name=name,stage=stage,level=level,xp=xp,hunger=hunger,happiness=happiness,energy=energy,petType=petType,lastInteractionAt=lastInteractionAt,sleepUntil=sleepUntil,backgroundId=backgroundId,
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
    public val operationName: String = "UpdateChorebooFull"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateChorebooFullMutation.ref(
  
    chorebooId: java.util.UUID,name: String,stage: String,level: Int,xp: Int,hunger: Int,happiness: Int,energy: Int,petType: String,lastInteractionAt: com.google.firebase.Timestamp,

  
    block_: UpdateChorebooFullMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateChorebooFullMutation.Data,
    UpdateChorebooFullMutation.Variables
  > =
  ref(
    
      UpdateChorebooFullMutation.Variables.build(
        chorebooId=chorebooId,name=name,stage=stage,level=level,xp=xp,hunger=hunger,happiness=happiness,energy=energy,petType=petType,lastInteractionAt=lastInteractionAt,
  
    block_
      )
    
  )

public suspend fun UpdateChorebooFullMutation.execute(

  
    
      chorebooId: java.util.UUID,name: String,stage: String,level: Int,xp: Int,hunger: Int,happiness: Int,energy: Int,petType: String,lastInteractionAt: com.google.firebase.Timestamp,

  
    block_: UpdateChorebooFullMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateChorebooFullMutation.Data,
    UpdateChorebooFullMutation.Variables
  > =
  ref(
    
      chorebooId=chorebooId,name=name,stage=stage,level=level,xp=xp,hunger=hunger,happiness=happiness,energy=energy,petType=petType,lastInteractionAt=lastInteractionAt,
  
    block_
    
  ).execute()


