package com.akonizo.orientdb

import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import org.hyperic.sigar.Sigar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.hyperic.sigar.CpuInfo

class GraphPerformance {

    final static Logger LOGGER = LoggerFactory.getLogger( GraphPerformance.class )

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

    def showSysData() {
        try {
            def sigar = new Sigar()
            def infos = sigar.getCpuInfoList()
            LOGGER.info( 'System: {}', infos[0] )

        } catch ( Exception e ) {
            LOGGER.error( "Sigar", e )
        }
    }

    static void main( String[] args ) {
        GraphPerformance gp = new GraphPerformance()
        try {
            gp.showSysData()
            gp.initialize( 123465798L )
            gp.createDatabase("memory:test")
            LOGGER.info("Start graph performance test")

            LOGGER.info("Test complete")
        } finally {
            gp.cleanup()
        }
    }
}