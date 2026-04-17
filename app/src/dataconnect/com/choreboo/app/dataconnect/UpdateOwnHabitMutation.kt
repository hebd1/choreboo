
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



public interface UpdateOwnHabitMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ChorebooConnector,
      UpdateOwnHabitMutation.Data,
      UpdateOwnHabitMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val habitId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val title: String,
    val description: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val iconName: String,
    val customDays: String,
    val difficulty: Int,
    val baseXp: Int,
    val reminderEnabled: Boolean,
    val reminderTime: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val isHouseholdHabit: Boolean,
    val householdId: com.google.firebase.dataconnect.OptionalVariable<@kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID?>,
    val assignedToId: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var habitId: java.util.UUID
        public var title: String
        public var description: String?
        public var iconName: String
        public var customDays: String
        public var difficulty: Int
        public var baseXp: Int
        public var reminderEnabled: Boolean
        public var reminderTime: String?
        public var isHouseholdHabit: Boolean
        public var householdId: java.util.UUID?
        public var assignedToId: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          habitId: java.util.UUID,title: String,iconName: String,customDays: String,difficulty: Int,baseXp: Int,reminderEnabled: Boolean,isHouseholdHabit: Boolean,
          block_: Builder.() -> Unit
        ): Variables {
          var habitId= habitId
            var title= title
            var description: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var iconName= iconName
            var customDays= customDays
            var difficulty= difficulty
            var baseXp= baseXp
            var reminderEnabled= reminderEnabled
            var reminderTime: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var isHouseholdHabit= isHouseholdHabit
            var householdId: com.google.firebase.dataconnect.OptionalVariable<java.util.UUID?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var assignedToId: com.google.firebase.dataconnect.OptionalVariable<String?> =
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
              
            override var difficulty: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { difficulty = value_ }
              
            override var baseXp: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { baseXp = value_ }
              
            override var reminderEnabled: Boolean
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { reminderEnabled = value_ }
              
            override var reminderTime: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { reminderTime = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var isHouseholdHabit: Boolean
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { isHouseholdHabit = value_ }
              
            override var householdId: java.util.UUID?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { householdId = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var assignedToId: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { assignedToId = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              habitId=habitId,title=title,description=description,iconName=iconName,customDays=customDays,difficulty=difficulty,baseXp=baseXp,reminderEnabled=reminderEnabled,reminderTime=reminderTime,isHouseholdHabit=isHouseholdHabit,householdId=householdId,assignedToId=assignedToId,
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
    public val operationName: String = "UpdateOwnHabit"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateOwnHabitMutation.ref(
  
    habitId: java.util.UUID,title: String,iconName: String,customDays: String,difficulty: Int,baseXp: Int,reminderEnabled: Boolean,isHouseholdHabit: Boolean,

  
    block_: UpdateOwnHabitMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateOwnHabitMutation.Data,
    UpdateOwnHabitMutation.Variables
  > =
  ref(
    
      UpdateOwnHabitMutation.Variables.build(
        habitId=habitId,title=title,iconName=iconName,customDays=customDays,difficulty=difficulty,baseXp=baseXp,reminderEnabled=reminderEnabled,isHouseholdHabit=isHouseholdHabit,
  
    block_
      )
    
  )

public suspend fun UpdateOwnHabitMutation.execute(

  
    
      habitId: java.util.UUID,title: String,iconName: String,customDays: String,difficulty: Int,baseXp: Int,reminderEnabled: Boolean,isHouseholdHabit: Boolean,

  
    block_: UpdateOwnHabitMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateOwnHabitMutation.Data,
    UpdateOwnHabitMutation.Variables
  > =
  ref(
    
      habitId=habitId,title=title,iconName=iconName,customDays=customDays,difficulty=difficulty,baseXp=baseXp,reminderEnabled=reminderEnabled,isHouseholdHabit=isHouseholdHabit,
  
    block_
    
  ).execute()


