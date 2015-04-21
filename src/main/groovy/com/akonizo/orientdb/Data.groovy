package com.akonizo.orientdb

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Data {

    final static Logger LOGGER = LoggerFactory.getLogger( Data.class )

    static final int CENTER = 6000
    static final int SPREAD = 4000
    
    static List<String> WORDS = null
    static NODES = ['foo', 'bar', 'baz', 'quux']
    static EDGES = ['sees', 'hears', 'feels', 'smells', 'tastes']

    Random rand

    Data( long seed ) {
        this( new Random( seed ) )
    }

    Data( Random r ) {
        this.rand = r

        if (WORDS == null) {
            LOGGER.debug( "Loading dictionary" )
            WORDS = new ArrayList<String>( 42000 )
            this.getClass().getResourceAsStream( "/words.txt").eachLine { WORDS.add( it ) }
            LOGGER.debug( "Loaded {} words", WORDS.size() )
        }
    }

    /** Return a simple graph that contains N edges */
    def getSimpleGraph( int size=0 ) {
        size = size ?: randomSize()

        def sg = new SubGraph( size )
        def center = randomNode()
        sg.nodes.add( center )
        for (int i=0; i < size; i++ ) {
            def petal = randomNode()
            while (petal == center) {
                LOGGER.debug( "Skipping duplicate: {} == {}", center, petal )
                petal = randomNode()
            }
            sg.nodes.add( petal )
            sg.edges.add( randomEdge( center, petal ) )
        }
        return sg
    }

    int randomSize() {
        return Math.max( 1, CENTER + SPREAD * rand.nextGaussian() )
    }

    def randomNode( ) {
        return new MyNode( random( NODES ), random( WORDS), random( WORDS), random( WORDS ), random( WORDS ) )
    }

    def randomEdge( MyNode source, MyNode target ) {
        def type = random( EDGES )
        if ( type  == "tastes" ) {
            def now = new Date()
            return new MyEdge( type, source, target, now, now )
        }
        return new MyEdge( type, source, target )
    }

    String random( List<String> words ) {
        return words[ rand.nextInt( words.size ) ]
    }

    /** Simple Fibonacci numbers */
    static int fib( int f ) {
        assert f <= 46
        if (f <= 1) return f
        return fib( f-1 ) + fib( f-2 )
    }
}
