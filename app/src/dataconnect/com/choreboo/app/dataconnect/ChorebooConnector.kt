
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

import com.google.firebase.dataconnect.getInstance as _fdcGetInstance
import kotlin.time.Duration.Companion.milliseconds as _milliseconds

public interface ChorebooConnector : com.google.firebase.dataconnect.generated.GeneratedConnector<ChorebooConnector> {
  override val dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect

  
    public val archiveHabit: ArchiveHabitMutation
  
    public val createHabit: CreateHabitMutation
  
    public val createHabitLog: CreateHabitLogMutation
  
    public val createHousehold: CreateHouseholdMutation
  
    public val deleteAllMyHabitLogs: DeleteAllMyHabitLogsMutation
  
    public val deleteAllMyHabits: DeleteAllMyHabitsMutation
  
    public val deleteAllMyPurchasedBackgrounds: DeleteAllMyPurchasedBackgroundsMutation
  
    public val deleteHabit: DeleteHabitMutation
  
    public val deleteHousehold: DeleteHouseholdMutation
  
    public val deleteLogsForHabit: DeleteLogsForHabitMutation
  
    public val deleteMyChoreboo: DeleteMyChorebooMutation
  
    public val deleteMyUser: DeleteMyUserMutation
  
    public val getAllMyHabits: GetAllMyHabitsQuery
  
    public val getCurrentUser: GetCurrentUserQuery
  
    public val getHouseholdByInviteCode: GetHouseholdByInviteCodeQuery
  
    public val getHouseholdHabitLogsForDate: GetHouseholdHabitLogsForDateQuery
  
    public val getLogsForHabitAndDate: GetLogsForHabitAndDateQuery
  
    public val getMyChoreboo: GetMyChorebooQuery
  
    public val getMyHousehold: GetMyHouseholdQuery
  
    public val getMyHouseholdChoreboos: GetMyHouseholdChoreboosQuery
  
    public val getMyHouseholdHabits: GetMyHouseholdHabitsQuery
  
    public val getMyHouseholdMembers: GetMyHouseholdMembersQuery
  
    public val getMyLogsForDateRange: GetMyLogsForDateRangeQuery
  
    public val getMyPurchasedBackgrounds: GetMyPurchasedBackgroundsQuery
  
    public val insertChoreboo: InsertChorebooMutation
  
    public val nullifyHouseholdForMembers: NullifyHouseholdForMembersMutation
  
    public val purchaseBackground: PurchaseBackgroundMutation
  
    public val unarchiveHabit: UnarchiveHabitMutation
  
    public val updateAssignedHabit: UpdateAssignedHabitMutation
  
    public val updateChorebooBackground: UpdateChorebooBackgroundMutation
  
    public val updateChorebooFull: UpdateChorebooFullMutation
  
    public val updateChorebooSleep: UpdateChorebooSleepMutation
  
    public val updateChorebooStats: UpdateChorebooStatsMutation
  
    public val updateChorebooXp: UpdateChorebooXpMutation
  
    public val updateOwnHabit: UpdateOwnHabitMutation
  
    public val updateUserHousehold: UpdateUserHouseholdMutation
  
    public val updateUserPoints: UpdateUserPointsMutation
  
    public val upsertUser: UpsertUserMutation
  

  public companion object {
    @Suppress("MemberVisibilityCanBePrivate")
    public val config: com.google.firebase.dataconnect.ConnectorConfig = com.google.firebase.dataconnect.ConnectorConfig(
      connector = "choreboo",
      location = "us-central1",
      serviceId = "choreboo-dataconnect",
    )

    public fun getInstance(
      dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect
    ):ChorebooConnector = synchronized(instances) {
      instances.getOrPut(dataConnect) {
        ChorebooConnectorImpl(dataConnect)
      }
    }

    private val instances = java.util.WeakHashMap<com.google.firebase.dataconnect.FirebaseDataConnect, ChorebooConnectorImpl>()

    
  }
}

public val ChorebooConnector.Companion.instance:ChorebooConnector
  get() = getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(
    config
  ))

public fun ChorebooConnector.Companion.getInstance(
  settings: com.google.firebase.dataconnect.DataConnectSettings = com.google.firebase.dataconnect.DataConnectSettings()
):ChorebooConnector =
  getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(config, settings))

