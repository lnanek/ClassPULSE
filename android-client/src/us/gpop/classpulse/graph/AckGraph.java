package us.gpop.classpulse.graph;

import android.content.Context;
import android.graphics.Point;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

/**
 * Created by Michael Yoon Huh on 6/20/2014.
 */
public class AckGraph {

    /** GRAPH FUNCTIONALITY ____________________________________________________________________ **/

    int ackPointCounter = 1; // Data point values.
    int overallAckRating = 0; // Acknowledgement rating value.
    GraphView.GraphViewData[] ackData; // Graph for ackdata.
    Point[] ackDataPoints = new Point[ackPointCounter];
    public GraphView graphView; // Graph title.

    // refreshGraph(): Updates the overall acknowledgment rating.
    public void refreshGraph(Context con, Boolean yayOrNay) {

        // If true, indicating YAY/UNDERSTOOD response.
        if (yayOrNay) {
            overallAckRating++; // Increment the acknowledgment rating.
            updateGraph(con, ackPointCounter); // Update the graph.
            ackPointCounter++; // Increment the counter.
        }

        // Otherwise, false indicates NAY/I'M LOST response.
        else {
            overallAckRating--; // Decrement the acknowledgment rating.
            updateGraph(con, ackPointCounter); // Update the graph.
            ackPointCounter++; // Increment the counter.
        }
    }


    // setUpGraph(): Sets up the graph.
    public void setUpGraph(Context con) {

        // Generate the line graph.
        ackData = new GraphView.GraphViewData[ackPointCounter];
        ackData[0] = new GraphView.GraphViewData(0, 0); // Start at 0, 0.

        ackDataPoints[0] = new Point(); // Initialize the first Point.
        ackDataPoints[0].set(0, 0); // Sets the initial (0, 0) point data.

        ackPointCounter++; // Increments the acknowledgment data point counter.

        graphView = new LineGraphView(con, ""); // Graph title.
        graphView.addSeries(new GraphViewSeries(ackData)); // Adds the data into the graph.

        graphView.setViewPort(0, 60); // Sets the bottom scale.
        graphView.setManualYAxisBounds(100, -100); // Sets the Y-Axis Bounds.
        graphView.setScrollable(true); // Enables scrolling of the graph.
        graphView.setScalable(true); // Enables scaling of the graph.

        //LinearLayout layout = (LinearLayout) findViewById(R.id.graph_container);
        //layout.addView(graphView);
    }

    // updateGraph(): Recreates the graph with the new points.
    public void updateGraph(Context con, int dataPoints) {

        // New AckPoints object to return.
        Point[] newAckPoints = new Point[dataPoints];

        // Re-create the array.
        for (int i = 0; i < dataPoints - 1; i++) {
            newAckPoints[i] = ackDataPoints[i]; // Copy each element.
        }

        // Set the latest data points.
        newAckPoints[dataPoints - 1] = new Point();
        newAckPoints[dataPoints - 1].set(dataPoints, overallAckRating);

        // TOAST DEBUG
        Toast.makeText(con,
                "Current Instructor Rating at " + newAckPoints[dataPoints - 1].x + " MINUTES is " + newAckPoints[dataPoints - 1].y + "! Thank you for your input!", Toast.LENGTH_SHORT).show();

        // Recreates the graph.
        ackData = new GraphView.GraphViewData[dataPoints];

        // Retrieves the past ackPoints and adds them into the new data series.
        for (int i = 0; i < dataPoints; i++) {
            ackData[i] = new GraphView.GraphViewData(i, newAckPoints[i].y);
        }

        graphView.addSeries(new GraphViewSeries(ackData)); // Adds the data into the graph.
        graphView.setViewPort(0, 60); // Sets the bottom scale.
        graphView.setManualYAxisBounds(100, -100); // Sets the Y-Axis Bounds.
        graphView.redrawAll();

        ackDataPoints = newAckPoints;
    }
}