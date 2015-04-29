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

    MyNode ann = new MyNode( 'foo', 'ann', 'someone' )
    MyNode bob = new MyNode( 'foo', 'bob', 'someone else' )

    @Before
    void initialize() {
        gp = new GraphPerformance( 'memory:test' ) // 'plocal:/Users/ctrader/db/test'
        gp.initialize( 123456789L )
        gp.createDatabase()
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
}