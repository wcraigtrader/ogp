package com.akonizo.orientdb

import static org.junit.Assert.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.*

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
    void testSimpleGraph() {
        def size = 7
        def sg = data.getSimpleGraph( size )
        assertThat( sg.nodes.size(), is( size+1 ) )
        assertThat( sg.edges.size(), is( size ) )
    }

    @Test
    void testSubGraphConnected() {
        def sg = data.getSimpleGraph( 7 )
    }
}
