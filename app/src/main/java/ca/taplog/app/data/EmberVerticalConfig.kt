package ca.taplog.app.data

object EmberVerticalConfig {

    fun build(): VerticalConfig = VerticalConfig(
        vertical = TapLogVertical.EMBER,
        displayName = "Ember — Fire Safety",
        shortName = "Ember",
        regulatoryFramework = "Ontario Fire Code / CAN/ULC-S536",
        triggerModel = TriggerModel.CALENDAR,
        roleModel = RoleModel.SINGLE_INSPECTOR,
        formProfile = InspectionFormProfile(
            resultOptions = listOf(
                ResultOption("PASS", "Pass", ResultAction.NONE),
                ResultOption("REQUIRES_ATTENTION", "Requires Attention", ResultAction.NONE),
                ResultOption("FAIL", "Fail", ResultAction.NONE)
            ),
            fields = listOf(
                FormField(
                    key = "notes",
                    label = "Notes",
                    type = FieldType.TEXT,
                    required = false
                )
            ),
            deficienciesEnabled = true,
            photoRequired = false
        ),
        assetTypeRegistry = OFCCategory.entries.flatMap { category ->
            category.types.map { type ->
                VerticalAssetType(
                    code = type.code,
                    label = type.label,
                    description = type.description,
                    triggerConfig = TriggerConfig(
                        type = TriggerModel.CALENDAR,
                        intervalMonths = type.inspectionIntervalMonths
                    ),
                    checklistItems = type.checklistItems
                )
            }
        }
    )
}
