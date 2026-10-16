package com.tehgeii.studenttoolkit.domain.tools

/** A unit defined by its factor to the category's base unit (value × factor = base value). */
data class UnitDef(val symbol: String, val name: String, val factor: Double)

enum class UnitCategory(val units: List<UnitDef>) {
    LENGTH(
        listOf(
            UnitDef("mm", "Millimeter", 0.001),
            UnitDef("cm", "Centimeter", 0.01),
            UnitDef("m", "Meter", 1.0),
            UnitDef("km", "Kilometer", 1000.0),
            UnitDef("in", "Inch", 0.0254),
            UnitDef("ft", "Foot", 0.3048),
            UnitDef("yd", "Yard", 0.9144),
            UnitDef("mi", "Mile", 1609.344),
            UnitDef("nmi", "Nautical mile", 1852.0),
        ),
    ),
    WEIGHT(
        listOf(
            UnitDef("mg", "Milligram", 1e-6),
            UnitDef("g", "Gram", 0.001),
            UnitDef("ons", "Ons (100 g)", 0.1),
            UnitDef("kg", "Kilogram", 1.0),
            UnitDef("t", "Tonne", 1000.0),
            UnitDef("oz", "Ounce", 0.028349523125),
            UnitDef("lb", "Pound", 0.45359237),
        ),
    ),
    TEMPERATURE(
        listOf(
            UnitDef("°C", "Celsius", 1.0),
            UnitDef("°F", "Fahrenheit", 1.0),
            UnitDef("K", "Kelvin", 1.0),
        ),
    ),
    TIME(
        listOf(
            UnitDef("ms", "Millisecond", 0.001),
            UnitDef("s", "Second", 1.0),
            UnitDef("min", "Minute", 60.0),
            UnitDef("h", "Hour", 3600.0),
            UnitDef("d", "Day", 86_400.0),
            UnitDef("wk", "Week", 604_800.0),
            UnitDef("yr", "Year (365.25 d)", 31_557_600.0),
        ),
    ),
    STORAGE(
        listOf(
            UnitDef("bit", "Bit", 0.125),
            UnitDef("B", "Byte", 1.0),
            UnitDef("kB", "Kilobyte (SI, 1000 B)", 1e3),
            UnitDef("MB", "Megabyte (SI)", 1e6),
            UnitDef("GB", "Gigabyte (SI)", 1e9),
            UnitDef("TB", "Terabyte (SI)", 1e12),
            UnitDef("KiB", "Kibibyte (IEC, 1024 B)", 1024.0),
            UnitDef("MiB", "Mebibyte (IEC)", 1_048_576.0),
            UnitDef("GiB", "Gibibyte (IEC)", 1_073_741_824.0),
            UnitDef("TiB", "Tebibyte (IEC)", 1_099_511_627_776.0),
        ),
    ),
    DATA_RATE(
        listOf(
            UnitDef("bps", "Bit per second", 1.0),
            UnitDef("kbps", "Kilobit per second", 1e3),
            UnitDef("Mbps", "Megabit per second", 1e6),
            UnitDef("Gbps", "Gigabit per second", 1e9),
            UnitDef("B/s", "Byte per second", 8.0),
            UnitDef("kB/s", "Kilobyte per second", 8e3),
            UnitDef("MB/s", "Megabyte per second", 8e6),
            UnitDef("MiB/s", "Mebibyte per second", 8.0 * 1_048_576.0),
        ),
    ),
    SPEED(
        listOf(
            UnitDef("m/s", "Meter per second", 1.0),
            UnitDef("km/h", "Kilometer per hour", 1.0 / 3.6),
            UnitDef("mph", "Mile per hour", 0.44704),
            UnitDef("kn", "Knot", 1852.0 / 3600.0),
            UnitDef("ft/s", "Foot per second", 0.3048),
        ),
    ),
    AREA(
        listOf(
            UnitDef("mm²", "Square millimeter", 1e-6),
            UnitDef("cm²", "Square centimeter", 1e-4),
            UnitDef("m²", "Square meter", 1.0),
            UnitDef("ha", "Hectare", 1e4),
            UnitDef("km²", "Square kilometer", 1e6),
            UnitDef("in²", "Square inch", 0.00064516),
            UnitDef("ft²", "Square foot", 0.09290304),
            UnitDef("ac", "Acre", 4046.8564224),
        ),
    ),
    VOLUME(
        listOf(
            UnitDef("mL", "Milliliter", 0.001),
            UnitDef("cm³", "Cubic centimeter", 0.001),
            UnitDef("L", "Liter", 1.0),
            UnitDef("m³", "Cubic meter", 1000.0),
            UnitDef("tsp", "Teaspoon (US)", 0.00492892159375),
            UnitDef("tbsp", "Tablespoon (US)", 0.01478676478125),
            UnitDef("fl oz", "Fluid ounce (US)", 0.0295735295625),
            UnitDef("cup", "Cup (US)", 0.2365882365),
            UnitDef("gal", "Gallon (US)", 3.785411784),
        ),
    ),
}

object UnitConverter {
    fun convert(value: Double, category: UnitCategory, from: UnitDef, to: UnitDef): Double {
        if (category == UnitCategory.TEMPERATURE) return fromKelvin(toKelvin(value, from.symbol), to.symbol)
        return value * from.factor / to.factor
    }

    private fun toKelvin(value: Double, symbol: String): Double = when (symbol) {
        "°C" -> value + 273.15
        "°F" -> (value - 32.0) * 5.0 / 9.0 + 273.15
        else -> value
    }

    private fun fromKelvin(kelvin: Double, symbol: String): Double = when (symbol) {
        "°C" -> kelvin - 273.15
        "°F" -> (kelvin - 273.15) * 9.0 / 5.0 + 32.0
        else -> kelvin
    }

    /** Kelvin cannot go below absolute zero. */
    fun isValid(value: Double, category: UnitCategory, from: UnitDef): Boolean =
        category != UnitCategory.TEMPERATURE || toKelvin(value, from.symbol) >= -1e-9
}
