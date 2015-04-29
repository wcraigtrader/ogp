This project is intended to measure performance of OrientDB under specific conditions. Those conditions are similar to those of a proprietary application that is much larger and complex, and not sharable.

Build and run with Gradle:

    ./gradlew run [ -Pmodel=radial|scatter|sprawl|mixed ] [ -POV=<orient version> ] [ -PGV=<groovy version> ] 

or to run a series of models under varying conditions:

    ./batch-run-models

This has been tested with Orient versions 1.7.8 and 2.0.7, and Groovy versions 1.8.9 and 2.4.3.

To run as a standalone application (for profiling):

    ./gradlew installApp [ -POV=<orient version> ] [ -PGV=<groovy version> ]
    build/install/orient-graph-performance/bin/orient-graph-performance [ radial|scatter|sprawl|mixed ]

Detailed data will be written to CSV files in the `results` directory.  The CSV files are manually combined into Excel worksheets with graphs for better visualization of the data. Logs are displayed and written to `performance.log`.

## Performance Observations

 * Orient 2.0.7 performs better than 1.8.9, in general.
 * Groovy 2.4.3 performs better than 1.8.9, when run without invokedynamic.
 * The *average* time to ingest a single node *appears* to be O(1), regardless of sub-graph size -- **this is good**.
 * The *average* time to ingest a single edge *appears* to be O(E), where E is the number of edges already connected to the source node.
 * The *average* time to ingest a single heavyweight edge appears to be 2.85 times longer than for lightweight edges.
 * Based upon profiling, the ingester is spending 75% or more of its time in `findOrCreateEdge()`, 
   evaluating the iterator returned by `OrientVertex::getEdges()`. This iterator should only return 
   0 or 1 edges, but it takes an inordinate amount of time to do so.
 * The first dozen ingest operations have poor performance as the Java JIT is getting up-to-speed and optimizing.

## Theory of Operation

This test suite creates a series of sub-graphs and then ingests those graphs 
into a single (in-memory) graph database, no sharding, no clustering, no replication.

 * Application node classes are descended from the Node class.
 * Application nodes are indexed by key, in separate indexes for each class.
 * Application edge classes are descended from the Edge class.
 * Most edges are lightweight, but the Edge class includes properties.
 * For any sub-graph, all of the nodes are ingested first, and then all of the edges are ingested.
 * Each sub-graph is ingested in its own transaction.
 * If a given node already exists, then its properties will be updated from the sub-graph.
 * There can be at most one edge of any given type between any two nodes. 
   (There can be multiple edges of different types between a pair of nodes, but at most one of each type).

The suite measures the time in milliseconds necessary to ingest all of the nodes and 
all of the edges for a given sub-graph and calculates the average time to ingest 
a single node and edge, for a given transaction. The time to create the sub-graph is not included in the measurements.

## Data Models

There are a number of models that represent different types of sub-graphs that can be ingested by the application. Each run will ingest 500 sub-graphs, based upon the selected data model. While the sub-graphs are randomly generated, each run uses a fixed seed, so each set of graphs will be repeatable. The data models for individual graphs are:

 * **radial** -- This produces a single central node, and then a number of nodes, each connected to the central node.
 * **scatter** -- This produces a collection of small graphs, not connected.
 * **sprawl** -- This produces a chain of nodes, with various branches along the way.
 
 There are specific patterns of data available as well:
 
 * **mixed** -- Starts with a large radial sub-graph, then a series of increasingly larger scatter sub-graphs.
 * **fixed1** -- 100 radial(500) + 100 radial(600) + 100 radial(700) + 100 radial(800) + 100 radial(900). *This model demonstrates the O(E) performance of the getEdges() method*.

## Classes

 * **GraphPerformance** -- This class drives everything else and contains the logic for ingesting graphs.
 * **Database** -- This class creates a fresh database, including schema classes for nodes and edges, and indexes for each class.
 * **Data** -- This class randomly creates sub-graphs, using a seeded random number generator, so every run will result in the same data.
 * **SubGraph**, **MyNode**, **MyEdge** -- These classes are POGOs to model a sub-graph.
 * **PerfCoounter** -- This class captures and formats metrics about each ingest.
    