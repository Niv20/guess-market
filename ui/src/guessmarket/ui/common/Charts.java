package guessmarket.ui.common;

import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.List;

/**
 * The one thing both graphs of the program need doing to them.
 *
 * <p>Each of them is drawn against a count rather than against a measurement: a user's balance
 * after their first action, their second, their third, and an option's price after the first
 * transaction in its event, the second, the third. There is no such thing as action 0.25.
 *
 * <p>A {@link NumberAxis} left to range itself does not know that, and with only a handful of
 * points it happily divides the space into quarters and labels them. Turning the ranging off and
 * naming the two ends and the step is what keeps the labels on whole numbers however few points
 * there are, and keeps them few enough to read however many there are.
 */
public final class Charts {

    /** About as many labels as fit along an axis of the width these graphs are given. */
    private static final int MOST_LABELS = 10;

    private Charts() {
    }

    /**
     * Lays an axis out as a count of things: whole numbers from the first to the last, and never
     * more labels than can be read.
     *
     * @param axis   the axis the count runs along
     * @param series every series drawn against it, which is where the two ends are read from
     */
    public static void countAlong(NumberAxis axis, List<XYChart.Series<Number, Number>> series) {
        double first = Double.MAX_VALUE;
        double last = -Double.MAX_VALUE;
        for (XYChart.Series<Number, Number> line : series) {
            for (XYChart.Data<Number, Number> point : line.getData()) {
                double x = point.getXValue().doubleValue();
                first = Math.min(first, x);
                last = Math.max(last, x);
            }
        }
        if (first > last) {
            axis.setAutoRanging(true);
            return;
        }

        double span = Math.max(1, last - first);
        axis.setAutoRanging(false);
        axis.setLowerBound(first);
        axis.setUpperBound(first + span);
        axis.setTickUnit(Math.ceil(span / MOST_LABELS));
        axis.setMinorTickCount(0);
    }
}
