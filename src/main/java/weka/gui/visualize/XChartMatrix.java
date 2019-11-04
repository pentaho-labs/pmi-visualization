/*******************************************************************************
 * Pentaho Data Science
 * <p/>
 * Copyright (c) 2002-2019 Hitachi Vantara. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.Histogram;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.internal.chartpart.Chart;

import weka.core.Attribute;
import weka.core.Instances;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class XChartMatrix {

  @SuppressWarnings("unchecked")
  public static JPanel getMatrix(Instances instances, int chartWidth,
    int markerSize, boolean image, boolean lowerTriangle) {

    int numPlots = instances.numAttributes() * instances.numAttributes();
    int classIndex = instances.classIndex();

    XChartPanel<Chart>[][] chartMatrix =
      new XChartPanel[instances.numAttributes()][instances.numAttributes()];

    int numToPlot = 0;
    List<Instances> perC =
      classIndex >= 0 && instances.classAttribute().isNominal() ? getPerClassData(instances)
        : null;
    int numBins =
      Math.min((int) (Math.log(instances.numInstances()) / Math.log(2.0)), 10);

    // data lookup for the full dataset
    List<double[]> attributeDataLookup = new ArrayList<>();

    // data lookup per nominal class
    Map<Integer, List<double[]>> attributeDataPerClassLookup = new HashMap<>();
    if (perC != null) {
      for (int i = 0; i < perC.size(); i++) {
        Instances fC = perC.get(i);
        List<double[]> attVals = new ArrayList<>();
        for (int j = 0; j < instances.numAttributes(); j++) {
          attVals.add(fC.attributeToDoubleArray(j));
        }
        attributeDataPerClassLookup.put(i, attVals);
      }
    }
    for (int i = 0; i < instances.numAttributes(); i++) {
      attributeDataLookup.add(instances.attributeToDoubleArray(i));
    }
    for (int y = 0; y < instances.numAttributes(); y++) {
      if ((instances.attribute(y).isNumeric() || instances.attribute(y)
        .isNominal())
        && instances.attributeStats(y).missingCount < instances.numInstances()) {
        numToPlot++;
      }
      int bound = lowerTriangle ? y : instances.numAttributes() - 1;
      for (int x = 0; x <= bound; x++) {
        Attribute yy = instances.attribute(y);
        Attribute xx = instances.attribute(x);
        if (instances.attributeStats(y).missingCount < instances.numInstances()
          && instances.attributeStats(x).missingCount < instances
            .numInstances()) {

          Chart chart = null;
          double xmin =
            xx.isNumeric() ? instances.attributeStats(x).numericStats.min : 0;
          double xmax =
            xx.isNumeric() ? instances.attributeStats(x).numericStats.max : xx
              .numValues() - 1;
          double ymin =
            yy.isNumeric() ? instances.attributeStats(y).numericStats.min : 0;
          double ymax =
            yy.isNumeric() ? instances.attributeStats(y).numericStats.max : yy
              .numValues() - 1;
          if (x == y && xmax - xmin > 0) {
            chart =
              new CategoryChartBuilder().width(chartWidth).height(chartWidth)
                .xAxisTitle(xx.name()).yAxisTitle(xx.name()).build();
            if (xx.isNumeric()) {
              // histograms (per class?)
              ((CategoryChart) chart).getStyler().setAvailableSpaceFill(0.96);
              ((CategoryChart) chart).getStyler().setAxisTitlesVisible(false);
              ((CategoryChart) chart).getStyler().setChartTitleVisible(false);
              ((CategoryChart) chart).getStyler().setLegendVisible(false);
              if (chartWidth <= 100) {
                ((CategoryChart) chart).getStyler().setAxisTicksVisible(false);
              }

              if (classIndex >= 0 && instances.classAttribute().isNominal()) {
                for (int k = 0; k < perC.size(); k++) {
                  Instances instForC = perC.get(k);
                  if (instForC.numInstances() > 0) {
                    // double[] xdata = instForC.attributeToDoubleArray(x);
                    double[] xdata = attributeDataPerClassLookup.get(k).get(x);
                    List<Double> xdataL = new ArrayList<Double>();
                    for (int l = 0; l < xdata.length; l++) {
                      xdataL.add(xdata[l]);
                    }
                    Histogram hist = new Histogram(xdataL, numBins, xmin, xmax);
                    ((CategoryChart) chart).addSeries(instances
                      .classAttribute().value(k), hist.getxAxisData(), hist
                      .getyAxisData());
                  }
                }
              } else {
                double[] xdata = attributeDataLookup.get(x);
                List<Double> xdataL = new ArrayList<Double>();
                for (int l = 0; l < xdata.length; l++) {
                  xdataL.add(xdata[l]);
                }
                Histogram hist = new Histogram(xdataL, numBins, xmin, xmax);
                ((CategoryChart) chart).addSeries("a", hist.getxAxisData(),
                  hist.getyAxisData());
              }
            } else {
              // TODO stacked by class distribution
              // nominal bar chart
              List<String> categories = new ArrayList<String>();
              List<Number> categoryCounts = new ArrayList<Number>();
              for (int k = 0; k < xx.numValues(); k++) {
                categories.add(xx.value(k));
                categoryCounts
                  .add(instances.attributeStats(x).nominalCounts[k]);
              }
              ((CategoryChart) chart).addSeries(xx.name(), categories,
                categoryCounts);
              ((CategoryChart) chart).getStyler().setLegendVisible(false);
              ((CategoryChart) chart).getStyler().setAxisTitlesVisible(false);
              if (chartWidth <= 100) {
                ((CategoryChart) chart).getStyler().setAxisTicksVisible(false);
              }
            }
          } else {
            chart =
              new XYChartBuilder().width(chartWidth).height(chartWidth).build();
            ((XYChart) chart).getStyler().setDefaultSeriesRenderStyle(
              XYSeries.XYSeriesRenderStyle.Scatter);
            ((XYChart) chart).getStyler().setAxisTitlesVisible(false);
            ((XYChart) chart).getStyler().setChartTitleVisible(false);
            ((XYChart) chart).getStyler().setXAxisMin(xmin);
            ((XYChart) chart).getStyler().setXAxisMax(xmax);
            ((XYChart) chart).getStyler().setYAxisMin(ymin);
            ((XYChart) chart).getStyler().setYAxisMax(ymax);
            ((XYChart) chart).getStyler().setLegendVisible(false);
            ((XYChart) chart).getStyler().setMarkerSize(markerSize);
            ((XYChart) chart).getStyler().setXAxisTicksVisible(false);
            ((XYChart) chart).getStyler().setYAxisTicksVisible(false);

            chart.setYAxisTitle(yy.name());
            chart.setXAxisTitle(xx.name());

            if (classIndex >= 0 && instances.classAttribute().isNominal()) {
              for (int k = 0; k < perC.size(); k++) {
                Instances instForC = perC.get(k);
                if (instForC.numInstances() > 0) {
                  double[] xdata = attributeDataPerClassLookup.get(k).get(x);
                  double[] ydata = attributeDataPerClassLookup.get(k).get(y);
                  ((XYChart) chart).addSeries(
                    instances.classAttribute().value(k), xdata, ydata);
                }
              }
            } else {
              double[] xdata = attributeDataLookup.get(x);
              double[] ydata = attributeDataLookup.get(y);
              ((XYChart) chart).addSeries("a", xdata, ydata);
            }
          }
          XChartPanel p = new XChartPanel(chart);
          chartMatrix[y][x] = p;
        }
      }
    }
    for (int i = 0; i < instances.numAttributes(); i++) {
      if (chartMatrix[i][0] != null) {
        if (chartMatrix[i][0].getChart() instanceof XYChart) {
          if (chartWidth >= 100) {
            ((XYChart) chartMatrix[i][0].getChart()).getStyler()
              .setYAxisTitleVisible(true);
            if (chartWidth > 100) {
              ((XYChart) chartMatrix[i][0].getChart()).getStyler()
                .setYAxisTicksVisible(true);
            }
          }
        } else {
          if (chartWidth >= 100) {
            ((CategoryChart) chartMatrix[i][0].getChart()).getStyler()
              .setYAxisTitleVisible(true);
            if (chartWidth > 100 && i == 0) {
              ((CategoryChart) chartMatrix[i][0].getChart()).getStyler()
                .setYAxisTicksVisible(true);
            }
          }
        }
      }
    }

    for (int i = 0; i < instances.numAttributes(); i++) {
      int yl = instances.numAttributes() - 1;
      if (chartMatrix[yl][i] != null) {
        if (chartMatrix[yl][i].getChart() instanceof XYChart) {
          if (chartWidth >= 100) {
            ((XYChart) chartMatrix[yl][i].getChart()).getStyler()
              .setXAxisTitleVisible(true);
            if (chartWidth > 100) {
              ((XYChart) chartMatrix[yl][i].getChart()).getStyler()
                .setXAxisTicksVisible(true);
            }
          }
        } else {
          if (chartWidth >= 100) {
            ((CategoryChart) chartMatrix[yl][i].getChart()).getStyler()
              .setXAxisTitleVisible(true);
            if (chartWidth > 100 && i == yl) {
              ((CategoryChart) chartMatrix[yl][i].getChart()).getStyler()
                .setXAxisTicksVisible(true);
            }
          }
        }
      }
    }

    JPanel chartP = new JPanel(new GridLayout(numToPlot, numToPlot));
    JPanel[][] chartHolders = new JPanel[numToPlot][numToPlot];
    for (int y = 0; y < numToPlot; y++) {
      for (int x = 0; x < numToPlot; x++) {
        JPanel temp = new JPanel(new BorderLayout());
        chartHolders[y][x] = temp;
        chartP.add(temp);
      }
    }

    int yy = 0;
    for (int y = 0; y < instances.numAttributes(); y++) {
      int xx = 0;
      boolean addSome = false;
      for (int x = 0; x < instances.numAttributes(); x++) {
        if (chartMatrix[y][x] != null) {
          if (image) {
            chartHolders[yy][xx].add(new JLabel(new ImageIcon(BitmapEncoder
              .getBufferedImage(chartMatrix[y][x].getChart()))));
          } else {
            chartHolders[yy][xx].add(chartMatrix[y][x], BorderLayout.CENTER);
          }
          xx++;
          addSome = true;
        }
      }
      if (addSome) {
        yy++;
      }
    }
    JPanel finalP = new JPanel(new FlowLayout(FlowLayout.LEFT));
    finalP.add(chartP);
    return finalP;
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

  protected static double[][] getDataForClass(int x, int y, int classValIndex,
    Instances instances) {
    List<Double> xx = new ArrayList<Double>();
    List<Double> yy = new ArrayList<Double>();
    for (int i = 0; i < instances.numInstances(); i++) {
      if (!instances.instance(i).isMissing(x)
        && !instances.instance(i).isMissing(y)
        && !instances.instance(i).classIsMissing()) {
        if (instances.instance(i).classValue() == classValIndex) {
          xx.add(instances.instance(i).value(x));
          yy.add(instances.instance(i).value(y));
        }
      }
    }
    double[][] result = new double[2][xx.size()];
    for (int i = 0; i < xx.size(); i++) {
      result[0][i] = xx.get(i);
      result[1][i] = yy.get(i);
    }
    return result;
  }

  public static void main(String[] args) {
    try {
      Instances inst =
        new Instances(new BufferedReader(new FileReader(args[0])));
      inst.setClassIndex(inst.numAttributes() - 1);

      int width = Integer.parseInt(args[1]);
      int size = Integer.parseInt(args[2]);
      JPanel p = XChartMatrix.getMatrix(inst, width, size, true, true);
      JScrollPane scrollPane = new JScrollPane();
      scrollPane.setViewportView(p);
      JFrame jf = new JFrame();
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(scrollPane, BorderLayout.CENTER);
      jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      jf.pack();
      jf.setSize(1024, 768);
      jf.setVisible(true);

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
