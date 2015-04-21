package com.akonizo.orientdb

import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.orient.*

import org.hyperic.sigar.Sigar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.profiler.Profiler

class GraphPerformance {

    final static Logger LOGGER = LoggerFactory.getLogger( GraphPerformance.class )

    final static String DBPATH = "memory:test"
    final static long SEED = 123456789L
    final static int TRANSACTIONS = 50
    final static String CSV_NAME = "transactions.csv"

    static PrintWriter metrics

    Data data
    String dbpath

    OrientGraphFactory factory
    OrientBaseGraph graph

    Date timestamp
    PerfCounter perf

    /** Constructor */
    GraphPerformance( String path ) {
        dbpath = path
    }

    /** Initialize the data provider */
    def initialize( long seed ) {
        data = new Data( seed )
    }

    /** Create a fresh database, with schema and indexes */
    def createDatabase() {
        Database.delete_database( dbpath )
        Database.create_database( dbpath )
        Database.create_schema( dbpath )
        Database.create_indexes( dbpath )

        factory = new OrientGraphFactory(dbpath, 'admin', 'admin').setupPool(1, 10)
    }

    /** Ingest some sub-graphs from the data provider */
    def ingestData() {
        for ( int i=1; i <= TRANSACTIONS; i++) {
            ingestGraph( data.getRadialGraph(), i )
        }
    }

    /** Ingest a single sub-graph, recording performance data */
    def ingestGraph( SubGraph sg, int count=0 ) {
        try {
            timestamp = new Date()
            graph = factory.getTx()
            graph.setAutoStartTx( false )

            perf = new PerfCounter()
            perf.nodes = sg.nodes.size()
            perf.edges = sg.edges.size()

            for ( MyNode node : sg.nodes ) {
                findOrCreateNode( node )
            }
            perf.doneWithNodes()

            for ( MyEdge edge : sg.edges ) {
                findOrCreateEdge( edge )
            }
            perf.doneWithEdges()

            graph.commit()
            perf.doneWithCommit()

            perf.log( LOGGER )
            metrics?.print( count )
            metrics?.print( ',' )
            metrics?.println( perf.metrics() )
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

    /** Find an existing node, or create a new one, updating all properties */
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

    /** Locate a node by key, using the class-specific key index */
    OrientVertex findNode(MyNode n) {
        OrientVertex node = null

        def indexname = "${n.type}.key"
        for (Vertex vraw : graph.getVertices( indexname, n.key ) ) {
            node = (OrientVertex) vraw
            break
        }
        return node
    }

    /** Create a new node */
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

    /** Update an existing node */
    void updateNode( OrientVertex node, MyNode n ) {
        LOGGER.info( "Updating node {}", n )
        timestamp = timestamp ?: new Date()
        def map = n.getProps()
        map.remove( 'key' )
        map[ 'updated' ] = timestamp
        node.setProperties( map )
        node.save()
    }

    /** Find an existing edge, or create a new one, updating all properties */
    OrientEdge findOrCreateEdge( MyEdge e, boolean create=true ) {
        LOGGER.debug( "${create ? 'create' : 'find' } ${e} ")
        OrientEdge edge = null

        OrientVertex src = findNode( e.source )
        OrientVertex tgt = findNode( e.target )
        if ( src == tgt ) {
            throw new IngestException ( "No loopback edges allowed" )
        }

        // NOTE: This is where the ingester spends 75+% of its time!!! 
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

    /** Create a new edge */
    OrientEdge createEdge( MyEdge e, OrientVertex src, OrientVertex tgt ) {
        OrientEdge edge = src.addEdge( e.type, tgt )
        if ( e.began ) edge.setProperty( "began", e.began )
        if ( e.ended ) edge.setProperty( "ended", e.ended )
        edge.save()
        return edge
    }

    /** Update an existing edge */
    void updateEdge( OrientEdge edge, MyEdge e ) {
        LOGGER.info("Updating edge {}", e )
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

    /** Cleanly close the database */
    def cleanup() {
        graph?.shutdown()
        graph = null
        if (dbpath.startsWith( 'memory:') ) {
            factory?.drop()
        }
        factory?.close()
        factory = null
    }

    /** Log useful system metrics */
    static void logSystemInformation() {
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
        logSystemInformation()

        // Create a spreadsheet for capturing metrics
        metrics = new PrintWriter( new FileWriter( new File( CSV_NAME ) ) )
        metrics.println('Chunk,Nodes,Edges,Node Time (ms),Edge Time (ms),Average Node Time (ms),Average Edge Time (ms)')


        Profiler profiler = new Profiler( "GraphPerformance" )
        GraphPerformance gp = new GraphPerformance( DBPATH )
        try {
            profiler.start( "Initialize data" )
            gp.initialize( SEED )

            profiler.start("Create database")
            gp.createDatabase()

            profiler.start("Ingest data")
            gp.ingestData()

        } finally {
            profiler.start( "Database cleanup" )
            gp.cleanup()

            profiler.stop()
            LOGGER.info( "{}", profiler )
            metrics?.close()
        }
    }
}