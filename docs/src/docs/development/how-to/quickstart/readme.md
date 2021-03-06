---
section: Docs
subsection: Development
chapter: How-To
topic: Quick Start
permalink: docs/development/how-to/quickstart/
layout: submenu
---

# Quick Start Guide #

This guide, which provides a short tutorial on the process of creating and configuring an Aperture Tiles project, covers the following topics:

1. Generating a sample data set to analyze
2. Tiling and storing the sample data set
3. Configuring a client to serve and display the tiles in a web browser

At the end of this guide, you will have successfully created an example Aperture Tiles project that displays the points in an example Julia set fractal dataset on an X/Y plot with five zoom levels.

<img src="../../../../img/julia-set.png" class="screenshot" alt="Aperture Tiles Julia Set Project" />

## <a name="prerequisites"></a> Prerequisites ##

To begin this Quick Start example, you must perform the following steps:

1. Download and install the necessary [third-party tools](#third-party-tools).
2. Download and install the [Aperture Tiles Packaged Distribution](#aperture-tiles-utilities).
3. Generate the [Julia set data](#julia-set-data-generation), from which you will later create a set of tiles that will be used in your Aperture Tiles project.

### <a name="third-party-tools"></a> Third-Party Tools ###

Aperture Tiles requires the following third-party tools on your local system:

<div class="props">
    <nav>
        <table class="summaryTable" width="100%">
            <thead >
                <th scope="col" width="20%">Component</th>
                <th scope="col" width="30%">Required</th>
                <th scope="col" width="50%">Notes</th>
            </thead>
            <tr>
                <td style="vertical-align: text-top" class="attributes">Operating System</td>
                <td style="vertical-align: text-top" class="nameDescription">
                    <div class="description">
                        Linux or OS X
                    </div>
                </td>
                <td style="vertical-align: text-top" class="nameDescription">
                    <div class="description">Windows support available with <a href="https://cygwin.com/">Cygwin</a> or DOS command prompt; precludes the use of Hadoop/HBase.</div>
                </td>
            </tr>
            <tr>
                <td style="vertical-align: text-top" class="attributes">Cluster Computing Framework</td>
                <td style="vertical-align: text-top" class="nameDescription">
                    <div class="description">
                        <a href="http://spark.incubator.apache.org//">Apache Spark</a><br>v1.0.0+
                    </div>
                </td>
                <td style="vertical-align: text-top" class="nameDescription">
                    <div class="description">
                        The latest version of Spark may cause class path issues if you compile from source code. We recommend using a pre-built Spark package.
                    </div>
                </td>
            </tr>
        </table>
    </nav>
</div>

If you later intend to create Aperture Tiles projects using particularly large data sets, we recommend you also install the following tools. Otherwise, if your data set is sufficiently small (i.e., it can fit in the memory of a single machine) or if wait times are not an issue, you can simply install and run Spark locally.

<div class="props">
    <nav>
        <table class="summaryTable" width="100%">
            <thead >
                <th scope="col" width="20%">Component</th>
                <th scope="col" width="30%">Required</th>
                <th scope="col" width="50%">Notes</th>
            </thead>
            <tr>
                <td style="vertical-align: text-top" class="attributes">Cluster Computing Framework</td>
                <td style="vertical-align: text-top" class="nameDescription">
                    <div class="description">
                        <a href="http://hadoop.apache.org/">Hadoop</a> (<em>optional</em>):
                        <ul>
                            <li><a href="http://www.cloudera.com/content/cloudera/en/products-and-services/cdh.html">Cloudera</a> v4.6 (<em>recommended)</em></li>
                            <li><a href="http://hadoop.apache.org/docs/r1.2.1/index.html">Apache</a></li>
                            <li><a href="http://www.mapr.com/products/apache-hadoop">MapR</a></li>
                            <li><a href="http://hortonworks.com/">HortonWorks</a></li>
                        </ul>
                    </div>
                </td>
                <td style="vertical-align: text-top" class="nameDescription">
                    <div class="description">
                        Some Hadoop distributions automatically install Apache Spark. Upgrade to v1.0.0+ if the installation is older.
                    </div>
                </td>
            </tr>
        </table>
    </nav>
</div>

### <a name="aperture-tiles-utilities"></a> Aperture Tiles Packaged Distribution ###

Save and unzip the following Aperture Tiles distributions available on the [Download](../../../../download/) section of this website. You will use these utilities to create the Julia set data and provision the example Aperture Tiles project.

- [Tile Generator](../../../../download/#tile-generator): Enables you to create the Julia set data and generate a set of tiles that can be viewed in the Tile Quick Start Application
- [Tile Quick Start Application](../../../../download/#tile-quick-start-application): An example Tile Client application that you can quickly copy and deploy to your web server after minimal modification

The full Aperture Tiles source code, available for download from [GitHub](https://github.com/unchartedsoftware/aperture-tiles/tree/master), is not required for this example. For information on full installations of Aperture Tiles, see the [Installation](../installation/) page.

### <a name="julia-set-data-generation"></a> Julia Set Data Generation ###

For a typical Aperture Tiles project, you will work with your own custom data set. To avoid packaging a large example data set with Aperture Tiles, we have instead provided a simple data set generator. For this demonstration, you will use the provided Tile Generator utility to create the Julia set data.

1. Extract the contents of the [tile-generator.zip](../../../../download/#tile-generator).
2. Execute the standard [spark-submit](http://spark.apache.org/docs/1.0.0/submitting-applications.html) script using the following command, changing the output URI (HDFS or local file system) to specify the location in which you want to save the Julia set data. <p class="list-paragraph">The rest of the flags pass in the correct program main class, data set limits, number of output files (5) and total number of data points (10M) to generate in the Julia set.</p>

```bash
$SPARK_HOME/bin/spark-submit --class com.oculusinfo.tilegen.examples.datagen
.JuliaSetGenerator --master local[2] lib/tile-generation-assembly.jar -real 
-0.8 -imag 0.156 -output datasets/julia -partitions 5 -samples 10000000
```

Check your output folder for 5 part files (`part-00000` to `part-00004`) of roughly equal size (2M records and ~88 MB). These files contain the tab-delimited points in the Julia set you will use Aperture Tiles to visualize.

## <a name="tile-generation"></a> Tile Generation ##

The first step in building any Aperture Tiles project is to create a set of Avro tiles that aggregate your source data across the plot/map and its various zoom levels.

For delimited numeric data sources like the Julia set, the included CSVBinner tool can create these tiles. The CSVBinner tool requires two types of input:

- The [base properties file](#base-property-file-configuration), which describes the general characteristics of your data
- The [tiling properties files](#tiling-property-file-configuration), each of which describes a specific attribute you want to plot and the number of zoom levels

### <a name="base-property-file-configuration"></a> Base Property File Configuration ###

A pre-configured properties file (**julia-base.bd**) can be found in the Tile Generator *examples/* folder. For this example, you only need to edit the base property file if you intend to save your Avro tiles to HBase. Otherwise, you can skip ahead to the [execution](#execution) of the tile generation job.

**NOTE**: For a typical Aperture Tiles project, you will need to edit the additional properties files to define the types of fields in your source data. For more information on these properties, see the [Tile Generation](../generation/) topic on this website.

#### General Input Properties ####

These properties specify the location of your Julia set data.

<div class="props">
    <nav>
        <table class="summaryTable" width="100%">
            <thead >
                <th scope="col" width="20%">Property</th>
                <th scope="col" width="80%">Description</th>
            </thead>
            <tr>
                <td class="attributes"><strong>oculus.binning.source.location</strong></td>
                <td class="attributes">Path of the source data files:
                    <ul>
                        <li>Local system: <em>/data/julia</em></li>
                        <li>HDFS path: <em>hdfs://hadoop.example.com/data/julia</em></li>
                    </ul>
                </td>
            </tr>
        </table>
    </nav>
</div>

#### General Output Properties ####

These properties specify where to save the generated tiles.

<div class="props">
    <nav>
        <table class="summaryTable" width="100%">
            <thead >
                <th scope="col" width="20%">Property</th>
                <th scope="col" width="80%">Description</th>
            </thead>
            <tr>
                <td class="attributes"><strong>oculus.tileio.type</strong></td>
                <td class="attributes">Specify whether the tiles should be saved locally (<em>file</em>) or to HBase (<em>hbase</em>). Local tile IO is supported only for standalone Spark installations.</td>
            </tr>
            <tr>
                <td class="attributes"><strong>oculus.binning.name</strong></td>
                <td class="attributes">Specify the name of the output tile set. If you are writing to a file system, use a relative path instead of an absolute path. Use <em>julia</em> for this example.</td>
            </tr>
        </table>
    </nav>
</div>

#### HBase Connection Details (Optional) ####

These properties should only be included if you are using Hadoop/HDFS and HBase, and are required if you want to run a tile generation job on a multi-computer cluster.

<div class="props">
    <nav>
        <table class="summaryTable" width="100%">
            <thead >
                <th scope="col" width="20%">Property</th>
                <th scope="col" width="80%">Description</th>
            </thead>
            <tr>
                <td class="attributes"><strong>hbase.zookeeper.quorum</strong></td>
                <td class="attributes">Zookeeper quorum location needed to connect to HBase.</td>
            </tr>
            <tr>
                <td class="attributes"><strong>hbase.zookeeper.port</strong></td>
                <td class="attributes">Port through which to connect to zookeeper. Typically defaults to <em>2181</em>.</td>
            </tr>
            <tr>
                <td class="attributes"><strong>hbase.master</strong></td>
                <td class="attributes">Location of the HBase master to which to save the tiles.</td>
            </tr>
        </table>
    </nav>
</div>

### <a name="tiling-property-file-configuration"></a> Tiling Property File Configuration ###

The **julia-tiling.bd** file in your Tile Generator *examples/* folder should not need to be edited. 

**NOTE**: For a typical Aperture Tiles project, you will need to edit properties in this file to define the layout of the map/plot on which to project your data. For more information on these additional properties, see the [Tile Generation](../generation/) topic on this website.

### <a name="execution"></a> Execution ###

With the required properties files, execute the standard spark-submit script again. This time you will invoke the CSVBinner and use the `-d` switch to pass your edited base property file. Tiling property files can be passed in without a switch.

```bash
$SPARK_HOME/bin/spark-submit --class com.oculusinfo.tilegen.examples.apps
.CSVBinner --master local[2] --driver-memory 1G lib/tile-generation-assembly.jar 
-d examples/julia-base.bd examples/julia-tiling.bd
```

When the tile generation is complete, you should have a folder containing six subfolders, each of which corresponds to a zoom level in your project (0, being the highest, through 5, being the lowest). Across all the folders, you should have a total of 1,365 Avro tile files.

For this example, the tile folder will be named `julia.x.y.v`. The output folder is always named using the the following values your property files:

```
[<oculus.binning.prefix>.]<oculus.binning.name>.<oculus.binning.xField>.
<oculus.binning.yField>.<oculus.binning.valueField>
``` 

The `oculus.binning.prefix` value is only included if you set it in the property file. This is useful if you want to run a second tile generation without overwriting the already generated version.

## <a name="tile-server-configuration"></a> Tile Server Configuration ##

For this example, a preconfigured example server application has been provided as part of the Tile Quick Start Application ([tile-quickstart.war](../../../../download/#tile-quick-start-application)). The server renders the layers that are displayed in your Aperture Tiles visualization and passes them to the client.

If you stored your Avro tiles on your local filesystem, zip the *julia.x.y.v* directory produced during the Tile Generation stage and add it to the *WEB-INF/classes/* directory of the Tile Quick Start Application.

For typical Aperture Tiles projects, you will also need to edit the */WEB-INF/***web.xml** and *WEB-INF/classes/***tile.properties** files in the Tile Quick Start Application. For more information on editing these files, see the [Configuration](../configuration/) topic on this website.

### Layer Properties ###

Layer properties (within the **tile-quickstart.war** at *WEB-INF/classes/layers/***julia-layer.json**) specify the layers that can be overlaid on your base map or plot. 

For this example, you only need to edit the layer properties file if you saved your Avro tiles to HBase. Otherwise, you can skip ahead to the configuration of the [Tile Client Application](#tile-client-application). 

<h6 class="procedure">To edit the layer properties for your project</h6>

1. Access the *WEB-INF/classes/layers/***julia-layer.json** file.
2. Make sure the **id** property under the `private` node matches the name given to the HBase table name to which your Avro tiles were generated. For the Julia set example, this should be *julia.x.y.v*.
3. Clear the existing attributes under the `pyramidio` node and add the following HBase connection details:
	- `type`: Enter *hbase*
	- `hbase.zookeeper.quorum`: Zookeeper quorum location needed to connect to HBase (e.g., *my-zk-server1.example.com*, *my-zk-server2.example.com*, *my-zk-server3.example.com*).
	- `hbase.zookeeper.port`: Port through which to connect to zookeeper. Typically defaults to *2181*.
	- `hbase.master`: Location of the HBase master in which the tiles are saved (e.g., *my-hbase-master.example.com:60000*).
4. Save the file.

For information on additional layer properties you can specify, see the Layers section of the [Configuration](../configuration/#layers) topic.

## <a name="tile-client-application"></a> Tile Client Application ##

For this example, a preconfigured example client application has been provided as part of the Tile Quick Start Application ([tile-quickstart.war](../../../../download/#tile-quick-start-application)). The client displays the base map or plot and any layers passed in from the server.

For information on additional map properties you can specify, see the Maps section of the [Configuration](../configuration/#maps) topic, which describes how to configure settings such as boundaries and axes. 

## <a name="deployment"></a> Deployment ##

Once you have finished configuring the map and layer properties, copy the *tile-quickstart.war* to the *webapps/* directory or your web server (e.g., Apache Tomcat or Jetty).

Once your server is running, use your web browser to access the application at `http://localhost:8080/julia-demo`. The Julia set application data is plotted on an X/Y chart with six layers of zoom available.

## Next Steps ##

For a detailed description of the prerequisites and installation procedures for Aperture Tiles, see the [Installation](../installation/) topic.