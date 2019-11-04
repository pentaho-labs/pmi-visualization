/*******************************************************************************
 * Pentaho Data Science
 * <p/>
 * Copyright (c) 2002-2018 Hitachi Vantara. All rights reserved.
 * <p/>
 * ******************************************************************************
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 * <p/>
 ******************************************************************************/

package weka.gui.visualize;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class FXChartMatrix {

  protected final GridPane m_root = new GridPane();
  protected Scene m_scene;
  protected Instances m_data;
  protected Instances m_dataBinned;
  protected List<XYChart.Series<Number, Number>> m_scatterSeries =
    new ArrayList<XYChart.Series<Number, Number>>();
  protected List<Instances> m_originalPerC;
  protected List<Instances> m_binnedPerC;

  public void setInstances(Instances instances) {
    // TODO clear root

    m_data = instances;
    int classIndex = m_data.classIndex();
    m_originalPerC =
      classIndex >= 0 && m_data.classAttribute().isNominal() ? getPerClassData(m_data)
        : null;

    int numBins =
      Math.min((int) (Math.log(instances.numInstances()) / Math.log(2.0)), 10);
    Discretize discretize = new Discretize();
    discretize.setBins(numBins);
    discretize.setBinRangePrecision(2);
    discretize.setIgnoreClass(true);
    try {
      discretize.setInputFormat(instances);
      m_dataBinned = Filter.useFilter(m_data, discretize);
      m_binnedPerC =
        classIndex >= 0 && m_data.classAttribute().isNominal() ? getPerClassData(m_dataBinned)
          : null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void buildChart(int x, int y, int chartWidth) {
    Attribute xx = m_data.attribute(x);
    Attribute yy = m_data.attribute(y);
    int classIndex = m_data.classIndex();

    if (x == y) {
      // bar/histogram
      final CategoryAxis xAxis = new CategoryAxis();
      final NumberAxis yAxis = new NumberAxis();
      final BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
      barChart.setCategoryGap(0);
      barChart.setBarGap(1);
      if (x == m_data.numAttributes() - 1) {
        xAxis.setLabel(xx.name());
      }
      if (y == 0) {
        yAxis.setLabel(yy.name());
      }

      Instances full = xx.isNominal() ? m_data : m_dataBinned;
      List<Instances> pC = xx.isNominal() ? m_originalPerC : m_binnedPerC;
      if (classIndex >= 0 && m_data.classAttribute().isNominal()) {
        for (int k = 0; k < pC.size(); k++) {
          Instances instForC = pC.get(k);
          if (instForC.numInstances() > 0) {
            XYChart.Series<String, Number> aSeries = new XYChart.Series<>();
            aSeries.setName(m_data.classAttribute().value(k));
            for (int i = 0; i < instForC.attribute(x).numValues(); i++) {
              String cat = full.attribute(x).value(i);
              int catCount = instForC.attributeStats(x).nominalCounts[i];
              aSeries.getData().add(
                new XYChart.Data<String, Number>(cat, catCount));
            }
            barChart.getData().add(aSeries);
          }
        }
      } else {
        XYChart.Series<String, Number> aSeries = new XYChart.Series<>();
        for (int i = 0; i < full.attribute(x).numValues(); i++) {
          String cat = full.attribute(x).value(i);
          int catCount = full.attributeStats(x).nominalCounts[i];
          aSeries.getData().add(
            new XYChart.Data<String, Number>(cat, catCount));
        }
        barChart.getData().add(aSeries);
      }
      barChart.setLegendVisible(false);
      barChart.setPrefSize(chartWidth, chartWidth);
      barChart.setMinSize(100, 100);
      GridPane.setConstraints(barChart, x, y);
      m_root.getChildren().add(barChart);
    } else {
      final NumberAxis xAxis = new NumberAxis();
      if (y == m_data.numAttributes() - 1) {
        xAxis.setLabel(xx.name());
      }
      final NumberAxis yAxis = new NumberAxis();
      if (x == 0) {
        yAxis.setLabel(yy.name());
      }

      // yAxis.setTickLabelsVisible(false);

      final ScatterChart<Number, Number> sc =
        new ScatterChart<Number, Number>(xAxis, yAxis);

      if (classIndex >= 0 && m_data.classAttribute().isNominal()) {
        // series per class label
        for (int k = 0; k < m_originalPerC.size(); k++) {
          Instances instForC = m_originalPerC.get(k);
          if (instForC.numInstances() > 0) {
            XYChart.Series<Number, Number> aSeries =
              new XYChart.Series<Number, Number>();
            for (int i = 0; i < instForC.numInstances(); i++) {
              if (!instForC.instance(i).isMissing(x)
                && !instForC.instance(i).isMissing(y)) {
                aSeries.getData().add(
                  new XYChart.Data<Number, Number>(instForC.instance(i)
                    .value(x), instForC.instance(i).value(y)));
              }
            }
            aSeries.setName(m_data.classAttribute().value(k));
            sc.getData().add(aSeries);
            m_scatterSeries.add(aSeries);
          }
        }
      } else {
        XYChart.Series<Number, Number> aSeries =
          new XYChart.Series<Number, Number>();
        aSeries.setName(m_data.relationName());
        for (int i = 0; i < m_data.numInstances(); i++) {
          Instance current = m_data.instance(i);
          if (!current.isMissing(x) && !current.isMissing(y)) {
            aSeries.getData().add(
              new XYChart.Data<Number, Number>(current.value(x), current
                .value(y)));
          }
        }

        sc.getData().add(aSeries);
        m_scatterSeries.add(aSeries);
      }
      sc.setLegendVisible(false);

      // sc.setTranslateX(0);
      // sc.setTranslateY(0);
      sc.setPrefSize(chartWidth, chartWidth);
      sc.setMinSize(100, 00);
      GridPane.setConstraints(sc, x, y);
      m_root.getChildren().add(sc);
    }
  }

  public Scene buildScene() {
    System.out.println("buildScene()");
    ScrollPane s1 = new ScrollPane();
    s1.setPrefSize(1024, 768);
    s1.setContent(m_root);
    m_scene = new Scene(s1, 1024, 768, Color.LIGHTGRAY);

    return m_scene;
  }

  public void setScatterPointSize(int pointSize) {
    // try to adjust point size
    for (XYChart.Series<Number, Number> series : m_scatterSeries) {
      for (XYChart.Data<Number, Number> data : series.getData()) {
        StackPane stackPane = (StackPane) data.getNode();
        stackPane.setPrefWidth(pointSize);
        stackPane.setPrefHeight(pointSize);
      }
    }
  }

  public void freeMemory() {
    m_scatterSeries.clear();
    m_data = null;
    m_dataBinned = null;
    m_binnedPerC.clear();
    m_originalPerC.clear();
  }

  public void adjustChartSizes(int chartWidth) {
    for (Node n : m_root.getChildren()) {
      if (n instanceof Region) {
        ((Region) n).setPrefSize(chartWidth, chartWidth);
      }
    }
  }

  private static Instances loadInstances(String path) {
    try {
      return new Instances(new BufferedReader(new FileReader(path)));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  protected static List<Instances> getPerClassData(Instances data) {
    List<Instances> perC = new ArrayList<Instances>();
    for (int i = 0; i < data.classAttribute().numValues(); i++) {
      Instances c = new Instances(data, 0);
      perC.add(c);
    }

    for (int i = 0; i < data.numInstances(); i++) {
      if (!data.instance(i).classIsMissing()) {
        perC.get((int) data.instance(i).classValue()).add(data.instance(i));
      }
    }

    return perC;
  }

  private static FXChartMatrix initFX(JFXPanel fxPanel, String[] args) {
    FXChartMatrix fxChartMatrix = new FXChartMatrix();
    Instances toPlot = loadInstances(args[0]);
    toPlot.setClassIndex(toPlot.numAttributes() - 1);
    int chartWidth = Integer.parseInt(args[1]);
    int pointSize = Integer.parseInt(args[2]);
    fxChartMatrix.setInstances(toPlot);
    for (int i = 0; i < toPlot.numAttributes(); i++) {
      for (int j = i; j < toPlot.numAttributes(); j++) {
        fxChartMatrix.buildChart(i, j, chartWidth);
      }
    }

    Scene scene = fxChartMatrix.buildScene();
    fxChartMatrix.setScatterPointSize(pointSize);
    fxChartMatrix.freeMemory();
    fxPanel.setScene(scene);
    return fxChartMatrix;
  }

  private static void initAndShowGUI(final String[] args) {
    // This method is invoked on the EDT thread
    JFrame frame = new JFrame("Swing and JavaFX");
    final JFXPanel fxPanel = new JFXPanel();
    frame.add(fxPanel);
    frame.setSize(1024, 768);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        FXChartMatrix fm = initFX(fxPanel, args);
      }
    });
  }

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        initAndShowGUI(args);
      }
    });
  }
}
