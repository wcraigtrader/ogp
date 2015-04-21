package com.akonizo.orientdb

import static org.junit.Assert.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.*

import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MyNodeTest {

    final static Logger LOGGER = LoggerFactory.getLogger( MyNodeTest.class )

    @Test
    void testNode0Data() {
        def n = new MyNode( 'foo', 'ann' )
        assertThat( n.type, is( 'foo' ) )
        assertThat( n.key, is( 'ann' ) )
        assertThat( n.data, is ( '' ) )
    }

    @Test
    void testNode1Data() {
        def n = new MyNode( 'foo', 'ann', 'one' )
        assertThat( n.type, is( 'foo' ) )
        assertThat( n.key, is( 'ann' ) )
        assertThat( n.data, is ( 'one' ) )
    }

    @Test
    void testNode2Data() {
        def n = new MyNode( 'foo', 'ann', 'one', 'two' )
        assertThat( n.type, is( 'foo' ) )
        assertThat( n.key, is( 'ann' ) )
        assertThat( n.data, is ( 'one two' ) )
    }

    @Test
    void testNode3Data() {
        def n = new MyNode( 'foo', 'ann', 'one', 'two', 'three' )
        assertThat( n.type, is( 'foo' ) )
        assertThat( n.key, is( 'ann' ) )
        assertThat( n.data, is ( 'one two three' ) )
    }
}