package ca.taplog.app.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    // List<String>
    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type)

    // VerticalConfig (stored as JSON blob in vertical_configs table)
    @TypeConverter
    fun fromVerticalConfig(value: VerticalConfig?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toVerticalConfig(value: String?): VerticalConfig? =
        value?.let { gson.fromJson(it, VerticalConfig::class.java) }


    // InspectionResult
    @TypeConverter
    fun fromInspectionResult(value: InspectionResult): String = value.name

    @TypeConverter
    fun toInspectionResult(value: String): InspectionResult = InspectionResult.valueOf(value)

    // DeficiencySeverity
    @TypeConverter
    fun fromDeficiencySeverity(value: DeficiencySeverity): String = value.name

    @TypeConverter
    fun toDeficiencySeverity(value: String): DeficiencySeverity = DeficiencySeverity.valueOf(value)

    // SubscriptionTier
    @TypeConverter
    fun fromSubscriptionTier(value: SubscriptionTier): String = value.name

    @TypeConverter
    fun toSubscriptionTier(value: String): SubscriptionTier = SubscriptionTier.valueOf(value)

    // SubscriptionStatus
    @TypeConverter
    fun fromSubscriptionStatus(value: SubscriptionStatus): String = value.name

    @TypeConverter
    fun toSubscriptionStatus(value: String): SubscriptionStatus = SubscriptionStatus.valueOf(value)

    // RetireReason
    @TypeConverter
    fun fromRetireReason(value: RetireReason?): String? = value?.name

    @TypeConverter
    fun toRetireReason(value: String?): RetireReason? = value?.let { RetireReason.valueOf(it) }

    // ScanEventType
    @TypeConverter
    fun fromScanEventType(value: ScanEventType): String = value.name

    @TypeConverter
    fun toScanEventType(value: String): ScanEventType = ScanEventType.valueOf(value)

    // TapLogVertical
    @TypeConverter
    fun fromTapLogVertical(value: TapLogVertical): String = value.name

    @TypeConverter
    fun toTapLogVertical(value: String): TapLogVertical = TapLogVertical.valueOf(value)

    // TagEventRole
    @TypeConverter
    fun fromTagEventRole(value: TagEventRole): String = value.name

    @TypeConverter
    fun toTagEventRole(value: String): TagEventRole = TagEventRole.valueOf(value)
}