
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



public interface UpdateAssignedHabitMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UpdateAssignedHabitMutation.Data,
      UpdateAssignedHabitMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val habitId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val title: String,
    val description: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val iconName: String,
    val customDays: String,
    val reminderEnabled: Boolean,
    val reminderTime: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var habitId: java.util.UUID
        public var title: String
        public var description: String?
        public var iconName: String
        public var customDays: String
        public var reminderEnabled: Boolean
        public var reminderTime: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          habitId: java.util.UUID,title: String,iconName: String,customDays: String,reminderEnabled: Boolean,
          block_: Builder.() -> Unit
        ): Variables {
          var habitId= habitId
            var title= title
            var description: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var iconName= iconName
            var customDays= customDays
            var reminderEnabled= reminderEnabled
            var reminderTime: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var habitId: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { habitId = value_ }
              
            override var title: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { title = value_ }
              
            override var description: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { description = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var iconName: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { iconName = value_ }
              
            override var customDays: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { customDays = value_ }
              
            override var reminderEnabled: Boolean
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { reminderEnabled = value_ }
              
            override var reminderTime: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { reminderTime = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              habitId=habitId,title=title,description=description,iconName=iconName,customDays=customDays,reminderEnabled=reminderEnabled,reminderTime=reminderTime,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val habit_updateMany: Int
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateAssignedHabit"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateAssignedHabitMutation.ref(
  
    habitId: java.util.UUID,title: String,iconName: String,customDays: String,reminderEnabled: Boolean,

  
    block_: UpdateAssignedHabitMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateAssignedHabitMutation.Data,
    UpdateAssignedHabitMutation.Variables
  > =
  ref(
    
      UpdateAssignedHabitMutation.Variables.build(
        habitId=habitId,title=title,iconName=iconName,customDays=customDays,reminderEnabled=reminderEnabled,
  
    block_
      )
    
  )

public suspend fun UpdateAssignedHabitMutation.execute(

  
    
      habitId: java.util.UUID,title: String,iconName: String,customDays: String,reminderEnabled: Boolean,

  
    block_: UpdateAssignedHabitMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateAssignedHabitMutation.Data,
    UpdateAssignedHabitMutation.Variables
  > =
  ref(
    
      habitId=habitId,title=title,iconName=iconName,customDays=customDays,reminderEnabled=reminderEnabled,
  
    block_
    
  ).execute()


