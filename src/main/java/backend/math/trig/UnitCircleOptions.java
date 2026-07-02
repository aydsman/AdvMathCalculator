package backend.math.trig;

/**
 * Optional sections shown in the unit-circle Calculate panel.
 */
public record UnitCircleOptions(
        boolean coordinates,
        boolean referenceAngle,
        boolean reciprocals,
        boolean coterminal,
        boolean quadrant,
        boolean convertAngle) {

    public static UnitCircleOptions allEnabled() {
        return new UnitCircleOptions(true, true, true, true, true, true);
    }
}
