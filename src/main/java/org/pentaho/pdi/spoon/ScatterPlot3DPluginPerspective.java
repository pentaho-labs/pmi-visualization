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

package org.pentaho.pdi.spoon;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javafx.embed.swt.FXCanvas;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.pentaho.di.core.EngineMetaInterface;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.RowListener;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.ui.spoon.SpoonPerspective;
import org.pentaho.di.ui.spoon.SpoonPerspectiveListener;
import org.pentaho.dm.commons.ArffMeta;
import org.pentaho.ui.xul.XulOverlay;
import org.pentaho.ui.xul.impl.XulEventHandler;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.gui.beans.AttributeSummarizer;
import weka.gui.visualize.ScatterScene3D;
import weka.gui.visualize.XChartMatrix;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A Spoon perspective that provides a 3D scatter plot of a data set sourced
 * from either a Kettle TableInput or TableOutput step. It also provides an
 * overview of the data in the form of a second visualization made up of
 * an matrix of histograms, one for each incoming field. If the coloring
 * dimension is discrete, each histogram will show the relative proportion
 * of each value of the coloring dimension within each bar of a given histogram.
 *
 * @author Mark Hall (mhall{[at]}pentaho.com}
 */
public class ScatterPlot3DPluginPerspective implements SpoonPerspective, RowListener {

  /**
   * The 3D panel
   */
  protected ScatterScene3D m_scatterScene;
  protected Composite m_plotComposite;
  protected FXCanvas m_3dCanvas;

  /**
   * The histogram matrix
   */
  protected AttributeSummarizer m_atts;

  protected java.awt.Panel m_scatterHolder;

  /**
   * Combo boxes for selecting visualization axes and coloring dimension
   */
  protected CCombo m_wbX;
  protected CCombo m_wbY;
  protected CCombo m_wbZ;
  protected CCombo m_wbC;
  protected boolean m_combosChanged;
  protected boolean m_colorComboChanged;
  protected boolean m_scatterControlsChanged;

  // protected Button m_wbGetDataFromTrans;

  /**
   * Button to update the display
   */
  protected Button m_wbUpdateDisplay;

  /**
   * Radio buttons for selecting random sampling or first n rows
   */
  protected Button m_wbRandomRadio;
  protected Button m_wbFirstRadio;

  /**
   * Spinners for the number of rows to sample and random seed
   */
  protected Spinner m_wNumberOfRows;
  protected Spinner m_wRandomSeed;
  protected Spinner m_wScatterPlotWidth;
  protected Spinner m_wScatterMarkerSize;
  protected Button m_lowerTriangleBut;
  protected Button m_imageBut;

  protected int m_seed = 1;
  protected List<Object[]> m_sample;
  protected int m_currentRow;
  protected Random m_random;

  /**
   * Reseviour size/max number of rows to visualize
   */
  protected int m_k = 5000;
  protected boolean m_stopAfterFirstKRows;

  protected ArffMeta[] m_incomingFields;
  protected boolean m_hasNominalAtts = false;
  protected Map<Object, Object>[] m_nominalVals;

  protected RowMetaInterface m_rowMeta;
  protected Instances m_data;
  protected boolean m_isWindows;

  /**
   * TODO: recieving rows from a step is not implemented yet.
   * Are we receiving rows from a step (rather than pulling
   * from a database?
   */
  protected StepInterface m_rowProducer = null;

  protected static ScatterPlot3DPluginPerspective SINGLETON = null;
  protected Composite m_UI;
  protected Shell m_shell;