public fun ChorebooConnector.Companion.getInstance(
  app: com.google.firebase.FirebaseApp,
  settings: com.google.firebase.dataconnect.DataConnectSettings = com.google.firebase.dataconnect.DataConnectSettings()
):ChorebooConnector =
  getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(app, config, settings))

private class ChorebooConnectorImpl(
  override val dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect
) : ChorebooConnector {
  
    override val archiveHabit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      ArchiveHabitMutationImpl(this)
    }
  
    override val createHabit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateHabitMutationImpl(this)
    }
  
    override val createHabitLog by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateHabitLogMutationImpl(this)
    }
  
    override val createHousehold by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateHouseholdMutationImpl(this)
    }
  
    override val deleteAllMyHabitLogs by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteAllMyHabitLogsMutationImpl(this)
    }
  
    override val deleteAllMyHabits by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteAllMyHabitsMutationImpl(this)
    }
  
    override val deleteAllMyPurchasedBackgrounds by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteAllMyPurchasedBackgroundsMutationImpl(this)
    }
  
    override val deleteHabit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteHabitMutationImpl(this)
    }
  
    override val deleteHousehold by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteHouseholdMutationImpl(this)
    }
  
    override val deleteLogsForHabit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteLogsForHabitMutationImpl(this)
    }
  
    override val deleteMyChoreboo by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteMyChorebooMutationImpl(this)
    }
  
    override val deleteMyUser by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteMyUserMutationImpl(this)
    }
  
    override val getAllMyHabits by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetAllMyHabitsQueryImpl(this)
    }
  
    override val getCurrentUser by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetCurrentUserQueryImpl(this)
    }
  
    override val getHouseholdByInviteCode by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetHouseholdByInviteCodeQueryImpl(this)
    }
  
    override val getHouseholdHabitLogsForDate by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetHouseholdHabitLogsForDateQueryImpl(this)
    }
  
    override val getLogsForHabitAndDate by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetLogsForHabitAndDateQueryImpl(this)
    }
  
    override val getMyChoreboo by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetMyChorebooQueryImpl(this)
    }
  
    override val getMyHousehold by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetMyHouseholdQueryImpl(this)
    }
  
    override val getMyHouseholdChoreboos by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetMyHouseholdChoreboosQueryImpl(this)
    }
  
    override val getMyHouseholdHabits by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetMyHouseholdHabitsQueryImpl(this)
    }
  
    override val getMyHouseholdMembers by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetMyHouseholdMembersQueryImpl(this)
    }
  
    override val getMyLogsForDateRange by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetMyLogsForDateRangeQueryImpl(this)
    }
  
    override val getMyPurchasedBackgrounds by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetMyPurchasedBackgroundsQueryImpl(this)
    }
  
    override val insertChoreboo by lazy(LazyThreadSafetyMode.PUBLICATION) {
      InsertChorebooMutationImpl(this)
    }
  
    override val nullifyHouseholdForMembers by lazy(LazyThreadSafetyMode.PUBLICATION) {
      NullifyHouseholdForMembersMutationImpl(this)
    }
  
    override val purchaseBackground by lazy(LazyThreadSafetyMode.PUBLICATION) {
      PurchaseBackgroundMutationImpl(this)
    }
  
    override val unarchiveHabit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UnarchiveHabitMutationImpl(this)
    }
  
    override val updateAssignedHabit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateAssignedHabitMutationImpl(this)
    }
  
    override val updateChorebooBackground by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateChorebooBackgroundMutationImpl(this)
    }
  
    override val updateChorebooFull by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateChorebooFullMutationImpl(this)
    }
  
    override val updateChorebooSleep by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateChorebooSleepMutationImpl(this)
    }
  
    override val updateChorebooStats by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateChorebooStatsMutationImpl(this)
    }
  
    override val updateChorebooXp by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateChorebooXpMutationImpl(this)
    }
  
    override val updateOwnHabit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateOwnHabitMutationImpl(this)
    }
  
    override val updateUserHousehold by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateUserHouseholdMutationImpl(this)
    }
  
    override val updateUserPoints by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateUserPointsMutationImpl(this)
    }
  
    override val upsertUser by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpsertUserMutationImpl(this)
    }
  

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun operations(): List<com.google.firebase.dataconnect.generated.GeneratedOperation<ChorebooConnector, *, *>> =
    queries() + mutations()

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun mutations(): List<com.google.firebase.dataconnect.generated.GeneratedMutation<ChorebooConnector, *, *>> =
    listOf(
      archiveHabit,
        createHabit,
        createHabitLog,
        createHousehold,
        deleteAllMyHabitLogs,
        deleteAllMyHabits,
        deleteAllMyPurchasedBackgrounds,
        deleteHabit,
        deleteHousehold,
        deleteLogsForHabit,
        deleteMyChoreboo,
        deleteMyUser,
        insertChoreboo,
        nullifyHouseholdForMembers,
        purchaseBackground,
        unarchiveHabit,
        updateAssignedHabit,
        updateChorebooBackground,
        updateChorebooFull,
        updateChorebooSleep,
        updateChorebooStats,
        updateChorebooXp,
        updateOwnHabit,
        updateUserHousehold,
        updateUserPoints,
        upsertUser,
        
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun queries(): List<com.google.firebase.dataconnect.generated.GeneratedQuery<ChorebooConnector, *, *>> =
    listOf(
      getAllMyHabits,
        getCurrentUser,
        getHouseholdByInviteCode,
        getHouseholdHabitLogsForDate,
        getLogsForHabitAndDate,
        getMyChoreboo,
        getMyHousehold,
        getMyHouseholdChoreboos,
        getMyHouseholdHabits,
        getMyHouseholdMembers,
        getMyLogsForDateRange,
        getMyPurchasedBackgrounds,
        
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect) =
    ChorebooConnectorImpl(dataConnect)

  override fun equals(other: Any?): Boolean =
    other is ChorebooConnectorImpl &&
    other.dataConnect == dataConnect

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "ChorebooConnectorImpl",
      dataConnect,
    )

  override fun toString(): String =
    "ChorebooConnectorImpl(dataConnect=$dataConnect)"
}



