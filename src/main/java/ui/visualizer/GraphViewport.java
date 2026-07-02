package ui.visualizer;

/** Maps math coordinates to canvas pixels with a uniform scale so grid cells are square. */
final class GraphViewport {

    static final double DEFAULT_MIN = -20;
    static final double DEFAULT_MAX = 20;
    static final double MINI_MIN = -5;
    static final double MINI_MAX = 5;

    private static final double MIN_SPAN = 2;
    private static final double MAX_SPAN = 10_000;
    private static final double LABEL_MIN_PX = 14;

    double xMin = DEFAULT_MIN;
    double xMax = DEFAULT_MAX;
    double yMin = DEFAULT_MIN;
    double yMax = DEFAULT_MAX;

    private double defaultMin = DEFAULT_MIN;
    private double defaultMax = DEFAULT_MAX;

    private double scale = 1;
    private double offsetX;
    private double offsetY;
    private double canvasWidth;
    private double canvasHeight;

    void updateLayout(double width, double height) {
        canvasWidth = width;
        canvasHeight = height;
        double plotW = Math.max(1, width);
        double plotH = Math.max(1, height);
        double xSpan = xMax - xMin;
        double ySpan = yMax - yMin;
        if (xSpan <= 0 || ySpan <= 0) {
            resetToDefault();
            xSpan = xMax - xMin;
            ySpan = yMax - yMin;
        }
        // Fill the canvas (no letterboxing dead zones); grid cells stay square.
        scale = Math.max(plotW / xSpan, plotH / ySpan);
        if (!Double.isFinite(scale) || scale <= 0) {
            scale = 1;
        }
        offsetX = (plotW - xSpan * scale) / 2;
        offsetY = (plotH - ySpan * scale) / 2;
    }

    double scale() {
        return scale;
    }

    double toScreenX(double x) {
        return offsetX + (x - xMin) * scale;
    }

    double toScreenY(double y) {
        return offsetY + (yMax - y) * scale;
    }

    double screenToMathX(double screenX) {
        return xMin + (screenX - offsetX) / scale;
    }

    double screenToMathY(double screenY) {
        return yMax - (screenY - offsetY) / scale;
    }

    double visibleMathXLeft() {
        return screenToMathX(0);
    }

    double visibleMathXRight() {
        return screenToMathX(canvasWidth);
    }

    double visibleMathYTop() {
        return screenToMathY(0);
    }

    double visibleMathYBottom() {
        return screenToMathY(canvasHeight);
    }

    boolean xAxisInView() {
        return yMin <= 0 && yMax >= 0;
    }

    boolean yAxisInView() {
        return xMin <= 0 && xMax >= 0;
    }

    int gridXStart() {
        return firstTick(Math.min(visibleMathXLeft(), visibleMathXRight()), 1);
    }

    int gridXEnd() {
        return lastTick(Math.max(visibleMathXLeft(), visibleMathXRight()), 1);
    }

    int gridYStart() {
        return firstTick(Math.min(visibleMathYBottom(), visibleMathYTop()), 1);
    }

    int gridYEnd() {
        return lastTick(Math.max(visibleMathYBottom(), visibleMathYTop()), 1);
    }

    int labelStepX() {
        return labelStepForScale(scale);
    }

    int labelStepY() {
        return labelStepForScale(scale);
    }

    private static int labelStepForScale(double pixelsPerUnit) {
        if (pixelsPerUnit >= LABEL_MIN_PX) {
            return 1;
        }
        if (pixelsPerUnit >= LABEL_MIN_PX / 2) {
            return 2;
        }
        if (pixelsPerUnit >= LABEL_MIN_PX / 4) {
            return 5;
        }
        return 10;
    }

    /** Shifts the view when the user drags the graph (pixels → math coordinates). */
    void panPixels(double dxPixels, double dyPixels) {
        if (scale <= 0) {
            return;
        }
        xMin -= dxPixels / scale;
        xMax -= dxPixels / scale;
        yMin += dyPixels / scale;
        yMax += dyPixels / scale;
    }

    /** Zooms about a screen point. {@code factor > 1} zooms in. */
    void zoomAbout(double screenX, double screenY, double factor) {
        if (scale <= 0 || factor <= 0 || !Double.isFinite(factor)) {
            return;
        }
        double mathX = screenToMathX(screenX);
        double mathY = screenToMathY(screenY);

        double xSpan = clampSpan((xMax - xMin) / factor);
        double ySpan = clampSpan((yMax - yMin) / factor);

        double fracX = (mathX - xMin) / (xMax - xMin);
        double fracY = (yMax - mathY) / (yMax - yMin);

        xMin = mathX - fracX * xSpan;
        xMax = xMin + xSpan;
        yMax = mathY + fracY * ySpan;
        yMin = yMax - ySpan;
    }

    void zoomAboutCenter(double factor) {
        zoomAbout(canvasWidth / 2, canvasHeight / 2, factor);
    }

    private static double clampSpan(double span) {
        return Math.max(MIN_SPAN, Math.min(MAX_SPAN, span));
    }

    void resetToDefault() {
        xMin = defaultMin;
        xMax = defaultMax;
        yMin = defaultMin;
        yMax = defaultMax;
    }

    void useFullDefaults() {
        defaultMin = DEFAULT_MIN;
        defaultMax = DEFAULT_MAX;
        resetToDefault();
    }

    void useMiniDefaults() {
        defaultMin = MINI_MIN;
        defaultMax = MINI_MAX;
        resetToDefault();
    }

    void fitToRange(double xMin, double xMax, double yMin, double yMax) {
        if (xMax <= xMin || yMax <= yMin) {
            return;
        }
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    static int firstTick(double min, int step) {
        return (int) (Math.ceil(min / step - 1e-9) * step);
    }

    static int lastTick(double max, int step) {
        return (int) (Math.floor(max / step + 1e-9) * step);
    }

    static String formatInteger(int value) {
        return Integer.toString(value);
    }
}