  public ScatterPlot3DPluginPerspective() {
    if ( SINGLETON == null ) {

      Shell shell = null;

      shell = new Shell();
      shell.setLayout( new GridLayout() );
      shell.setSize( 900, 600 );
      m_shell = shell;

      // panel to hold the scatter plots
      m_scatterHolder = new java.awt.Panel();
      m_scatterHolder.setLayout( new BorderLayout() );

      // panel to hold buttons and the embedded 3D panel
      final Composite holderPanel = new Composite( shell, SWT.NONE );
      holderPanel.setLayout( new GridLayout( 2, true ) );

      // combo box setup
      m_wbX = new CCombo( holderPanel, SWT.BORDER | SWT.READ_ONLY );
      m_wbX.setText( "X:" );
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      m_wbX.setLayoutData( gridData );

      m_wbY = new CCombo( holderPanel, SWT.BORDER | SWT.READ_ONLY );
      m_wbY.setText( "Y:" );
      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      m_wbY.setLayoutData( gridData );

      m_wbZ = new CCombo( holderPanel, SWT.BORDER | SWT.READ_ONLY );
      m_wbZ.setText( "Z:" );
      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      m_wbZ.setLayoutData( gridData );

      m_wbC = new CCombo( holderPanel, SWT.BORDER | SWT.READ_ONLY );
      m_wbC.setText( "Color:" );
      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      m_wbC.setLayoutData( gridData );

      // button setup      
      m_wbUpdateDisplay = new Button( holderPanel, SWT.PUSH );
      m_wbUpdateDisplay.setText( "Update display" );
      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.horizontalSpan = 2;
      m_wbUpdateDisplay.setLayoutData( gridData );

      // sampling controls
      Group samplingGroup = new Group( holderPanel, SWT.SHADOW_IN );
      samplingGroup.setLayout( new GridLayout( 16, false ) );
      samplingGroup.setText( "Rows to visualize" );
      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.horizontalSpan = 2;
      samplingGroup.setLayoutData( gridData );

      Label numRowsLabel = new Label( samplingGroup, SWT.RIGHT );
      numRowsLabel.setText( "Number of rows" );
      m_wNumberOfRows = new Spinner( samplingGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
      //m_wNumberOfRows.set
      //m_wNumberOfRows.setText("" + m_k + "  ");
      m_wNumberOfRows.setMaximum( 10000 );
      m_wNumberOfRows.setMinimum( 1 );
      m_wNumberOfRows.setIncrement( 100 );
      m_wNumberOfRows.setPageIncrement( 500 );
      m_wNumberOfRows.setSelection( m_k );

      Label firstRadioLabel = new Label( samplingGroup, SWT.RIGHT );
      firstRadioLabel.setText( "First" );
      m_wbFirstRadio = new Button( samplingGroup, SWT.RADIO );

      Label randomRadioLabel = new Label( samplingGroup, SWT.RIGHT );
      randomRadioLabel.setText( "Random" );
      m_wbRandomRadio = new Button( samplingGroup, SWT.RADIO );

      Label randomSeedLabel = new Label( samplingGroup, SWT.RIGHT );
      randomSeedLabel.setText( "Random seed" );
      m_wRandomSeed = new Spinner( samplingGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
      m_wRandomSeed.setMaximum( 10000 );
      m_wRandomSeed.setMinimum( 1 );
      m_wRandomSeed.setIncrement( 1 );
      m_wRandomSeed.setPageIncrement( 100 );
      m_wRandomSeed.setSelection( m_seed );

      Label scatterWidthLabel = new Label( samplingGroup, SWT.RIGHT );
      scatterWidthLabel.setText( "Scatter plot cell width/height" );
      m_wScatterPlotWidth = new Spinner( samplingGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
      m_wScatterPlotWidth.setMaximum( 500 );
      m_wScatterPlotWidth.setMinimum( 50 );
      m_wScatterPlotWidth.setPageIncrement( 25 );
      m_wScatterPlotWidth.setSelection( 250 );
      m_wScatterPlotWidth.setEnabled( false );

      Label scatterMarkerLabel = new Label( samplingGroup, SWT.RIGHT );
      scatterMarkerLabel.setText( "Scatter plot point size" );
      m_wScatterMarkerSize = new Spinner( samplingGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
      m_wScatterMarkerSize.setMaximum( 10 );
      m_wScatterMarkerSize.setMinimum( 1 );
      m_wScatterMarkerSize.setPageIncrement( 1 );
      m_wScatterMarkerSize.setSelection( 3 );
      m_wScatterMarkerSize.setEnabled( false );

      Label triangleLabel = new Label( samplingGroup, SWT.RIGHT );
      triangleLabel.setText( "Lower triangle" );
      m_lowerTriangleBut = new Button( samplingGroup, SWT.CHECK );
      m_lowerTriangleBut.setEnabled( false );
      m_lowerTriangleBut.setSelection( true );

      Label imageLabel = new Label( samplingGroup, SWT.RIGHT );
      imageLabel.setText( "Matrix of images" );
      imageLabel.setToolTipText(  "Slower generation/faster scrolling" );
      m_imageBut = new Button( samplingGroup, SWT.CHECK );
      m_imageBut.setEnabled( false );
      String osType = System.getProperty("os.name");
      m_isWindows = osType.toLowerCase().contains( "windows" );
      m_imageBut.setSelection( !m_isWindows );

      m_wbFirstRadio.setSelection( true );
      m_wRandomSeed.setEnabled( false );

      // listeners      
      m_wbRandomRadio.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e ) {
          m_wRandomSeed.setEnabled( m_wbRandomRadio.getSelection() );
        }
      } );

      m_wbUpdateDisplay.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e ) {
          try {
            int x = m_wbX.getSelectionIndex();
            int y = m_wbY.getSelectionIndex();
            int z = m_wbZ.getSelectionIndex();
            int c = m_wbC.getSelectionIndex();

            if ( m_combosChanged ) {
              m_scatterScene.updateAxes( x, y, z, c );
            }
            if ( ( m_colorComboChanged || m_scatterControlsChanged ) && !m_wbX.isEnabled() && m_data != null ) {
              // m_atts.setColoringIndex( c );
              // m_atts.performRequest( "dummy" ); // just trigger an update
              // m_scatterHolder.validate();
              if ( m_wbC.getSelectionIndex() < m_data.numAttributes() ) {
                m_data.setClassIndex( m_wbC.getSelectionIndex() );
                updateScatter( m_data, m_wScatterPlotWidth.getSelection(), m_wScatterMarkerSize.getSelection(), m_lowerTriangleBut.getSelection(),
                    m_imageBut.getSelection());
                m_colorComboChanged = false;
                m_scatterControlsChanged = false;
              }
            }
            m_combosChanged = false;

          } catch ( Exception ex ) {
            ex.printStackTrace();
          }
        }
      } );