private open class ChorebooConnectorGeneratedQueryImpl<Data, Variables>(
  override val connector: ChorebooConnector,
  override val operationName: String,
  override val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
  override val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
) : com.google.firebase.dataconnect.generated.GeneratedQuery<ChorebooConnector, Data, Variables> {

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(
    connector: ChorebooConnector,
    operationName: String,
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
    variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
  ) =
    ChorebooConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewVariables> withVariablesSerializer(
    variablesSerializer: kotlinx.serialization.SerializationStrategy<NewVariables>
  ) =
    ChorebooConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewData> withDataDeserializer(
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<NewData>
  ) =
    ChorebooConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun equals(other: Any?): Boolean =
    other is ChorebooConnectorGeneratedQueryImpl<*,*> &&
    other.connector == connector &&
    other.operationName == operationName &&
    other.dataDeserializer == dataDeserializer &&
    other.variablesSerializer == variablesSerializer

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "ChorebooConnectorGeneratedQueryImpl",
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun toString(): String =
    "ChorebooConnectorGeneratedQueryImpl(" +
    "operationName=$operationName, " +
    "dataDeserializer=$dataDeserializer, " +
    "variablesSerializer=$variablesSerializer, " +
    "connector=$connector)"
}

private open class ChorebooConnectorGeneratedMutationImpl<Data, Variables>(
  override val connector: ChorebooConnector,
  override val operationName: String,
  override val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
  override val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
) : com.google.firebase.dataconnect.generated.GeneratedMutation<ChorebooConnector, Data, Variables> {

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(
    connector: ChorebooConnector,
    operationName: String,
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
    variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
  ) =
    ChorebooConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewVariables> withVariablesSerializer(
    variablesSerializer: kotlinx.serialization.SerializationStrategy<NewVariables>
  ) =
    ChorebooConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewData> withDataDeserializer(
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<NewData>
  ) =
    ChorebooConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun equals(other: Any?): Boolean =
    other is ChorebooConnectorGeneratedMutationImpl<*,*> &&
    other.connector == connector &&
    other.operationName == operationName &&
    other.dataDeserializer == dataDeserializer &&
    other.variablesSerializer == variablesSerializer

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "ChorebooConnectorGeneratedMutationImpl",
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun toString(): String =
    "ChorebooConnectorGeneratedMutationImpl(" +
    "operationName=$operationName, " +
    "dataDeserializer=$dataDeserializer, " +
    "variablesSerializer=$variablesSerializer, " +
    "connector=$connector)"
}



private class ArchiveHabitMutationImpl(
  connector: ChorebooConnector
):
  ArchiveHabitMutation,
  ChorebooConnectorGeneratedMutationImpl<
      ArchiveHabitMutation.Data,
      ArchiveHabitMutation.Variables
  >(
    connector,
    ArchiveHabitMutation.Companion.operationName,
    ArchiveHabitMutation.Companion.dataDeserializer,
    ArchiveHabitMutation.Companion.variablesSerializer,
  )


