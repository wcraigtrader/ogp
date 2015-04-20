package com.akonizo.orientdb

import static org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DataTest {

    final static Logger LOGGER = LoggerFactory.getLogger( DataTest.class )

    Data data

    @Before
    void initialize() {
        data = new Data( new Random( 123456789L ) )
    }

    @Test
    void testFibonacci() {
        assertEquals( 'fib(0)', 0, data.fib( 0 ) )
        assertEquals( 'fib(1)', 1, data.fib( 1 ) )
        assertEquals( 'fib(2)', 1, data.fib( 2 ) )
        assertEquals( 'fib(3)', 2, data.fib( 3 ) )
        assertEquals( 'fib(4)', 3, data.fib( 4 ) )
        assertEquals( 'fib(5)', 5, data.fib( 5 ) )
        assertEquals( 'fib(6)', 8, data.fib( 6 ) )
        assertEquals( 'fib(7)', 13, data.fib( 7 ) )
        assertEquals( 'fib(8)', 21, data.fib( 8 ) )
        assertEquals( 'fib(9)', 34, data.fib( 9 ) )
        assertEquals( 'fib(10)', 55, data.fib( 10 ) )
        assertEquals( 'fib(11)', 89, data.fib( 11 ) )
        assertEquals( 'fib(12)', 144, data.fib( 12 ) )
        assertEquals( 'fib(13)', 233, data.fib( 13 ) )
        assertEquals( 'fib(14)', 377, data.fib( 14 ) )
        assertEquals( 'fib(15)', 610, data.fib( 15 ) )
    }

    @Test
    void testWords() {
        assertNotNull( 'WORDS', data.WORDS )
        assertEquals( 'WORDS count', 41131, data.WORDS.size() )
    }

    @Test
    void testNode0Data() {
        def n = new Node( 'foo', 'ann' )
        assertEquals( 'Type', 'foo', n.type )
        assertEquals( 'Key', 'ann', n.key )
        assertEquals( 'Data', '', n.data )
    }

    @Test
    void testNode1Data() {
        def n = new Node( 'foo', 'ann', 'one' )
        assertEquals( 'Type', 'foo', n.type )
        assertEquals( 'Key', 'ann', n.key )
        assertEquals( 'Data', 'one', n.data )
    }

    @Test
    void testNode2Data() {
        def n = new Node( 'foo', 'ann', 'one', 'two' )
        assertEquals( 'Type', 'foo', n.type )
        assertEquals( 'Key', 'ann', n.key )
        assertEquals( 'Data', 'one two', n.data )
    }

    @Test
    void testNode3Data() {
        def n = new Node( 'foo', 'ann', 'one', 'two', 'three' )
        assertEquals( 'Type', 'foo', n.type )
        assertEquals( 'Key', 'ann', n.key )
        assertEquals( 'Data', 'one two three', n.data )
    }
}
