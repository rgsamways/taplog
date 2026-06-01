package ca.taplog.app.data

data class OFCAssetType(
    val code: String,
    val label: String,
    val inspectionIntervalMonths: Int,
    val description: String,
    val checklistItems: List<String> = emptyList()
)

enum class OFCCategory(
    val label: String,
    val types: List<OFCAssetType>
) {

    PORTABLE_EXTINGUISHERS(
        label = "Portable Extinguishers",
        types = listOf(
            OFCAssetType(
                "EXT_ABC", "ABC Dry Chemical", 12,
                "Red cylinder, wall-mounted or in cabinet. Most common type. Fights ordinary fires, flammable liquids, and electrical fires.",
                listOf(
                    "Verify pull pin and tamper seal are intact",
                    "Confirm pressure gauge needle is in the green zone",
                    "Inspect cylinder for dents, corrosion, bulging, or physical damage",
                    "Check label is legible, complete, and shows correct service date",
                    "Confirm mount or bracket is secure and accessible with no obstructions",
                    "Inspect discharge horn and nozzle — no blockage, cracks, or damage",
                    "Verify annual inspection tag is current (within 12 months)"
                )
            ),
            OFCAssetType(
                "EXT_BC", "BC Dry Chemical", 12,
                "Red cylinder, similar to ABC. For flammable liquids and electrical fires only — not for wood or paper fires.",
                listOf(
                    "Verify pull pin and tamper seal are intact",
                    "Confirm pressure gauge needle is in the green zone",
                    "Inspect cylinder for dents, corrosion, or physical damage",
                    "Check label is legible and shows correct service date",
                    "Confirm mount or bracket is secure and accessible",
                    "Inspect discharge horn and nozzle — no blockage or damage",
                    "Verify annual inspection tag is current (within 12 months)"
                )
            ),
            OFCAssetType(
                "EXT_CO2", "Carbon Dioxide (CO₂)", 12,
                "Red cylinder with a black band and flared horn nozzle. No residue — common in server rooms and kitchens.",
                listOf(
                    "Verify pull pin and tamper seal are intact",
                    "Check gross weight — compare to stencilled nameplate weight (within 10%)",
                    "Inspect discharge horn for cracks, blockage, or missing anti-static boot",
                    "Inspect cylinder for corrosion, damage, or missing hydrostatic test date",
                    "Confirm mount or bracket is secure and location is accessible",
                    "Verify annual inspection tag is current (within 12 months)"
                )
            ),
            OFCAssetType(
                "EXT_WETK", "Wet Chemical — Class K", 6,
                "Tall, narrow cylinder, often stainless or red. Found in commercial kitchens near deep fryers and cooking equipment.",
                listOf(
                    "Verify pull pin and tamper seal are intact",
                    "Confirm pressure gauge needle is in the green zone",
                    "Inspect cylinder for corrosion or leaks at the valve",
                    "Check nozzle and applicator tube are clean and unobstructed",
                    "Confirm monthly maintenance tag is current",
                    "Verify 6-month service interval tag is current",
                    "Confirm unit is located at kitchen exit — not directly adjacent to cooking equipment"
                )
            ),
            OFCAssetType(
                "EXT_WATER", "Water / Water Mist", 12,
                "Red cylinder, chrome or silver finish. Water or fine mist. For ordinary combustibles only — never use on electrical or grease fires.",
                listOf(
                    "Verify pull pin and tamper seal are intact",
                    "Confirm pressure gauge needle is in the green zone (stored-pressure type)",
                    "Inspect cylinder for dents, corrosion, or physical damage",
                    "Check nozzle is unobstructed and undamaged",
                    "Confirm unit is not stored in an area subject to freezing temperatures",
                    "Verify annual inspection tag is current (within 12 months)"
                )
            ),
            OFCAssetType(
                "EXT_FOAM", "Foam (AFFF)", 12,
                "Red cylinder with foam label. For flammable liquid spills. Leaves a blanket of foam over the fire.",
                listOf(
                    "Verify pull pin and tamper seal are intact",
                    "Confirm pressure gauge needle is in the green zone",
                    "Inspect cylinder for dents, corrosion, or physical damage",
                    "Check nozzle is unobstructed and undamaged",
                    "Confirm label identifies as AFFF foam and is legible",
                    "Verify annual inspection tag is current (within 12 months)"
                )
            ),
            OFCAssetType(
                "EXT_HALON", "Halogenated Agent (Halon)", 12,
                "Red or green cylinder, older installation. Clean agent — leaves no residue. Being phased out due to environmental regulations.",
                listOf(
                    "Verify pull pin and tamper seal are intact",
                    "Confirm pressure gauge reading is within specified range (if gauge-equipped)",
                    "Check gross weight — compare to nameplate weight within 5%",
                    "Inspect cylinder for corrosion or physical damage",
                    "Note: Halon is being phased out — confirm replacement plan per Canadian regulations",
                    "Verify annual inspection tag is current (within 12 months)"
                )
            ),
            OFCAssetType(
                "EXT_CLEAN", "Clean Agent", 12,
                "Red or grey cylinder with coloured band. Modern replacement for Halon. No residue — used in server rooms and archives.",
                listOf(
                    "Verify pull pin and tamper seal are intact",
                    "Confirm pressure gauge needle is in the green zone",
                    "Check weight or pressure against nameplate specification",
                    "Inspect cylinder and discharge nozzle for damage or blockage",
                    "Confirm label identifies agent type and is legible",
                    "Verify annual inspection tag is current (within 12 months)"
                )
            )
        )
    ),

    SUPPRESSION_SYSTEMS(
        label = "Suppression Systems",
        types = listOf(
            OFCAssetType(
                "SUP_SPRINK", "Sprinkler System", 12,
                "Network of pipes in the ceiling with sprinkler heads — small brass or chrome fittings. Activates individually over heat.",
                listOf(
                    "Verify main control valve is in the open (normal) position and is locked or supervised",
                    "Check alarm valve and pressure gauges — confirm pressure is within normal range",
                    "Inspect visible sprinkler heads for paint, corrosion, or physical damage",
                    "Confirm no obstructions within 18 inches below any sprinkler head",
                    "Verify inspector test valve flows freely and drain is clear",
                    "Check all control valves are in the correct position and tamper switches are functional",
                    "Confirm date of last flow test is recorded in system log"
                )
            ),
            OFCAssetType(
                "SUP_STANDPIPE", "Standpipe & Hose System", 12,
                "Red valve cabinet on the wall, usually in stairwells. Contains a hose connection for fire department or occupant use.",
                listOf(
                    "Verify cabinet is accessible and unlocked per local authority requirements",
                    "Inspect hose for mildew, damage, or deterioration (occupant-use systems)",
                    "Confirm hose connections (2.5-inch and/or 1.5-inch) have caps and are undamaged",
                    "Check valve opens smoothly and closes fully",
                    "Verify pressure reducing valve (if equipped) is within required range",
                    "Confirm location signage is visible and legible"
                )
            ),
            OFCAssetType(
                "SUP_KITCHEN", "Kitchen Hood Suppression", 6,
                "Nozzles inside the exhaust hood above commercial cooking equipment. Automatically discharges over grease fires.",
                listOf(
                    "Verify system is not in manual pull-out or lockout state",
                    "Check agent cylinder pressure gauges — confirm all are in the green zone",
                    "Inspect fusible links — confirm no corrosion, paint coating, or grease accumulation",
                    "Verify nozzle caps are intact and nozzles are aimed at cooking surfaces",
                    "Confirm gas and electric interlock test performed within the last 6 months",
                    "Check service tag shows 6-month interval is current"
                )
            ),
            OFCAssetType(
                "SUP_CLEAN", "Clean Agent System", 12,
                "Pipes and nozzles in ceiling of a protected room (server room, archive). Floods the space with clean gas to suppress fire.",
                listOf(
                    "Verify agent cylinder pressure and weight against nameplate specifications",
                    "Check all nozzles are uncovered and unobstructed",
                    "Confirm room integrity — inspect for new penetrations in walls, ceiling, or floor",
                    "Verify audible and visual pre-discharge warning devices are functional",
                    "Confirm manual release and abort station are accessible and labeled",
                    "Check system control panel is in AUTO mode"
                )
            ),
            OFCAssetType(
                "SUP_CO2SYS", "CO₂ Suppression System", 12,
                "Pipes and nozzles similar to clean agent but uses carbon dioxide. Industrial environments — hazardous to occupants.",
                listOf(
                    "Verify agent cylinder weight against nameplate gross weight",
                    "Check all nozzles are unobstructed",
                    "Confirm room integrity — no penetrations that would compromise hold time",
                    "Verify CO₂ concentration warning signs are posted at all room entries",
                    "Confirm pre-discharge alarm and abort controls are functional",
                    "Verify lock-out/tag-out procedures are documented and posted"
                )
            ),
            OFCAssetType(
                "SUP_FOAM", "Foam Suppression System", 12,
                "Pipes and foam nozzles, typically in aircraft hangars or fuel storage areas. Blankets fuel fires with foam.",
                listOf(
                    "Check foam concentrate tank level against minimum required volume",
                    "Verify proportioner is operational and calibration is current",
                    "Inspect foam nozzles and deflectors for blockage or corrosion",
                    "Confirm detection and activation system is in auto mode",
                    "Check all control valve positions are correct"
                )
            ),
            OFCAssetType(
                "SUP_DRYPIPE", "Dry Pipe Sprinkler System", 12,
                "Like a wet sprinkler system but pipes contain pressurized air, not water. Used in unheated spaces to prevent freezing.",
                listOf(
                    "Verify supervisory air pressure in dry pipe system is within specified range (typically 40 psi)",
                    "Confirm water supply control valve is open and supervised",
                    "Check dry pipe valve is in the set position",
                    "Verify low-point drains are accessible for scheduled blowdown",
                    "Confirm heated enclosures for dry pipe valves are maintained above freezing",
                    "Check date of last trip test is recorded in the system log"
                )
            ),
            OFCAssetType(
                "SUP_PREACT", "Pre-Action Sprinkler System", 12,
                "Two-step sprinkler — requires both heat and a separate detector signal before water flows. Used in data centres.",
                listOf(
                    "Verify air or nitrogen supervisory pressure is within specified range",
                    "Confirm water supply valve is open and supervised",
                    "Check pre-action control panel is in normal mode — no active troubles or alarms",
                    "Verify detection system integrated with pre-action valve is tested within the last 12 months",
                    "Confirm solenoid valve and deluge valve are operational"
                )
            )
        )
    ),

    DETECTION_AND_ALARM(
        label = "Detection & Alarm",
        types = listOf(
            OFCAssetType(
                "DET_FACP", "Fire Alarm Control Panel", 12,
                "Large metal box on the wall, typically red or grey, with a display, LEDs, and keypad. The brain of the fire alarm system.",
                listOf(
                    "Verify panel is in normal mode — no active alarms, troubles, or supervisory signals",
                    "Confirm all zones are enabled and zone labels are accurate",
                    "Check battery backup — confirm charge status indicator is normal",
                    "Confirm AC power indicator is lit",
                    "Verify panel clock is accurate",
                    "Review event log for recent unexplained faults or alarms",
                    "Confirm panel is accessible and enclosure is secure"
                )
            ),
            OFCAssetType(
                "DET_SMOKE", "Smoke Detector", 12,
                "Round white or cream disc on the ceiling. Detects smoke particles. May have a blinking LED.",
                listOf(
                    "Visually inspect for physical damage, discolouration, or contamination",
                    "Confirm LED indicator blinks at normal standby interval",
                    "Test sensitivity using listed aerosol or magnet tester per manufacturer spec",
                    "Verify detector activates and FACP registers the alarm",
                    "Confirm detector resets to normal after test",
                    "Check for nearby obstructions or supply air vents within 3 feet"
                )
            ),
            OFCAssetType(
                "DET_HEAT", "Heat Detector", 12,
                "Round white disc on the ceiling, similar to smoke detector. Activates at a fixed temperature — used where smoke detectors would false-alarm.",
                listOf(
                    "Visually inspect for physical damage, corrosion, or contamination",
                    "Confirm LED indicator blinks at normal standby interval",
                    "Note detector type from label — rate-of-rise or fixed temperature",
                    "Verify location is appropriate and not above heat sources that could cause nuisance alarms",
                    "Test using listed heat gun per manufacturer specification",
                    "Confirm FACP registers the alarm and detector resets normally"
                )
            ),
            OFCAssetType(
                "DET_PULL", "Manual Pull Station", 12,
                "Red box on the wall near exits, chest height. Pull the handle to trigger the alarm. Often has a plastic cover to prevent accidental activation.",
                listOf(
                    "Inspect for physical damage, paint-over, or obstructions",
                    "Verify pull station is accessible — unobstructed within 5 feet at chest height",
                    "Confirm dual-action mechanism (if applicable) operates correctly per manufacturer spec",
                    "Check reset key is available on-site",
                    "Verify identification signage is visible and legible"
                )
            ),
            OFCAssetType(
                "DET_HORN", "Horn / Strobe Notification", 12,
                "Wall or ceiling-mounted device with a horn or speaker and a bright flashing strobe light. Alerts occupants during an alarm.",
                listOf(
                    "Visually inspect for physical damage or blockage of horn or strobe lens",
                    "Verify strobe is unobstructed and visible from required positions in the space",
                    "Confirm device activates correctly during full-system alarm test",
                    "Check that candela rating is marked and is appropriate for the space dimensions",
                    "Verify wire connections are secure (visual check at accessible junction box)"
                )
            ),
            OFCAssetType(
                "DET_CO", "Carbon Monoxide Detector", 12,
                "White or cream unit on the wall or ceiling, similar in appearance to a smoke detector. Detects CO gas — not smoke.",
                listOf(
                    "Visually inspect for damage and confirm indicator light shows normal standby state",
                    "Test using listed CO test gas or manufacturer test button",
                    "Confirm unit is not within 5 feet of gas appliances or cooking equipment",
                    "Verify unit end-of-life date (printed on back) has not passed"
                )
            ),
            OFCAssetType(
                "DET_DUCT", "Duct Smoke Detector", 12,
                "Detector mounted on an HVAC duct with sampling tubes extending into the air stream. Shuts down air handling on smoke detection.",
                listOf(
                    "Visually inspect sampling tubes for blockage or physical damage",
                    "Verify cover is properly sealed and unit is accessible for service",
                    "Test sensitivity using listed aerosol introduced through sampling tube port",
                    "Confirm air handling unit shuts down on test activation",
                    "Reset and confirm normal operation is restored"
                )
            ),
            OFCAssetType(
                "DET_BEAM", "Projected Beam Detector", 12,
                "Two units mounted across a large open space (warehouses, atriums) — a transmitter and a receiver. Detects smoke crossing the beam.",
                listOf(
                    "Verify transmitter and receiver are properly aligned — confirm alignment indicator on receiver",
                    "Check beam path is clear and unobstructed along full travel distance",
                    "Confirm sensitivity setting has not drifted — check signal strength reading if display is available",
                    "Test using listed neutral density filter set to simulate obscuration",
                    "Verify both units are securely mounted with no visible vibration or movement"
                )
            )
        )
    ),

    EMERGENCY_LIGHTING(
        label = "Emergency Lighting & Exit Signs",
        types = listOf(
            OFCAssetType(
                "EMRG_UNIT", "Emergency Lighting Unit", 6,
                "White box on the wall with two forward-facing bulbs or LED heads. Turns on automatically when power fails.",
                listOf(
                    "Verify indicator light shows unit is on charge — AC power present",
                    "Perform 30-second function test — confirm both lamp heads illuminate",
                    "Confirm lamps are not discoloured, burned out, or missing",
                    "Check unit is unobstructed and lamp heads illuminate the intended egress path",
                    "Record date of last annual 90-minute duration test in service log"
                )
            ),
            OFCAssetType(
                "EMRG_EXIT", "Exit Sign", 12,
                "Illuminated green or red sign reading EXIT above a door or in a corridor. May be self-luminous (no power needed) or wired.",
                listOf(
                    "Verify sign is illuminated and legible from 30 metres",
                    "Confirm arrow direction matches the correct egress route",
                    "Check sign face for cracks, yellowing, or missing letters",
                    "For wired types: confirm AC indicator (green LED) is lit",
                    "For self-luminous types: confirm luminosity has not degraded below visible threshold"
                )
            ),
            OFCAssetType(
                "EMRG_COMBO", "Combo Exit / Emergency Light", 6,
                "EXIT sign with emergency light heads attached. Combines both functions in one unit.",
                listOf(
                    "Verify AC power indicator is lit",
                    "Confirm exit text is illuminated and legible from 30 metres",
                    "Perform 30-second function test — confirm lamp heads illuminate",
                    "Confirm arrow direction matches the correct egress route",
                    "Check for physical damage to sign face or lamp heads",
                    "Record date of last annual 90-minute duration test in service log"
                )
            ),
            OFCAssetType(
                "EMRG_CENT", "Central Battery System", 12,
                "Rack or cabinet in an electrical room that powers all emergency lighting in the building from a central battery bank.",
                listOf(
                    "Verify AC power is present to the charger and charger is operational",
                    "Check battery charge status indicator — confirm normal",
                    "Confirm all output circuits show normal status on system panel",
                    "Review maintenance log for date of last load test",
                    "Inspect battery terminal connections for corrosion or loose connections"
                )
            )
        )
    ),

    FIRE_DOORS_AND_CLOSERS(
        label = "Fire Doors & Closers",
        types = listOf(
            OFCAssetType(
                "DOOR_FIRE", "Fire Door Assembly", 12,
                "Heavy door with a fire rating label on the edge — 20, 45, 60, or 90 minute rating. Must self-close and latch.",
                listOf(
                    "Verify door self-closes and latches fully without manual assistance from full-open position",
                    "Inspect door face, edges, and frame for damage, gaps, warping, or missing hardware",
                    "Confirm fire rating label is present and legible on the door edge",
                    "Check intumescent strip and smoke seal are continuous and undamaged",
                    "Verify no unauthorized holes, penetrations, or modifications",
                    "Confirm hold-open device (if present) releases and door closes on smoke or alarm signal"
                )
            ),
            OFCAssetType(
                "DOOR_CLOSER", "Door Closer", 12,
                "Mechanical arm mounted on the top of a door that pulls it closed automatically. Silver or black, rectangular body.",
                listOf(
                    "Verify closer arm is securely attached to both door and frame brackets",
                    "Confirm door closes fully from full-open position without manual assistance",
                    "Check closing speed — door should latch within 3 to 4 seconds without slamming",
                    "Inspect closer body for oil leaks or physical damage",
                    "Confirm closer provides positive latching against the door frame"
                )
            ),
            OFCAssetType(
                "DOOR_RELEASE", "Electromagnetic Door Hold-Open", 12,
                "Magnet mounted on wall or floor that holds a fire door open. Releases automatically on alarm to let the door close.",
                listOf(
                    "Verify magnet holds door open under normal operating conditions",
                    "Test release: activate fire alarm or disconnect power — confirm door closes and latches fully",
                    "Inspect magnet face and armature plate for corrosion or physical damage",
                    "Confirm unit resets and re-engages when alarm is cleared",
                    "Verify power supply wiring is secure and conduit is undamaged"
                )
            ),
            OFCAssetType(
                "DOOR_DAMPER", "Fire Damper", 12,
                "Metal flap inside a duct where it passes through a fire-rated wall or floor. Closes automatically in a fire to stop flame spread.",
                listOf(
                    "Verify damper blade is in the open position during normal operation",
                    "Test fusible link release per scheduled maintenance (typically annually)",
                    "Confirm damper blade fully closes on activation with no gaps",
                    "Check for corrosion, obstructions, or physical damage to blade and frame",
                    "Verify damper access door is accessible and labeled for service",
                    "Reset and confirm damper reopens correctly if motorized"
                )
            ),
            OFCAssetType(
                "DOOR_SMOKE", "Smoke Damper", 12,
                "Similar to a fire damper but closes on smoke detection rather than heat. Controls smoke movement through ductwork.",
                listOf(
                    "Verify damper blade is in the open position during normal operation",
                    "Confirm damper is supervised and connected to building fire alarm system",
                    "Test activation from fire alarm panel — confirm damper closes fully",
                    "Check for corrosion or obstruction in blade and frame",
                    "Verify end switch or position indicator confirms closed position on activation"
                )
            )
        )
    ),

    FIREFIGHTING_EQUIPMENT(
        label = "Firefighting Equipment",
        types = listOf(
            OFCAssetType(
                "FFE_HOSECAB", "Fire Hose Cabinet", 12,
                "Red metal cabinet on the wall containing a coiled fire hose and nozzle. For occupant or fire department use.",
                listOf(
                    "Verify cabinet is accessible and not blocked by storage or equipment",
                    "Inspect hose for mildew, tears, kinks, or deterioration",
                    "Confirm nozzle is present and undamaged",
                    "Check valve opens freely and closes fully without leakage",
                    "Verify hose is properly racked or rolled for deployment",
                    "Confirm cabinet label and signage are visible and legible"
                )
            ),
            OFCAssetType(
                "FFE_HYDRANT", "Private Fire Hydrant", 12,
                "Yellow or red hydrant on the property, separate from the municipal hydrant at the street. Feeds the building's fire suppression system.",
                listOf(
                    "Visually inspect for physical damage, corrosion, or missing outlet caps",
                    "Confirm outlet caps are present and removable",
                    "Verify hydrant is accessible with minimum 3-metre clearance on all sides",
                    "Confirm flow and pressure tested within the last 12 months",
                    "Verify paint colour matches jurisdiction colour coding for flow rate class",
                    "Check drain valve is operating correctly — no standing water around base"
                )
            ),
            OFCAssetType(
                "FFE_SIAMESE", "Fire Department Connection (Siamese)", 12,
                "Two-inlet brass fitting on the exterior of the building, near the entrance. Fire department connects hoses here to boost suppression system pressure.",
                listOf(
                    "Inspect both inlets — confirm caps or plugs are present and removable",
                    "Check inlets for corrosion, internal damage, or blockage",
                    "Verify signage identifies the system being served (sprinkler or standpipe)",
                    "Confirm location is accessible from the street and unobstructed",
                    "Verify check valve prevents backflow — test if service interval requires it"
                )
            ),
            OFCAssetType(
                "FFE_RISER", "Sprinkler Riser / Control Valve", 12,
                "Large pipe and valve assembly in a mechanical room or stairwell. The main shutoff and test point for the sprinkler system.",
                listOf(
                    "Verify main control valve is in the fully open position",
                    "Check pressure gauge — confirm static pressure is within acceptable range",
                    "Confirm flow switch and tamper switch are connected and supervised by FACP",
                    "Verify drain valve can be operated and flows freely",
                    "Check riser pipe for corrosion, leaks, or physical damage",
                    "Confirm riser area is accessible and equipment is properly labeled"
                )
            )
        )
    );

    companion object {
        private val byCode: Map<String, OFCAssetType> by lazy {
            entries.flatMap { it.types }.associateBy { it.code }
        }

        fun findByCode(code: String): OFCAssetType? = byCode[code]

        fun labelForCode(code: String): String =
            byCode[code]?.label ?: code
    }
}
