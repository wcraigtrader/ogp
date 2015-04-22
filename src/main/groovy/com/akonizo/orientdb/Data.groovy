package com.akonizo.orientdb

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Data {

    final static Logger LOGGER = LoggerFactory.getLogger( Data.class )

    static final String WORDLIST = "/words.txt"

    /** Center of Gaussian distribution */
    static final int CENTER = 6000

    /** Spread of Gaussian distribution */
    static final int SPREAD = 4000

    /** Word list for node keys and data */
    static List<String> WORDS = null

    /** Types of nodes */
    static NODES = ['foo', 'bar', 'baz', 'quux']

    /** Types of edges */
    static EDGES = ['sees', 'hears', 'feels', 'smells', 'tastes']

    /** Random number generator */
    Random rand

    /** Seeded constructor */
    Data( long seed ) {
        this( new Random( seed ) )
    }

    /** Random constructor */
    Data( Random r ) {
        this.rand = r

        if (WORDS == null) {
            LOGGER.debug( "Loading dictionary" )
            WORDS = new ArrayList<String>( 42000 )
            this.getClass().getResourceAsStream( WORDLIST ).eachLine { WORDS.add( it ) }
            LOGGER.info( "Loaded {} words", WORDS.size() )
        }
    }

    /** Return a radial graph that contains N edges */
    def getRadialGraph( int size=0 ) {
        size = size ?: randomSize()

        def sg = new SubGraph( size )
        def center = randomNode()
        sg.nodes.add( center )
        for (int i=0; i < size; i++ ) {
            def petal = randomNode()
            while (petal == center) {
                LOGGER.info( "Skipping duplicate: {} == {}", center, petal )
                petal = randomNode()
            }
            sg.nodes.add( petal )
            sg.edges.add( randomEdge( center, petal ) )
        }
        return sg
    }

    /** Return a scatter graph that contains N edges */
    def getScatterGraph( int size=0 ) {
        size = size ?: randomSize()

        def sg = new SubGraph( size )
        for (int i=0; i < size; i++ ) {
            def left = randomNode()
            def right = randomNode()
            while (left == right) {
                LOGGER.info( "Skipping duplicate: {} == {}", left, right )
                right = randomNode()
            }
            if (!(left in sg.nodes )) {
                sg.nodes.add( left )
            }
            if (!(right in sg.nodes )) {
                sg.nodes.add( right )
            }
            sg.edges.add( randomEdge( left, right ) )
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
        assert source != target
        def type = random( EDGES )
        if ( type  == "tastes" ) {
            def now = new Date()
            return new MyEdge( type, source, target, now, now )
        }
        return new MyEdge( type, source, target )
    }

    String random( List<String> words ) {
        return words[ rand.nextInt( words.size() ) ]
    }

    /** Simple Fibonacci numbers */
    static int fib( int f ) {
        assert f <= 46
        if (f <= 1) return f
        return fib( f-1 ) + fib( f-2 )
    }
}
