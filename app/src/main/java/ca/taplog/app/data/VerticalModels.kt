package ca.taplog.app.data

enum class TriggerModel {
    CALENDAR,
    PRE_USE,
    ENTRY_EVENT,
    ON_DEMAND,
    MILEAGE,
    ENGINE_HOURS
}

enum class ResultAction {
    NONE,
    REMOVE_FROM_SERVICE,
    NOTIFY_AUTHORITY,
    ISSUE_CERTIFICATE,
    DELIVER_REPORT
}

data class ResultOption(
    val code: String,
    val label: String,
    val action: ResultAction
)

data class TriggerConfig(
    val type: TriggerModel,
    val intervalMonths: Int? = null,
    val intervalMiles: Int? = null,
    val intervalHours: Int? = null
)

enum class RoleModel {
    SINGLE_INSPECTOR,
    MULTI_ROLE
}

enum class FieldType {
    TEXT,
    NUMBER,
    BOOLEAN,
    SINGLE_SELECT,
    MULTI_SELECT,
    DATE,
    PHOTO
}

data class FormField(
    val key: String,
    val label: String,
    val type: FieldType,
    val required: Boolean,
    val applicableAssetTypes: List<String> = emptyList()
)

data class InspectionFormProfile(
    val resultOptions: List<ResultOption>,
    val fields: List<FormField>,
    val requiresPermit: Boolean = false,
    val requiresWitness: Boolean = false,
    val deficienciesEnabled: Boolean = true,
    val photoRequired: Boolean = false
)

data class VerticalAssetType(
    val code: String,
    val label: String,
    val description: String,
    val triggerConfig: TriggerConfig,
    val checklistItems: List<String>,
    val defaultFormFields: List<String> = emptyList()
)

data class VerticalConfig(
    val vertical: TapLogVertical,
    val displayName: String,
    val shortName: String,
    val regulatoryFramework: String,
    val triggerModel: TriggerModel,
    val roleModel: RoleModel,
    val formProfile: InspectionFormProfile,
    val assetTypeRegistry: List<VerticalAssetType>
)

object VerticalRegistry {
    private val registry = mutableMapOf<TapLogVertical, VerticalConfig>()

    fun register(config: VerticalConfig) {
        registry[config.vertical] = config
    }

    fun get(vertical: TapLogVertical): VerticalConfig =
        registry[vertical] ?: error("Vertical not registered: $vertical")

    fun all(): List<VerticalConfig> = registry.values.toList()

    fun isRegistered(vertical: TapLogVertical): Boolean = registry.containsKey(vertical)

    fun count(): Int = registry.size
}
