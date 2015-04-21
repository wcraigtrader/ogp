package com.akonizo.orientdb

import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.orient.*

import org.hyperic.sigar.Sigar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.profiler.Profiler

class GraphPerformance {

    final static Logger LOGGER = LoggerFactory.getLogger( GraphPerformance.class )

    static Profiler PROFILER
    static PrintWriter csv
    
    final static int TRANSACTIONS = 50
    final static String CSV_NAME = "transactions.csv"

    Data data
    String dbpath

    OrientGraphFactory factory
    OrientBaseGraph graph

    Date timestamp
    PerfCounter perf

    GraphPerformance( String path ) {
        dbpath = path

    }

    def initialize( long seed ) {
        data = new Data( seed )
    }

    def createDatabase() {
        Database.delete_database( dbpath )
        Database.create_database( dbpath )
        Database.create_schema( dbpath )
        Database.create_indexes( dbpath )

        factory = new OrientGraphFactory(dbpath, 'admin', 'admin').setupPool(1, 10)
    }

    def ingestData() {
        1.upto( TRANSACTIONS ) { count ->
            ingestGraph( data.getSimpleGraph(), count )
        }
    }

    def ingestGraph( SubGraph sg, int count=0 ) {
        try {
            timestamp = new Date()
            graph = factory.getTx()
            graph.setAutoStartTx( false )

            perf = new PerfCounter()
            perf.nodes = sg.nodes.size()
            perf.edges = sg.edges.size()

            sg.nodes.each { findOrCreateNode( it ) }
            perf.doneWithNodes()

            sg.edges.each { findOrCreateEdge( it ) }
            perf.doneWithEdges()

            graph.commit()
            perf.doneWithCommit()

            perf.log( LOGGER )
            csv?.print( count )
            csv?.print( ',' )
            csv?.println( perf.csv() )
            
        } catch (IngestException e ) {
            LOGGER.error( "During ingest: {}", e.getMessage() )
            graph.rollback()
        } catch (Exception e ) {
            LOGGER.error( "During ingest", e )
            graph.rollback()
        } finally {
            timestamp = null
        }
    }

    OrientVertex findNode(MyNode n) {
        OrientVertex node = null

        def indexname = "${n.type}.key"
        for (Vertex vraw : graph.getVertices( indexname, n.key ) ) {
            node = (OrientVertex) vraw
            break
        }
        return node
    }

    OrientVertex findOrCreateNode( MyNode n, boolean create=true ) {
        LOGGER.debug( "${create ? 'create' : 'find' } ${n} ")
        OrientVertex node = findNode(n)

        if (node == null) {
            if (create) {
                node = createNode( n )
            } else {
                throw new IngestException( "Did not find ${n}" )
            }
        } else {
            updateNode( node, n )
        }

        return node
    }

    OrientVertex createNode( MyNode n ) {
        timestamp = timestamp ?: new Date()
        OrientVertex node = graph.addVertex( OrientBaseGraph.CLASS_PREFIX + n.type )
        def map = n.getProps()
        map[ 'created' ] = timestamp
        map[ 'updated' ] = timestamp
        // LOGGER.debug( "Creating {} from {}", n, map )
        node.setProperties( map )
        node.save()
        return node
    }

    void updateNode( OrientVertex node, MyNode n ) {
        timestamp = timestamp ?: new Date()
        def map = n.getProps()
        map.remove( 'key' )
        map[ 'updated' ] = timestamp
        // LOGGER.debug( "Updating {} to {}", n, map )
        node.setProperties( map )
        node.save()
    }

    OrientEdge findOrCreateEdge( MyEdge e, boolean create=true ) {
        LOGGER.debug( "${create ? 'create' : 'find' } ${e} ")
        OrientEdge edge = null

        OrientVertex src = findNode( e.source )
        OrientVertex tgt = findNode( e.target )
        if ( src == tgt ) {
            throw new IngestException ( "No loopback edges allowed" )
        }

        for (Edge eraw : src.getEdges( tgt, Direction.BOTH, e.type ) ) {
            if ( eraw.getLabel() == e.type ) {
                if (edge != null) {
                    throw new IngestException( "Multiple edges match ${e}" )
                }
                edge = (OrientEdge) eraw
            }
        }

        if (edge == null) {
            if (create) {
                edge = createEdge( e, src, tgt )
            } else {
                throw new IngestException( "Did not find ${e}")
            }
        } else {
            updateEdge( edge, e )
        }

        return edge
    }

    OrientEdge createEdge( MyEdge e, OrientVertex src, OrientVertex tgt ) {
        OrientEdge edge = src.addEdge( e.type, tgt )
        if ( e.began ) edge.setProperty( "began", e.began )
        if ( e.ended ) edge.setProperty( "ended", e.ended )
        edge.save()
        return edge
    }

    void updateEdge( OrientEdge edge, MyEdge e ) {
        def updated = false
        if (e.began != null) {
            def began = edge.getProperty( "began" )
            if (began == null || e.began < began ) {
                edge.setProperty( "began", e.began )
                updated = true
            }
        }
        if (e.ended != null) {
            def ended = edge.getProperty( "ended" )
            if (ended == null || e.ended > ended ) {
                edge.setProperty( "ended", e.ended )
                updated = true
            }
        }
        if (updated) {
            edge.save()
        }
    }

    def cleanup() {
        graph?.shutdown()
        graph = null
        if (dbpath.startsWith( 'memory:') ) {
            factory?.drop()
        }
        factory?.close()
        factory = null
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
            LOGGER.error( "Alas, Sigar failed to collect system information", e )
        }
    }

    static void main( String[] args ) {
        csv = new PrintWriter( new FileWriter( new File( CSV_NAME ) ) )
        csv.println('Chunk,Nodes,Edges,Node Time (ms),Edge Time (ms),Average Node Time (ms),Average Edge Time (ms)')
        GraphPerformance gp = new GraphPerformance( "memory:test" )
        try {
            gp.logSystemInformation()
            PROFILER = new Profiler( "GraphPerformance" )
            PROFILER.start( "Initialize" )
            gp.initialize( 123465789L )
            PROFILER.start("Create database")
            gp.createDatabase()
            PROFILER.start("Ingest data")
            gp.ingestData()
        } finally {
            PROFILER.start( "Database cleanup" )
            gp.cleanup()
            PROFILER.stop()
            LOGGER.info( "{}", PROFILER )
            csv?.close()
        }
    }
}