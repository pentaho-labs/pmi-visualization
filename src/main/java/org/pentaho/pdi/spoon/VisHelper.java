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

import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.tableinput.TableInputMeta;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.SpoonPerspectiveManager;
import org.pentaho.di.ui.spoon.trans.TransGraph;
import org.pentaho.ui.xul.impl.AbstractXulEventHandler;

/**
 * @author Mark Hall
 */
public class VisHelper extends AbstractXulEventHandler {

  private static VisHelper instance;

  public String getName() {
    return "scatterPlot3D";
  }

  public static VisHelper getInstance() {
    if ( instance == null ) {
      instance = new VisHelper();
    }

    return instance;
  }

  protected void visualizeRows( StepMetaInterface currentStepMeta ) throws KettleException {
    Spoon spoon = ( (Spoon) SpoonFactory.getInstance() );

    TransGraph tg = spoon.getActiveTransGraph();
    Trans trans = new Trans( tg.getTransMeta() );

    trans.prepareExecution( null );
    String stepName = spoon.getActiveTransGraph().getCurrentStep().getName();
    System.err.println( "Preparing to execute transformation and extract rows from " + stepName );
    StepInterface step = trans.findRunThread( stepName );
    ScatterPlot3DPluginPerspective.getSingleton().resetIncomingFields();
    ScatterPlot3DPluginPerspective.getSingleton().preRows();
    step.addRowListener( ScatterPlot3DPluginPerspective.getSingleton() );
    trans.startThreads();
    trans.waitUntilFinished();
    ScatterPlot3DPluginPerspective.getSingleton().rowsDone();
  }

  public void visualize() {
    try {
      SpoonPerspectiveManager.getInstance().activatePerspective( ScatterPlot3DPluginPerspective.class );
      Spoon spoon = ( (Spoon) SpoonFactory.getInstance() );

      StepMetaInterface currentStepMeta = spoon.getActiveTransGraph().getCurrentStep().getStepMetaInterface();
      if ( !( currentStepMeta instanceof TableInputMeta ) && !( currentStepMeta instanceof TableOutputMeta ) ) {
        // we'll have to run the transformation...
        visualizeRows( currentStepMeta );
        return;
      }

      String tableName = null;
      String sql = null;
      DatabaseMeta dbMeta = null;

      if ( currentStepMeta instanceof TableOutputMeta ) {
        tableName = ( (TableOutputMeta) currentStepMeta ).getTablename();
        dbMeta = ( (TableOutputMeta) currentStepMeta ).getDatabaseMeta();
      } else {
        // has to be a TableInputStep
        sql = ( (TableInputMeta) currentStepMeta ).getSQL();
        dbMeta = ( (TableInputMeta) currentStepMeta ).getDatabaseMeta();
      }

      //((ScatterPlot3DPluginPerspective)SpoonPerspectiveManager.getInstance().getActivePerspective()).setDataSource(tableName, dbMeta);
      ScatterPlot3DPluginPerspective.getSingleton().setDataSource( tableName, sql, dbMeta );

    } catch ( KettleException ex ) {
      ex.printStackTrace();
    }
  }
}
