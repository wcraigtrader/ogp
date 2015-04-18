package com.akonizo.orientdb

import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory

import org.hyperic.sigar.Sigar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.profiler.Profiler

class GraphPerformance {

    final static Logger LOGGER = LoggerFactory.getLogger( GraphPerformance.class )
    static Profiler PROFILER

    Random random
    String dbpath

    OrientGraphFactory factory
    OrientBaseGraph graph

    def initialize( long seed ) {
        random = new Random( seed )
    }

    def createDatabase( String path ) {
        dbpath = path
    }

    def cleanup() {
    }

    def logSystemInformation() {
        def vendor = System.properties.get('java.vendor')
        def version = System.properties.get( 'java.version')
        LOGGER.info( 'Java: {} {}', vendor, version )

        try {
            def sigar = new Sigar()
            LOGGER.info( 'System:  {}', sigar.getCpuInfoList() )
            LOGGER.info( 'Memory:  {}', sigar.getMem() )
        } catch ( Exception e ) {
            LOGGER.error( "Sigar failed", e )
        }
    }

    static void main( String[] args ) {
        GraphPerformance gp = new GraphPerformance()
        try {
            gp.logSystemInformation()
            PROFILER = new Profiler( "GraphPerformance" )
            gp.initialize( 123465798L )
            PROFILER.start("Create database")
            gp.createDatabase("memory:test")
        } finally {
            PROFILER.start( "Database cleanup" )
            gp.cleanup()
            PROFILER.stop()
            LOGGER.info( "{}", PROFILER )
        }
    }
}