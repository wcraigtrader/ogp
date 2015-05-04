package com.akonizo.orientdb

import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.*
import groovy.util.logging.Slf4j

import org.junit.*

import com.tinkerpop.blueprints.*
import com.tinkerpop.blueprints.impls.orient.*

@Slf4j
class GraphPerformanceTest {

    GraphPerformance gp

    Date now = new Date()
    MyNode ann = new MyNode( 'foo', 'ann', 'someone' )
    MyNode bob = new MyNode( 'foo', 'bob', 'someone else' )
    MyEdge light = new MyEdge( 'sees', ann, bob )
    MyEdge heavy = new MyEdge( 'tastes', ann, bob, now, now )

    OrientVertex oann, obob
    OrientEdge edge

    @Before
    void initialize() {
        gp = new GraphPerformance( 'memory:test' ) // 'plocal:/Users/ctrader/db/test' // 'memory:test'
        gp.initialize( 123456789L )
        gp.createDatabase( 'graph' )
    }

    @After
    void cleanup() {
        gp.cleanup()
    }

    @Test
    void testCreateNode() {
        gp.graph = gp.factory.getTx()
        gp.graph.setAutoStartTx( false )

        def node = gp.createNode( ann )
        gp.graph.commit()

        assertThat( node, is( notNullValue() ) )

        def all = gp.graph.getVerticesOfClass( 'foo' ).collect()
        assertThat( all, hasItem( node ) )
        assertThat( all.size(), is( 1 ) )
        assertThat( all[0].getProperty( 'data' ), is( 'someone' ) )
        assertThat( all[0], is( node ) )

        def nodes = gp.graph.getVertices( 'foo.key', 'ann' ).collect()
        assertThat( nodes, hasItem( node ) )
        assertThat( nodes.size(), is( 1 ) )

        def node2 = gp.findNode( ann )
        assertThat( node2, is( node ) )
    }

    @Test
    void testFindEdgeUsingSourceGetEdges() {
        setupTwoNodesWithAnEdge()

        gp.graph = gp.factory.getNoTx()
        def edge2 = gp.findEdgeUsingSourceGetEdges('tastes', oann, obob)
        assertThat( edge2, is( edge ) )
    }

    @Test
    void testFindEdgeUsingGraphGetEdges() {
        setupTwoNodesWithAnEdge()

        gp.graph = gp.factory.getNoTx()
        def edge2 = gp.findEdgeUsingGraphGetEdges('tastes', oann, obob)
        assertThat( edge2, is( edge ) )
    }

    @Test
    void testFindEdgeUsingQuery() {
        setupTwoNodesWithAnEdge()

        gp.graph = gp.factory.getNoTx()
        def edge2 = gp.findEdgeUsingQuery('tastes', oann, obob)
        assertThat( edge2, is( edge ) )
    }

    @Test
    void testFindEdge() {
        setupTwoNodesWithAnEdge()

        gp.graph = gp.factory.getNoTx()
        def edge2 = gp.findEdge('tastes', oann, obob)
        assertThat( edge2, is( edge ) )
    }

    @Test
    void testIngestGraph() {
        SubGraph s = new SubGraph( 1 )
        s.nodes.add( ann )
        s.nodes.add( bob )
        s.edges.add( new MyEdge( 'feels', ann, bob, new Date(), new Date() ) )

        gp.ingestGraph( s )

        def nodes = gp.graph.getVerticesOfClass( 'foo' ).collect()
        assertThat( nodes.size(), is( 2 ) )

        s.edges[0].ended = new Date()
        gp.ingestGraph( s )

        nodes = gp.graph.getVerticesOfClass( 'foo' ).collect()
        assertThat( nodes.size(), is( 2 ) )
    }

    @Test
    void testIngestRadialGraph() {
        SubGraph s = gp.data.getRadialGraph(21)
        gp.ingestGraph( s )
        def nodes = gp.graph.getVerticesOfClass( 'node' ).collect()
        assertThat( nodes.size(), is( 21+1 ) )
    }

    // ----- Helper methods ---------------------------------------------------

    private OrientEdge setupTwoNodesWithAnEdge() {
        gp.graph = gp.factory.getTx()
        gp.graph.setAutoStartTx( false )

        oann = gp.createNode( ann )
        obob = gp.createNode( bob )
        edge = gp.createEdge( heavy, oann, obob )

        gp.graph.commit()
        gp.graph.shutdown()

        return edge
    }

    private addNodePetals( MyNode center ) {
        1.upto(100) {
            def n = gp.data.randomNode('foo')
            gp.findOrCreateNode(n)
            def e = gp.data.randomEdge( center, n, 'tastes' )
            gp.findOrCreateEdge(e)
        }
    }

}