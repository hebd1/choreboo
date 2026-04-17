
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



public interface InsertChorebooMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      InsertChorebooMutation.Data,
      InsertChorebooMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val name: String,
    val stage: String,
    val level: Int,
    val xp: Int,
    val hunger: Int,
    val happiness: Int,
    val energy: Int,
    val petType: String,
    val lastInteractionAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val sleepUntil: com.google.firebase.dataconnect.OptionalVariable<@kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
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
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          name: String,stage: String,level: Int,xp: Int,hunger: Int,happiness: Int,energy: Int,petType: String,lastInteractionAt: com.google.firebase.Timestamp,
          block_: Builder.() -> Unit
        ): Variables {
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
            

          return object : Builder {
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
              
            
          }.apply(block_)
          .let {
            Variables(
              name=name,stage=stage,level=level,xp=xp,hunger=hunger,happiness=happiness,energy=energy,petType=petType,lastInteractionAt=lastInteractionAt,sleepUntil=sleepUntil,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val choreboo_insert: ChorebooKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "InsertChoreboo"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun InsertChorebooMutation.ref(
  
    name: String,stage: String,level: Int,xp: Int,hunger: Int,happiness: Int,energy: Int,petType: String,lastInteractionAt: com.google.firebase.Timestamp,

  
    block_: InsertChorebooMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    InsertChorebooMutation.Data,
    InsertChorebooMutation.Variables
  > =
  ref(
    
      InsertChorebooMutation.Variables.build(
        name=name,stage=stage,level=level,xp=xp,hunger=hunger,happiness=happiness,energy=energy,petType=petType,lastInteractionAt=lastInteractionAt,
  
    block_
      )
    
  )

public suspend fun InsertChorebooMutation.execute(

  
    
      name: String,stage: String,level: Int,xp: Int,hunger: Int,happiness: Int,energy: Int,petType: String,lastInteractionAt: com.google.firebase.Timestamp,

  
    block_: InsertChorebooMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    InsertChorebooMutation.Data,
    InsertChorebooMutation.Variables
  > =
  ref(
    
      name=name,stage=stage,level=level,xp=xp,hunger=hunger,happiness=happiness,energy=energy,petType=petType,lastInteractionAt=lastInteractionAt,
  
    block_
    
  ).execute()


