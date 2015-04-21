This project is intended to measure performance of OrientDB under specfic conditions.

Build and run with Gradle:

    ./gradlew run
    
To run as a standalone application (for profiling):

    ./gradlew installApp
    build/install/orient-graph-performance/bin/orient-graph-performance

## Theory of Operation

This test suite creates a series of sub-graphs and then ingests those graphs 
into a single graph database, no sharding, no clustering, no replication.

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
a single node and edge, for a given transaction.

## Classes

 * **GraphPerformance** -- This class drives everything else and contains the logic for ingesting graphs.
 * **Database** -- This class creates a fresh database, including schema classes for nodes and edges, and indexes for each class.
 * **Data** -- This class randomly creates sub-graphs, using a seeded random number generator, so every run will result in the same data.
 * **SubGraph**, **MyNode**, **MyEdge** -- These classes are POGOs to model a sub-graph.
 * **PerfCoounter** -- This class captures and formats metrics about each ingest.

## Performance Observations

 * The *average* time to ingest a single node appears to be O(1), regardless of sub-graph size.
 * The *average* time to ingest a single edge appears to be O(E), where E is the number of edges in the sub-graph.
 * Alternately, the time to ingest a sub-graph appears to be O(N+E^2), where N is the number of nodes and E is the number of edges in the sub-graph.
 * Based upon profiling, the ingester is spending 75% or more of its time in `findOrCreateEdge()`, 
   evaluating the iterator returned by `OrientVertex::getEdges()`. This iterator should only return 
   0 or 1 edges, but it takes an inordinate amount of time to do so.
    