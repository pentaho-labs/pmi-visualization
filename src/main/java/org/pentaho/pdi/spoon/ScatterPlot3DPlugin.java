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

import org.pentaho.di.ui.spoon.*;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulException;

/**
 * Spoon plugin that provides a 3D scatter plot and histogram
 * matrix perspective.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
@SpoonPlugin(id = "ScatterPlot3D", image = "")
@SpoonPluginCategories({"spoon", "trans-graph"})
public class ScatterPlot3DPlugin implements SpoonPluginInterface {
  
  public ScatterPlot3DPlugin() {    
  }

  /**
   * Provides an optional SpoonLifecycleListener to be notified of Spoon startup and shutdown.
   * 
   * @return optional SpoonLifecycleListener
   */
  public SpoonLifecycleListener getLifecycleListener() {
    return null;
  }
  
  /**
   * Provides an optional SpoonPerspective.
   * 
   * @return optional SpoonPerspective
   */
  public SpoonPerspective getPerspective() {
    return ScatterPlot3DPluginPerspective.getSingleton();
  }

  @Override
  public void applyToContainer(String category, XulDomContainer container)
      throws XulException {

    container.registerClassLoader(getClass().getClassLoader());
    if (category.equals("trans-graph")){
      container.loadOverlay("org/pentaho/pdi/spoon/trans_overlay.xul");
      container.addEventHandler(VisHelper.getInstance());
    }
  }
}