private class CreateHabitMutationImpl(
  connector: ChorebooConnector
):
  CreateHabitMutation,
  ChorebooConnectorGeneratedMutationImpl<
      CreateHabitMutation.Data,
      CreateHabitMutation.Variables
  >(
    connector,
    CreateHabitMutation.Companion.operationName,
    CreateHabitMutation.Companion.dataDeserializer,
    CreateHabitMutation.Companion.variablesSerializer,
  )


private class CreateHabitLogMutationImpl(
  connector: ChorebooConnector
):
  CreateHabitLogMutation,
  ChorebooConnectorGeneratedMutationImpl<
      CreateHabitLogMutation.Data,
      CreateHabitLogMutation.Variables
  >(
    connector,
    CreateHabitLogMutation.Companion.operationName,
    CreateHabitLogMutation.Companion.dataDeserializer,
    CreateHabitLogMutation.Companion.variablesSerializer,
  )


private class CreateHouseholdMutationImpl(
  connector: ChorebooConnector
):
  CreateHouseholdMutation,
  ChorebooConnectorGeneratedMutationImpl<
      CreateHouseholdMutation.Data,
      CreateHouseholdMutation.Variables
  >(
    connector,
    CreateHouseholdMutation.Companion.operationName,
    CreateHouseholdMutation.Companion.dataDeserializer,
    CreateHouseholdMutation.Companion.variablesSerializer,
  )


private class DeleteAllMyHabitLogsMutationImpl(
  connector: ChorebooConnector
):
  DeleteAllMyHabitLogsMutation,
  ChorebooConnectorGeneratedMutationImpl<
      DeleteAllMyHabitLogsMutation.Data,
      Unit
  >(
    connector,
    DeleteAllMyHabitLogsMutation.Companion.operationName,
    DeleteAllMyHabitLogsMutation.Companion.dataDeserializer,
    DeleteAllMyHabitLogsMutation.Companion.variablesSerializer,
  )


private class DeleteAllMyHabitsMutationImpl(
  connector: ChorebooConnector
):
  DeleteAllMyHabitsMutation,
  ChorebooConnectorGeneratedMutationImpl<
      DeleteAllMyHabitsMutation.Data,
      Unit
  >(
    connector,
    DeleteAllMyHabitsMutation.Companion.operationName,
    DeleteAllMyHabitsMutation.Companion.dataDeserializer,
    DeleteAllMyHabitsMutation.Companion.variablesSerializer,
  )


private class DeleteAllMyPurchasedBackgroundsMutationImpl(
  connector: ChorebooConnector
):
  DeleteAllMyPurchasedBackgroundsMutation,
  ChorebooConnectorGeneratedMutationImpl<
      DeleteAllMyPurchasedBackgroundsMutation.Data,
      Unit
  >(
    connector,
    DeleteAllMyPurchasedBackgroundsMutation.Companion.operationName,
    DeleteAllMyPurchasedBackgroundsMutation.Companion.dataDeserializer,
    DeleteAllMyPurchasedBackgroundsMutation.Companion.variablesSerializer,
  )


private class DeleteHabitMutationImpl(
  connector: ChorebooConnector
):
  DeleteHabitMutation,
  ChorebooConnectorGeneratedMutationImpl<
      DeleteHabitMutation.Data,
      DeleteHabitMutation.Variables
  >(
    connector,
    DeleteHabitMutation.Companion.operationName,
    DeleteHabitMutation.Companion.dataDeserializer,
    DeleteHabitMutation.Companion.variablesSerializer,
  )


private class DeleteHouseholdMutationImpl(
  connector: ChorebooConnector
):
  DeleteHouseholdMutation,
  ChorebooConnectorGeneratedMutationImpl<
      DeleteHouseholdMutation.Data,
      DeleteHouseholdMutation.Variables
  >(
    connector,
    DeleteHouseholdMutation.Companion.operationName,
    DeleteHouseholdMutation.Companion.dataDeserializer,
    DeleteHouseholdMutation.Companion.variablesSerializer,
  )


