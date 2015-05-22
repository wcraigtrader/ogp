package com.akonizo.orientdb

import groovy.util.logging.Slf4j

import org.hyperic.sigar.Sigar
import org.slf4j.profiler.Profiler

import com.orientechnologies.orient.core.index.OCompositeKey
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.orient.*

@Slf4j
class GraphPerformance {

    final static String DBPATH = 'memory:test'
    final static long SEED = 123456789L
    final static int TRANSACTIONS = 500
    final static String RESULTS_DIR = 'results'
    final static String DEFAULT_METRICS_FILE = 'transactions.csv'

    static PrintWriter metrics
    static String javaName, groovyName, orientName, osName

    Data data
    String dbpath
    String indexes

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
        log.info( "initialize( ${seed} )" )
        data = new Data( seed )
        log.info('initialized')
    }

    /** Create a fresh database, with schema and indexes */
    def createDatabase( String mode=null ) {
        indexes = mode
        log.info( "createDatabase( ${dbpath}, ${indexes} )" )
        Database.delete_database( dbpath )
        Database.create_database( dbpath, indexes!=null )
        Database.create_schema( dbpath, indexes!=null )
        Database.create_indexes( dbpath, indexes!=null )

        factory = new OrientGraphFactory(dbpath, 'admin', 'admin').setupPool(1, 10)
    }

    /** Ingest some sub-graphs from the data provider */
    def ingestData( String model ) {
        log.info("Start ingesting data from ${model}" )
        int i = 1
        for (SubGraph sg : data.getGraphs( model, TRANSACTIONS ) ) {
            ingestGraph( sg, i++ )
        }
        log.info('Ingestion complete' )
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
            log.debug( 'Loading graph with {} nodes and {} edges', perf.nodes, perf.edges )

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

            perf.log( log, count )
            metrics?.println( perf.metrics( count ) )
        } catch (IngestException e ) {
            log.error( 'During ingest: {}', e.getMessage() )
            graph.rollback()
        } catch (Exception e ) {
            log.error( 'During ingest', e )
            graph.rollback()
        } finally {
            timestamp = null
        }
    }

    /** Find an existing node, or create a new one, updating all properties */
    OrientVertex findOrCreateNode( MyNode n, boolean create=true ) {
        OrientVertex node = findNode(n)

        if (node == null) {
            if (create) {
                node = createNode( n )
            } else {
                throw new IngestException( 'Did not find ${n}' )
            }
        } else {
            if (create) {
                updateNode( node, n )
            }
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
        log.debug( "Creating node ${n}" )
        timestamp = timestamp ?: new Date()
        OrientVertex node = graph.addVertex( OrientBaseGraph.CLASS_PREFIX + n.type )
        def map = n.getProps()
        map[ 'created' ] = timestamp
        map[ 'updated' ] = timestamp
        node.setProperties( map )
        node.save()
        return node
    }

    /** Update an existing node */
    void updateNode( OrientVertex node, MyNode n ) {
        log.debug( "Updating node ${n}" )
        timestamp = timestamp ?: new Date()
        def map = n.getProps()
        map.remove( 'key' )
        map[ 'updated' ] = timestamp
        node.setProperties( map )
        node.save()
    }

    /** Find an existing edge, or create a new one, updating all properties */
    OrientEdge findOrCreateEdge( MyEdge e, boolean create=true ) {
        OrientVertex src = findNode( e.source )
        if (src == null) {
            throw new IngestException( String.format('Unable to find %s', e.source) )
        }
        OrientVertex tgt = findNode( e.target )
        if (tgt == null) {
            throw new IngestException( String.format('Unable to find %s', e.target) )
        }
        if ( src == tgt ) {
            throw new IngestException ( String.format( 'No loopback edges allowed (%s == %s)', src, tgt ) )
        }

        // Note: findEdge implementation depends upon edge indexing method
        OrientEdge edge = null
        switch (indexes) {
            case 'query' :
                edge = findEdgeUsingQuery( e.type, src, tgt )
                break
            case 'graph' :
                edge = findEdgeUsingGraphGetEdges( e.type, src, tgt )
                break
            default:
                edge = findEdgeUsingSourceGetEdges( e.type, src, tgt )
                break
        }

        if (edge == null) {
            if (create) {
                edge = createEdge( e, src, tgt )
            } else {
                throw new IngestException( "Did not find ${e}")
            }
        } else {
            if (create) {
                updateEdge( edge, e )
            }
        }

        return edge
    }

    // /** Find an existing edge */
    // OrientEdge findEdge(String type, OrientVertex src, OrientVertex tgt ) {
    //     // NOTE: This is where the ingester spends 75+% of its time!!!
    //     for (Edge eraw : src.getEdges( tgt, Direction.BOTH, type ) ) {
    //         return (OrientEdge) eraw
    //     }
    //     return null
    // }

    /** Find an existing edge
     * This works, but doesn't use edge indexes */
    OrientEdge findEdgeUsingSourceGetEdges(String type, OrientVertex src, OrientVertex tgt ) {
        // NOTE: This is where the ingester spends 75+% of its time!!!
        for (Edge eraw : src.getEdges( tgt, Direction.BOTH, type ) ) {
            return (OrientEdge) eraw
        }
        return null
    }

    /** Find an existing edge
     * This appears to be doing the right thing, but doesn't work */
    OrientEdge findEdgeUsingGraphGetEdges(String type, OrientVertex src, OrientVertex tgt ) {
        for (Edge eraw : graph.getEdges( "${type}.unique", new OCompositeKey( [src.id, tgt.id] ) ) ) {
            return (OrientEdge) eraw
        }
        return null
    }

    /** Find an existing edge */
    OrientEdge findEdgeUsingQuery(String type, OrientVertex src, OrientVertex tgt ) {
        def cmd = new OCommandSQL("select from index:${type}.unique where key=?")
        def key = new OCompositeKey( [src.id, tgt.id] )
        for (Vertex result : graph.command( cmd ).execute( key )) {
            return (OrientEdge) result.getProperty( 'rid' )
        }
        return null
    }

    /** Find an existing edge */
    OrientEdge findEdgeUsingTraverse(MyEdge e, OrientVertex src, OrientVertex tgt ) {
        return null
    }

    /** Create a new edge */
    OrientEdge createEdge( MyEdge e, OrientVertex src, OrientVertex tgt ) {
        log.debug( "Creating edge ${e}" )
        OrientEdge edge = src.addEdge( e.type, tgt )
        if ( e.began ) edge.setProperty( 'began', e.began )
        if ( e.ended ) edge.setProperty( 'ended', e.ended )
        edge.save()
        return edge
    }

    /** Update an existing edge */
    void updateEdge( OrientEdge edge, MyEdge e ) {
        log.debug("Updating edge ${e}" )
        def updated = false
        if (e.began != null) {
            def began = edge.getProperty( 'began' )
            if (began == null || e.began < began ) {
                edge.setProperty( 'began', e.began )
                updated = true
            }
        }
        if (e.ended != null) {
            def ended = edge.getProperty( 'ended' )
            if (ended == null || e.ended > ended ) {
                edge.setProperty( 'ended', e.ended )
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
    static void gatherSystemInformation() {
        try {
            def sigar = new Sigar()
            log.info( "System:  ${sigar.getCpuInfoList()[0]}" )
            log.info( "Memory:  ${sigar.getMem()}" )
        } catch ( Exception e ) {
            log.error( 'Alas, Sigar failed to collect system information', e )
        }

        def vendor = System.properties.get('java.vendor')
        def version = System.properties.get( 'java.version')
        def gv = Closure.class.package.implementationVersion
        log.info( "Java: ${vendor} ${version}, Groovy: ${gv}" )

        def orient = OrientGraphFactory.class.package.implementationVersion
        log.info( "OrientDB: ${orient}" )

        // def buffer = new ByteArrayOutputStream()
        // OGlobalConfiguration.dumpConfiguration( new PrintStream( buffer ) )
        // buffer.toString().split( '\n' ).each { log.info( it ) }

        osName = System.getProperty( 'os.name' ).replace( ' ', '').toLowerCase()
        javaName = version.split('_')[0].replace('.', '' )
        groovyName = gv.replace('.', '')
        orientName = orient.replace( '.', '' )
    }

    static void main( String[] args ) {
        def indexes = null
        def dbpath = DBPATH
        def model = 'radial'
        switch (args.length) {
            case 3:
                indexes = args[2]
            case 2:
                dbpath = args[1]
            case 1:
                model = args[0]
        }

        log.info( "Command line args: ${args}" )
        log.info( "Subgraph model: ${model}" )
        log.info( "DB path: ${dbpath}" )
        log.info( "Indexes: ${indexes}" )

        gatherSystemInformation()

        // Create a spreadsheet for capturing metrics
        File resultsDir = new File( RESULTS_DIR )
        if (!resultsDir.exists() ) {
            resultsDir.mkdirs()
        }
        String metricsFileName = "${osName}-${model}-${orientName}-${groovyName}-${javaName}.csv"
        File metricsFile = new File( resultsDir, metricsFileName )
        log.info("Logging metrics to ${metricsFile.canonicalPath}" )

        metrics = new PrintWriter( new FileWriter( metricsFile ) )
        metrics.println('Chunk,Nodes,Edges,Node Time (ms),Edge Time (ms),Average Node Time (ms),Average Edge Time (ms)')

        Profiler profiler = new Profiler( 'GraphPerformance' )
        GraphPerformance gp = new GraphPerformance( dbpath )
        try {
            profiler.start( 'Initialize data' )
            gp.initialize( SEED )

            profiler.start('Create database')
            gp.createDatabase( indexes )

            profiler.start('Ingest data')
            gp.ingestData( model )

        } finally {
            profiler.start( 'Database cleanup' )
            metrics?.close()

            gp.cleanup()

            profiler.stop()
            log.info( "${profiler}" )
        }
    }
}