      m_wbX.addModifyListener( new ModifyListener() {
        public void modifyText( ModifyEvent e ) {
          m_combosChanged = true;
        }
      } );

      m_wbY.addModifyListener( new ModifyListener() {
        public void modifyText( ModifyEvent e ) {
          m_combosChanged = true;
        }
      } );

      m_wbZ.addModifyListener( new ModifyListener() {
        public void modifyText( ModifyEvent e ) {
          m_combosChanged = true;
        }
      } );

      m_wbC.addModifyListener( new ModifyListener() {
        public void modifyText( ModifyEvent e ) {
          m_combosChanged = true;
          m_colorComboChanged = true;
        }
      } );

      m_wScatterPlotWidth.addModifyListener( new ModifyListener() {
        @Override public void modifyText( ModifyEvent modifyEvent ) {
          m_scatterControlsChanged = true;
        }
      } );

      m_wScatterMarkerSize.addModifyListener( new ModifyListener() {
        @Override public void modifyText( ModifyEvent modifyEvent ) {
          m_scatterControlsChanged = true;
        }
      } );

      m_lowerTriangleBut.addSelectionListener( new SelectionAdapter() {
        @Override public void widgetSelected( SelectionEvent selectionEvent ) {
          super.widgetSelected( selectionEvent );
          m_scatterControlsChanged = true;
        }
      } );

      m_imageBut.addSelectionListener( new SelectionAdapter() {
        @Override public void widgetSelected( SelectionEvent selectionEvent ) {
          super.widgetSelected( selectionEvent );
          m_scatterControlsChanged = true;
        }
      } );

      // tabs
      CTabFolder tabs = new CTabFolder( holderPanel, SWT.BORDER );
      tabs.setSimple( false );

      // tab item for the 3D panel
      final CTabItem tab3D = new CTabItem( tabs, SWT.NONE );
      tab3D.setText( "3D plot" );

