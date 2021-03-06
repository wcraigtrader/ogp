package com.akonizo.orientdb

import org.slf4j.Logger

class PerfCounter {
    static final double MICROS = 1000.0D // Nanoseconds
    static final double MILLIS = 1000 * MICROS
    static final double SECS = 1000 * MILLIS

    int nodes
    int edges

    long start_time
    long node_time
    long edge_time
    long ingest_time
    long commit_time
    long total_time

    double node_average
    double edge_average

    PerfCounter() {
        start_time = System.nanoTime()
    }

    def doneWithNodes() {
        node_time = System.nanoTime() - start_time
    }

    def doneWithEdges() {
        ingest_time = System.nanoTime() - start_time
        edge_time = ingest_time - node_time
    }

    def doneWithCommit() {
        total_time = System.nanoTime() - start_time
        commit_time = total_time - ingest_time

        node_average = nodes == 0 ? 0.0D : 1.0D * node_time / nodes
        edge_average = edges == 0 ? 0.0D : 1.0D * edge_time / edges
    }

    def log( Logger logger, int count ) {
        if (logger.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder()
            sb.append( String.format( '%4d) ', count ) )
            sb.append( String.format( '%6d nodes / %8.3f ms = %.3f ms/node', nodes, node_time/MILLIS, node_average/MILLIS ))
            sb.append( ', ' )
            sb.append( String.format( '%6d edges / %8.3f ms = %.3f ms/edge', edges, edge_time/MILLIS, edge_average/MILLIS ))
            logger.info(sb.toString() )
        }
    }

    String metrics( int count ) {
        return String.format( '%d,%d,%d,%f,%f,%f,%f', 
            count,nodes, edges, 
            node_time/MILLIS, edge_time/MILLIS, 
            node_average/MILLIS, edge_average/MILLIS )
    }
}