private class DeleteLogsForHabitMutationImpl(
  connector: ChorebooConnector
):
  DeleteLogsForHabitMutation,
  ChorebooConnectorGeneratedMutationImpl<
      DeleteLogsForHabitMutation.Data,
      DeleteLogsForHabitMutation.Variables
  >(
    connector,
    DeleteLogsForHabitMutation.Companion.operationName,
    DeleteLogsForHabitMutation.Companion.dataDeserializer,
    DeleteLogsForHabitMutation.Companion.variablesSerializer,
  )


private class DeleteMyChorebooMutationImpl(
  connector: ChorebooConnector
):
  DeleteMyChorebooMutation,
  ChorebooConnectorGeneratedMutationImpl<
      DeleteMyChorebooMutation.Data,
      Unit
  >(
    connector,
    DeleteMyChorebooMutation.Companion.operationName,
    DeleteMyChorebooMutation.Companion.dataDeserializer,
    DeleteMyChorebooMutation.Companion.variablesSerializer,
  )


private class DeleteMyUserMutationImpl(
  connector: ChorebooConnector
):
  DeleteMyUserMutation,
  ChorebooConnectorGeneratedMutationImpl<
      DeleteMyUserMutation.Data,
      Unit
  >(
    connector,
    DeleteMyUserMutation.Companion.operationName,
    DeleteMyUserMutation.Companion.dataDeserializer,
    DeleteMyUserMutation.Companion.variablesSerializer,
  )


private class GetAllMyHabitsQueryImpl(
  connector: ChorebooConnector
):
  GetAllMyHabitsQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetAllMyHabitsQuery.Data,
      Unit
  >(
    connector,
    GetAllMyHabitsQuery.Companion.operationName,
    GetAllMyHabitsQuery.Companion.dataDeserializer,
    GetAllMyHabitsQuery.Companion.variablesSerializer,
  )


private class GetCurrentUserQueryImpl(
  connector: ChorebooConnector
):
  GetCurrentUserQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetCurrentUserQuery.Data,
      Unit
  >(
    connector,
    GetCurrentUserQuery.Companion.operationName,
    GetCurrentUserQuery.Companion.dataDeserializer,
    GetCurrentUserQuery.Companion.variablesSerializer,
  )


private class GetHouseholdByInviteCodeQueryImpl(
  connector: ChorebooConnector
):
  GetHouseholdByInviteCodeQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetHouseholdByInviteCodeQuery.Data,
      GetHouseholdByInviteCodeQuery.Variables
  >(
    connector,
    GetHouseholdByInviteCodeQuery.Companion.operationName,
    GetHouseholdByInviteCodeQuery.Companion.dataDeserializer,
    GetHouseholdByInviteCodeQuery.Companion.variablesSerializer,
  )


private class GetHouseholdHabitLogsForDateQueryImpl(
  connector: ChorebooConnector
):
  GetHouseholdHabitLogsForDateQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetHouseholdHabitLogsForDateQuery.Data,
      GetHouseholdHabitLogsForDateQuery.Variables
  >(
    connector,
    GetHouseholdHabitLogsForDateQuery.Companion.operationName,
    GetHouseholdHabitLogsForDateQuery.Companion.dataDeserializer,
    GetHouseholdHabitLogsForDateQuery.Companion.variablesSerializer,
  )


private class GetLogsForHabitAndDateQueryImpl(
  connector: ChorebooConnector
):
  GetLogsForHabitAndDateQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetLogsForHabitAndDateQuery.Data,
      GetLogsForHabitAndDateQuery.Variables
  >(
    connector,
    GetLogsForHabitAndDateQuery.Companion.operationName,
    GetLogsForHabitAndDateQuery.Companion.dataDeserializer,
    GetLogsForHabitAndDateQuery.Companion.variablesSerializer,
  )


private class GetMyChorebooQueryImpl(
  connector: ChorebooConnector
):
  GetMyChorebooQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetMyChorebooQuery.Data,
      Unit
  >(
    connector,
    GetMyChorebooQuery.Companion.operationName,
    GetMyChorebooQuery.Companion.dataDeserializer,
    GetMyChorebooQuery.Companion.variablesSerializer,
  )


private class GetMyHouseholdQueryImpl(
  connector: ChorebooConnector
):
  GetMyHouseholdQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetMyHouseholdQuery.Data,
      Unit
  >(
    connector,
    GetMyHouseholdQuery.Companion.operationName,
    GetMyHouseholdQuery.Companion.dataDeserializer,
    GetMyHouseholdQuery.Companion.variablesSerializer,
  )


