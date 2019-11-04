Plugin Machine Intelligence Visualizations
==========================================

This project provides visualization capabilities to the PMI project. Two types of visualization are provided initially, in the form of a plugin Spoon perspective. These are a scatter plot matrix and a JavaFX-based 3D scatter plot visualization. Java 8 must be used to run PDI/Spoon in order for the 3D visualization to be available.

Note, while we include this visualization capability under the PMI umbrella, the plugin can actually be used independently of PMI.

Building
--------
The PMI Visualization Plugin is built with Maven.

    $ git clone https://github.com/pentaho-labs/pmi-visualization.git
    $ cd pmi-visualization
    $ mvn install

This will produce a plugin archive in target/pmi-visualization-${project.revision}.zip. This archive can then be extracted into the "spoon" subdirectory in your Pentaho Data Integration plugins directory. If the "spoon" subdirectory does not exist, you can just create it.

License
-------
Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.0. See LICENSE.txt for more information.