      // tab item for the field histograms
      final CTabItem scatterM = new CTabItem( tabs, SWT.NONE );
      scatterM.setText( "Scatter plot matrix" );

      // layout for the field histograms panel
      GridLayout fieldHistLayout = new GridLayout();
      fieldHistLayout.marginWidth = 3;
      fieldHistLayout.marginHeight = 3;

      m_plotComposite = new Composite( tabs, SWT.NONE );

      m_scatterScene = new ScatterScene3D();
      // final Scene scene = m_scatterScene.getScene();
      FXCanvas fxCanvas = new FXCanvas( m_plotComposite, SWT.NONE ) {
        @Override public Point computeSize( int wHint, int hHint, boolean changed ) {
          super.computeSize( wHint, hHint, changed );
          if ( getScene() != null ) {
            getScene().getWindow().sizeToScene();
            int width = (int) getScene().getWidth();
            int height = (int) getScene().getHeight();
            return new Point( width, height );
          }
          return super.computeSize( wHint, hHint, changed );
        }
      };
      // fxCanvas.setScene( scene );
      m_3dCanvas = fxCanvas;
      FillLayout fl = new FillLayout();
      m_plotComposite.setLayout( fl );
      m_plotComposite.layout( true );

      tab3D.setControl( m_plotComposite );

      // composite to hold the embedded field histograms
      Composite scatterComposite = new Composite( tabs, SWT.EMBEDDED );
      scatterComposite.setLayout( fieldHistLayout );
      java.awt.Frame embedded2 = SWT_AWT.new_Frame( scatterComposite );
      embedded2.setLayout( new BorderLayout() );
      embedded2.add( m_scatterHolder, BorderLayout.CENTER );

      // layout for the scatter plot matrix
      // gridData = new GridData();
      // gridData.horizontalAlignment = GridData.FILL;
      // gridData.verticalAlignment = GridData.FILL;
      // gridData.grabExcessHorizontalSpace = true;
      // gridData.grabExcessVerticalSpace = true;
      //      gridData.horizontalSpan = 2;
      FormData fd = new FormData(  );
      fd.left = new FormAttachment( 0,0 );
      fd.top = new FormAttachment( 0, 0 );
      fd.right = new FormAttachment( 100, 0 );
      fd.bottom = new FormAttachment( 100, 0 );
      scatterComposite.setLayoutData( fd );
      scatterComposite.layout(  );
      scatterM.setControl( scatterComposite );

      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.horizontalSpan = 2;
      tabs.setLayoutData( gridData );
      tabs.layout( true );

      // layout for the holder panel
      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      holderPanel.setLayoutData( gridData );
      holderPanel.layout( true );

      tabs.setSelection( 0 );