private class GetMyHouseholdChoreboosQueryImpl(
  connector: ChorebooConnector
):
  GetMyHouseholdChoreboosQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetMyHouseholdChoreboosQuery.Data,
      Unit
  >(
    connector,
    GetMyHouseholdChoreboosQuery.Companion.operationName,
    GetMyHouseholdChoreboosQuery.Companion.dataDeserializer,
    GetMyHouseholdChoreboosQuery.Companion.variablesSerializer,
  )


private class GetMyHouseholdHabitsQueryImpl(
  connector: ChorebooConnector
):
  GetMyHouseholdHabitsQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetMyHouseholdHabitsQuery.Data,
      Unit
  >(
    connector,
    GetMyHouseholdHabitsQuery.Companion.operationName,
    GetMyHouseholdHabitsQuery.Companion.dataDeserializer,
    GetMyHouseholdHabitsQuery.Companion.variablesSerializer,
  )


private class GetMyHouseholdMembersQueryImpl(
  connector: ChorebooConnector
):
  GetMyHouseholdMembersQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetMyHouseholdMembersQuery.Data,
      Unit
  >(
    connector,
    GetMyHouseholdMembersQuery.Companion.operationName,
    GetMyHouseholdMembersQuery.Companion.dataDeserializer,
    GetMyHouseholdMembersQuery.Companion.variablesSerializer,
  )


private class GetMyLogsForDateRangeQueryImpl(
  connector: ChorebooConnector
):
  GetMyLogsForDateRangeQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetMyLogsForDateRangeQuery.Data,
      GetMyLogsForDateRangeQuery.Variables
  >(
    connector,
    GetMyLogsForDateRangeQuery.Companion.operationName,
    GetMyLogsForDateRangeQuery.Companion.dataDeserializer,
    GetMyLogsForDateRangeQuery.Companion.variablesSerializer,
  )


private class GetMyPurchasedBackgroundsQueryImpl(
  connector: ChorebooConnector
):
  GetMyPurchasedBackgroundsQuery,
  ChorebooConnectorGeneratedQueryImpl<
      GetMyPurchasedBackgroundsQuery.Data,
      Unit
  >(
    connector,
    GetMyPurchasedBackgroundsQuery.Companion.operationName,
    GetMyPurchasedBackgroundsQuery.Companion.dataDeserializer,
    GetMyPurchasedBackgroundsQuery.Companion.variablesSerializer,
  )


private class InsertChorebooMutationImpl(
  connector: ChorebooConnector
):
  InsertChorebooMutation,
  ChorebooConnectorGeneratedMutationImpl<
      InsertChorebooMutation.Data,
      InsertChorebooMutation.Variables
  >(
    connector,
    InsertChorebooMutation.Companion.operationName,
    InsertChorebooMutation.Companion.dataDeserializer,
    InsertChorebooMutation.Companion.variablesSerializer,
  )


private class NullifyHouseholdForMembersMutationImpl(
  connector: ChorebooConnector
):
  NullifyHouseholdForMembersMutation,
  ChorebooConnectorGeneratedMutationImpl<
      NullifyHouseholdForMembersMutation.Data,
      NullifyHouseholdForMembersMutation.Variables
  >(
    connector,
    NullifyHouseholdForMembersMutation.Companion.operationName,
    NullifyHouseholdForMembersMutation.Companion.dataDeserializer,
    NullifyHouseholdForMembersMutation.Companion.variablesSerializer,
  )


private class PurchaseBackgroundMutationImpl(
  connector: ChorebooConnector
):
  PurchaseBackgroundMutation,
  ChorebooConnectorGeneratedMutationImpl<
      PurchaseBackgroundMutation.Data,
      PurchaseBackgroundMutation.Variables
  >(
    connector,
    PurchaseBackgroundMutation.Companion.operationName,
    PurchaseBackgroundMutation.Companion.dataDeserializer,
    PurchaseBackgroundMutation.Companion.variablesSerializer,
  )


private class UnarchiveHabitMutationImpl(
  connector: ChorebooConnector
):
  UnarchiveHabitMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UnarchiveHabitMutation.Data,
      UnarchiveHabitMutation.Variables
  >(
    connector,
    UnarchiveHabitMutation.Companion.operationName,
    UnarchiveHabitMutation.Companion.dataDeserializer,
    UnarchiveHabitMutation.Companion.variablesSerializer,
  )


private class UpdateAssignedHabitMutationImpl(
  connector: ChorebooConnector
):
  UpdateAssignedHabitMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateAssignedHabitMutation.Data,
      UpdateAssignedHabitMutation.Variables
  >(
    connector,
    UpdateAssignedHabitMutation.Companion.operationName,
    UpdateAssignedHabitMutation.Companion.dataDeserializer,
    UpdateAssignedHabitMutation.Companion.variablesSerializer,
  )


private class UpdateChorebooBackgroundMutationImpl(
  connector: ChorebooConnector
):
  UpdateChorebooBackgroundMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateChorebooBackgroundMutation.Data,
      UpdateChorebooBackgroundMutation.Variables
  >(
    connector,
    UpdateChorebooBackgroundMutation.Companion.operationName,
    UpdateChorebooBackgroundMutation.Companion.dataDeserializer,
    UpdateChorebooBackgroundMutation.Companion.variablesSerializer,
  )


private class UpdateChorebooFullMutationImpl(
  connector: ChorebooConnector
):
  UpdateChorebooFullMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateChorebooFullMutation.Data,
      UpdateChorebooFullMutation.Variables
  >(
    connector,
    UpdateChorebooFullMutation.Companion.operationName,
    UpdateChorebooFullMutation.Companion.dataDeserializer,
    UpdateChorebooFullMutation.Companion.variablesSerializer,
  )


private class UpdateChorebooSleepMutationImpl(
  connector: ChorebooConnector
):
  UpdateChorebooSleepMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateChorebooSleepMutation.Data,
      UpdateChorebooSleepMutation.Variables
  >(
    connector,
    UpdateChorebooSleepMutation.Companion.operationName,
    UpdateChorebooSleepMutation.Companion.dataDeserializer,
    UpdateChorebooSleepMutation.Companion.variablesSerializer,
  )


private class UpdateChorebooStatsMutationImpl(
  connector: ChorebooConnector
):
  UpdateChorebooStatsMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateChorebooStatsMutation.Data,
      UpdateChorebooStatsMutation.Variables
  >(
    connector,
    UpdateChorebooStatsMutation.Companion.operationName,
    UpdateChorebooStatsMutation.Companion.dataDeserializer,
    UpdateChorebooStatsMutation.Companion.variablesSerializer,
  )


private class UpdateChorebooXpMutationImpl(
  connector: ChorebooConnector
):
  UpdateChorebooXpMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateChorebooXpMutation.Data,
      UpdateChorebooXpMutation.Variables
  >(
    connector,
    UpdateChorebooXpMutation.Companion.operationName,
    UpdateChorebooXpMutation.Companion.dataDeserializer,
    UpdateChorebooXpMutation.Companion.variablesSerializer,
  )


private class UpdateOwnHabitMutationImpl(
  connector: ChorebooConnector
):
  UpdateOwnHabitMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateOwnHabitMutation.Data,
      UpdateOwnHabitMutation.Variables
  >(
    connector,
    UpdateOwnHabitMutation.Companion.operationName,
    UpdateOwnHabitMutation.Companion.dataDeserializer,
    UpdateOwnHabitMutation.Companion.variablesSerializer,
  )


private class UpdateUserHouseholdMutationImpl(
  connector: ChorebooConnector
):
  UpdateUserHouseholdMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateUserHouseholdMutation.Data,
      UpdateUserHouseholdMutation.Variables
  >(
    connector,
    UpdateUserHouseholdMutation.Companion.operationName,
    UpdateUserHouseholdMutation.Companion.dataDeserializer,
    UpdateUserHouseholdMutation.Companion.variablesSerializer,
  )


private class UpdateUserPointsMutationImpl(
  connector: ChorebooConnector
):
  UpdateUserPointsMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpdateUserPointsMutation.Data,
      UpdateUserPointsMutation.Variables
  >(
    connector,
    UpdateUserPointsMutation.Companion.operationName,
    UpdateUserPointsMutation.Companion.dataDeserializer,
    UpdateUserPointsMutation.Companion.variablesSerializer,
  )


private class UpsertUserMutationImpl(
  connector: ChorebooConnector
):
  UpsertUserMutation,
  ChorebooConnectorGeneratedMutationImpl<
      UpsertUserMutation.Data,
      UpsertUserMutation.Variables
  >(
    connector,
    UpsertUserMutation.Companion.operationName,
    UpsertUserMutation.Companion.dataDeserializer,
    UpsertUserMutation.Companion.variablesSerializer,
  )