      tabs.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e ) {
          if ( e.item.equals( scatterM ) ) {
            m_wbX.setEnabled( false );
            m_wbY.setEnabled( false );
            m_wbZ.setEnabled( false );
            m_wScatterPlotWidth.setEnabled( true );
            m_wScatterMarkerSize.setEnabled( true );
            m_lowerTriangleBut.setEnabled( true );
            m_imageBut.setEnabled( true );
          } else if ( e.item.equals( tab3D ) ) {
            m_wbX.setEnabled( true );
            m_wbY.setEnabled( true );
            m_wbZ.setEnabled( true );
            m_wScatterPlotWidth.setEnabled( false );
            m_wScatterMarkerSize.setEnabled( false );
            m_lowerTriangleBut.setEnabled( false );
            m_imageBut.setEnabled( false );
          }
        }
      } );

      m_UI = holderPanel;
      SINGLETON = this;
    }
  }

  /**
   * Return the embedded 3D visualization panel
   *
   * @return the 3D visualization panel
   */
  /* protected VisualizePanel3D getVisualizePanel() {
    return m_vis;
  } */

  /**
   * Return the histogram matrix panel.
   *
   * @return the histogram matrix panel
   */
  protected AttributeSummarizer getFieldHistogramsPanel() {
    return m_atts;
  }

  /**
   * Get the singleton instance of this class.
   *
   * @return the singleton instance of this class.
   */
  public static ScatterPlot3DPluginPerspective getSingleton() {
    if ( SINGLETON == null ) {
      new ScatterPlot3DPluginPerspective();
    }
    return SINGLETON;
  }

  @Override public void addPerspectiveListener( SpoonPerspectiveListener arg0 ) {
    // TODO Auto-generated method stub

  }

  @Override public EngineMetaInterface getActiveMeta() {
    return null;
  }

  @Override public String getDisplayName( Locale arg0 ) {
    return "PMI Visualization";
  }

  @Override public List<XulEventHandler> getEventHandlers() {
    return null;
  }

  @Override public String getId() {
    return "010-scatterPlot3D";
  }

  @Override public List<XulOverlay> getOverlays() {
    return null;
  }

  @Override public InputStream getPerspectiveIcon() {
    ClassLoader loader = getClass().getClassLoader();
    return loader.getResourceAsStream( "org/pentaho/pdi/spoon/scatterPlot3D.png" );
  }

  @Override public Composite getUI() {
    return m_UI;
  }

  @Override public void setActive( boolean arg0 ) {
    if ( m_UI != null ) {
      m_UI.layout( true );
      m_UI.redraw();
    }
  }

  // -------------

  protected void resetIncomingFields() {
    m_incomingFields = null;
  }

  protected void allocateForIncomingFields( int num ) {
    m_incomingFields = new ArffMeta[num];
  }

  protected void setupArffMetas( RowMetaInterface rmi ) {
    if ( rmi != null ) {
      m_rowMeta = rmi;
      allocateForIncomingFields( rmi.size() );
      initializeReservoir();

      m_nominalVals = (HashMap<Object, Object>[]) new HashMap[m_incomingFields.length];

      m_hasNominalAtts = false;

      for ( int i = 0; i < m_incomingFields.length; i++ ) {
        ValueMetaInterface inField = rmi.getValueMeta( i );
        int fieldType = inField.getType();
        switch ( fieldType ) {
          case ValueMetaInterface.TYPE_NUMBER:
          case ValueMetaInterface.TYPE_INTEGER:
          case ValueMetaInterface.TYPE_BOOLEAN:
            m_incomingFields[i] = new ArffMeta( inField.getName(), fieldType, ArffMeta.NUMERIC );
            // inField.getPrecision());
            break;
          case ValueMetaInterface.TYPE_STRING:
            m_incomingFields[i] = new ArffMeta( inField.getName(), fieldType, ArffMeta.NOMINAL );
            m_hasNominalAtts = true;
            m_nominalVals[i] = new HashMap<Object, Object>();
            break;
          case ValueMetaInterface.TYPE_DATE:
            m_incomingFields[i] = new ArffMeta( inField.getName(), fieldType, ArffMeta.DATE );
            m_incomingFields[i].setDateFormat( inField.getDateFormat().toPattern() );
            break;
        }
      }
    }
  }

  /**
   * Initialize the reservoir/cache for the requested sample (or cache)
   * size.
   */
  protected void initializeReservoir() {

    m_sample = ( m_k > 0 ) ? new ArrayList<Object[]>( m_k ) : new ArrayList<Object[]>();

    m_currentRow = 0;
    m_random = new Random( m_seed );

    // throw away the first 100 random numbers
    for ( int i = 0; i < 100; i++ ) {
      m_random.nextDouble();
    }
  }

  //brute force way of filling list when item index is  out of range,
  //should be ported to a commons or some library call or something
  //that works well with the "R" randomizing algorithm
  private static void setElement( List list, int idx, Object item ) {
    final int size = list.size();
    if ( size <= idx ) {
      //int len = Math.max(size, idx - size);
      int buff = ( size == 0 ) ? 100 : size * 2;
      for ( int i = 0; i < buff; i++ ) {
        list.add( null );
      }
    }
    list.set( idx, item );
  }

  private ArrayList<String> setupNominalVals( int index, ValueMetaInterface fieldMeta ) throws KettleException {
    Map<Object, Object> map = m_nominalVals[index];
    if ( fieldMeta.getType() != ValueMetaInterface.TYPE_STRING ) {
      throw new KettleException( "Field is not of type STRING! (setUpNominalVals)" );
    }

    // make sure that the values are in sorted order
    TreeSet<String> sorted = new TreeSet<String>();

    Set<Object> keySet = map.keySet();
    Iterator<Object> ksi = keySet.iterator();
    while ( ksi.hasNext() ) {
      Object val = ksi.next();
      String sval = fieldMeta.getString( val );
      sorted.add( sval );
    }

    // make the list
    ArrayList<String> attVals = new ArrayList<String>( sorted.size() );
    Iterator<String> tsi = sorted.iterator();
    while ( tsi.hasNext() ) {
      attVals.add( tsi.next() );
    }

    return attVals;
  }

  private Instances createHeader( RowMetaInterface rmi ) throws KettleException {
    ArrayList<Attribute> attInfo = new ArrayList<Attribute>( m_incomingFields.length );

    for ( int i = 0; i < m_incomingFields.length; i++ ) {
      ArffMeta tempField = m_incomingFields[i];
      Attribute tempAtt = null;
      int arffType = tempField.getArffType();
      switch ( arffType ) {
        case ArffMeta.NUMERIC:
          tempAtt = new Attribute( tempField.getFieldName() );
          break;
        case ArffMeta.NOMINAL:
          ArrayList<String> attVals = setupNominalVals( i, rmi.getValueMeta( i ) );
          tempAtt = new Attribute( tempField.getFieldName(), attVals );
          break;
        case ArffMeta.DATE:
          String dateF = tempField.getDateFormat();
          tempAtt = new Attribute( tempField.getFieldName(), dateF );
          break;
      }

      if ( tempAtt != null ) {
        attInfo.add( tempAtt );
      } else {
        throw new KettleException( "Unhandled attribute type (createHeader)" );
      }
    }

    Instances header = new Instances( "Visualize3D_data", attInfo, m_sample.size() );
    return header;
  }

  /**
   * Construct an instance from a kettle row
   *
   * @param inputRowMeta the meta data for the row
   * @param row          the row itself
   * @param header       the arff header to use
   * @return an Instance corresponding to the row
   * @throws KettleException if the conversion can't be performed
   */
  private Instance constructInstance( RowMetaInterface inputRowMeta, Object[] row, Instances header )
      throws KettleException {

    double[] vals = new double[header.numAttributes()]; // holds values for the new Instance

    for ( int i = 0; i < m_incomingFields.length; i++ ) {
      ArffMeta tempField = m_incomingFields[i];
      if ( tempField != null ) {
        int arffType = tempField.getArffType();
        Attribute currentAtt = header.attribute( i );

        ValueMetaInterface vmi = inputRowMeta.getValueMeta( i );
        int fieldType = vmi.getType();
        Object rowVal = row[i];

        // check for null
        String stringV = vmi.getString( rowVal );
        if ( stringV == null || stringV.length() == 0 ) {
          // set to missing value
          vals[i] = Utils.missingValue();
        } else {
          switch ( arffType ) {
            case ArffMeta.NUMERIC: {
              if ( fieldType == ValueMetaInterface.TYPE_BOOLEAN ) {
                Boolean b = vmi.getBoolean( rowVal );
                vals[i] = ( b.booleanValue() ) ? 1.0 : 0.0;
              } else if ( fieldType == ValueMetaInterface.TYPE_INTEGER ) {
                Long t = vmi.getInteger( rowVal );
                vals[i] = (double) t.longValue();
              } else {
                Double n = vmi.getNumber( rowVal );
                vals[i] = n.doubleValue();
              }
            }
            break;
            case ArffMeta.NOMINAL: {
              String s = vmi.getString( rowVal );
              // now need to look for this value in the attribute
              // in order to get the correct index
              int index = currentAtt.indexOfValue( s );
              if ( index < 0 ) {
                // shouldn't happen as we have been collecting all nominal values
                // over the entire stream of rows
                throw new KettleException( "Encountered a nominal value in the reservoir that "
                    + "wasn't seen over the entire stream! (reservoirToInstances)" );
              }
              vals[i] = (double) index;
            }
            break;
            case ArffMeta.DATE: {
              // Get the date as a number
              Double date = vmi.getNumber( rowVal );
              vals[i] = date.doubleValue();
            }
            break;
          }
        }
      }
    }
    Instance newInst = new DenseInstance( 1.0, vals );

    return newInst;
  }

  protected Instances reservoirToInstances( RowMetaInterface rmi ) throws KettleException {
    // Construct the Instances structure
    Instances header = createHeader( rmi );

    // Convert the sampled rows to Instance objects and add them to the data set
    Iterator<Object[]> li = m_sample.iterator();
    int counter = 0;
    while ( li.hasNext() ) {
      Object[] row = li.next(); // the row to process
      if ( row == null ) {
        // either more rows were requested than there actually were in the
        // end, or the mechanism for filling the list has added some residue null values
        break;
      }
      counter++;

      Instance newInst = constructInstance( rmi, row, header );

      // add that sucker...
      header.add( newInst );
    }

    // m_log.logBasic("[KFData] Converted " + counter + " rows to instances");
    header.compactify();
    return header;
  }

  protected void processRow( Object[] inputRow, RowMetaInterface inputMeta ) throws Exception {

    // Store any necessary nominal header values first.
    // Store as (possibly) kettle internal binary format (for speed).
    // At the end of the incoming stream of rows we will construct
    // the arff header and at that time convert from the binary format.
    if ( m_hasNominalAtts ) {
      for ( int i = 0; i < m_incomingFields.length; i++ ) {
        Object inField = inputRow[i];
        if ( inField != null ) {
          if ( m_incomingFields[i].getKettleType() == ValueMetaInterface.TYPE_STRING ) {
            if ( !m_nominalVals[i].containsKey( inField ) ) {
              m_nominalVals[i].put( inField, inField );
            }
          }
        }
      }
    }

    // Now see if this row should be stored in the reservoir
    // if sampling size is 0, do not sample.
    if ( m_k == 0 ) {
      m_sample.add( inputRow );
    } else if ( m_currentRow < m_k ) {
      setElement( m_sample, m_currentRow, inputRow );
      // size can be less than 0, which is essentially a blocking step
    } else if ( m_k > 0 ) {
      double r = m_random.nextDouble();
      if ( r < ( (double) m_k / (double) m_currentRow ) ) {
        r = m_random.nextDouble();
        int replace = (int) ( (double) m_k * r );
        setElement( m_sample, replace, inputRow );
      }
    }
    m_currentRow++;
  }

  public void setDataSource( String tableName, String sql, DatabaseMeta dbMeta ) {
    Database db = new Database( dbMeta );

    try {
      db.connect();

      String query = "SELECT * from " + tableName;
      if ( sql != null && sql.length() > 0 ) {
        query = sql;
      }
      ResultSet rs = db.openQuery( query );
      ResultSetMetaData rsmd = rs.getMetaData();
      RowMetaInterface rmi = db.getMetaFromRow( null, rsmd );
      m_rowMeta = rmi;
      preRows();
      setupArffMetas( rmi );

      Object[] row = null;
      int rowCount = 0;
      while ( ( row = db.getRow( rs ) ) != null ) {
        processRow( row, rmi );
        rowCount++;
        if ( m_stopAfterFirstKRows && rowCount == m_k ) {
          break;
        }
      }

      /* // Construct instances
      Instances data = reservoirToInstances( rmi );

      setupCombo( m_wbX, data, 0, "X" );
      setupCombo( m_wbY, data, 1, "Y" );
      setupCombo( m_wbZ, data, 2, "Z" );
      setupCombo( m_wbC, data, 3, "Color" );
      m_scatterScene.setInstances( data, 0, 1, 2, 3 );
      m_plotComposite.layout( true ); */

      rowsDone();

      db.disconnect();
    } catch ( Exception e ) {
      e.printStackTrace();
    }
  }

  private void setupCombo( CCombo combo, Instances data, int initialIndex, String prefix ) {
    combo.removeAll();

    for ( int i = 0; i < data.numAttributes(); i++ ) {
      String type = data.attribute( i ).isNumeric() ? "Numeric" : "Categorical";
      combo.add( prefix + ": " + "(" + type + ") " + data.attribute( i ).name() );
    }
    if ( initialIndex > data.numAttributes() - 1 ) {
      initialIndex = data.numAttributes() - 1;
    }
    combo.select( initialIndex );
  }

  // RowListener ----------------
  public void rowReadEvent( RowMetaInterface rowMeta, Object[] row ) throws KettleStepException {
    // We don't respond to these (rows incoming to the selected step)
  }

  public void rowWrittenEvent( RowMetaInterface rowMeta, Object[] row ) throws KettleStepException {
    // rows output from the selected step
    if ( m_incomingFields == null ) {
      allocateForIncomingFields( rowMeta.size() );
      setupArffMetas( rowMeta );
    }

    try {
      if ( m_stopAfterFirstKRows && m_currentRow == m_k ) {
        return;
      }
      processRow( row, rowMeta );
    } catch ( Exception e ) {
      throw new KettleStepException( e );
    }
  }

  public void preRows() {
    m_k = m_wNumberOfRows.getSelection();
    m_seed = m_wRandomSeed.getSelection();
    m_stopAfterFirstKRows = m_wbFirstRadio.getSelection();
  }

  public void rowsDone() throws KettleException {
    // Construct instances
    if ( m_rowMeta != null ) {
      Instances data = reservoirToInstances( m_rowMeta );
      m_data = data;

      setupCombo( m_wbX, data, 0, "X" );
      setupCombo( m_wbY, data, 1, "Y" );
      setupCombo( m_wbZ, data, 2, "Z" );
      setupCombo( m_wbC, data, 3, "Color" );
      m_scatterScene.setInstances( data, 0, 1, 2, 3 );
      if ( m_3dCanvas.getScene() == null ) {
        m_3dCanvas.setScene( m_scatterScene.getScene() );
      }
      m_plotComposite.layout( true );

      updateScatter( data, 250, 3, true, !m_isWindows );
    }
  }

  protected void updateScatter( Instances inst, int chartWidth, int markerSize, boolean lowerTriange, boolean image ) {
    Thread t = new Thread() {
      public void run() {
        JPanel scatter = XChartMatrix.getMatrix( inst, chartWidth, markerSize, image, lowerTriange );
        if ( m_scatterHolder.getComponentCount() > 0 ) {
          m_scatterHolder.remove( 0 );
        }
        JScrollPane sp = new JScrollPane();
        sp.setViewportView( scatter );
        m_scatterHolder.add( sp, BorderLayout.CENTER );
        m_scatterHolder.revalidate();
      }
    };
    t.setPriority( Thread.MIN_PRIORITY );
    t.start();
  }

  public void errorRowWrittenEvent( RowMetaInterface rowMeta, Object[] row ) throws KettleStepException {
    // We don't respond to these
  }

  /**
   * Main method for testing this class
   *
   * @param args
   */
  public static void main( String args[] ) {
    try {
      Display display = new Display();
      Shell shell = new Shell( display );
      //shell.setLayout( new FormLayout());
      shell.setLayout( new GridLayout() );

      ScatterPlot3DPluginPerspective perspective = ScatterPlot3DPluginPerspective.getSingleton();
      Composite embedded = perspective.getUI();

      // VisualizePanel3D vis = perspective.getVisualizePanel();
      Instances insts = new Instances( new BufferedReader( new FileReader( "/Users/mhall/datasets/UCI/iris.arff" ) ) );
      // vis.setInstances( insts, 0, 1, 2, 4 );

      AttributeSummarizer atts = perspective.getFieldHistogramsPanel();
      atts.setColoringIndex( 4 );
      atts.setInstances( insts );

      embedded.setParent( shell );

      shell.setSize( 900, 600 );
      //shell.pack();
      shell.open();
      while ( !shell.isDisposed() ) {
        if ( !display.readAndDispatch() )
          display.sleep();
      }
      display.dispose();
    } catch ( Exception ex ) {
      ex.printStackTrace();
    }
  }
}
