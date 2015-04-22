package com.akonizo.orientdb

import org.apache.commons.math3.random.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Data {

    final static Logger LOGGER = LoggerFactory.getLogger( Data.class )

    static final String WORDLIST = "/words.txt"

    /** Center of Gaussian distribution */
    static final int CENTER = 500

    /** Spread of Gaussian distribution */
    static final int SPREAD = 100

    /** Word list for node keys and data */
    static List<String> WORDS = null

    /** Types of nodes */
    static NODES = ['foo', 'bar', 'baz', 'quux']

    /** Types of edges */
    static EDGES = ['sees', 'hears', 'feels', 'smells', 'tastes']

    /** Random number generator */
    RandomGenerator rand

    /** Seeded constructor */
    Data( long seed ) {
        this( new MersenneTwister( seed ) )
    }

    /** Random constructor */
    Data( RandomGenerator r ) {
        this.rand = r

        if (WORDS == null) {
            LOGGER.debug( "Loading dictionary" )
            WORDS = new ArrayList<String>( 100000 )
            this.getClass().getResourceAsStream( WORDLIST ).eachLine { WORDS.add( it ) }
            LOGGER.info( "Loaded {} words", WORDS.size() )
        }
    }

    /** Return an iterator for a graph model */
    Iterator<SubGraph> getGraphs( String model, int count ) {
        switch ( model ) {
            case 'radial':
                return radialGraphIterator( count )
            case 'scatter':
                return scatterGraphIterator( count )
            case 'sprawl':
                return sprawlGraphIterator( count )
            case 'zaim' :
                return zaimGraphIterator( count )
            default:
                throw new Exception( "Unrecognized graph model ($model)" )
        }
    }

    /** Return an iterator for a radial graph model */
    Iterator<SubGraph> radialGraphIterator( int count ) {
        assert count > 0
        return new Iterator<SubGraph>() {
            int remaining = count
            boolean hasNext() { remaining > 0 }
            SubGraph next() { 
                if (!hasNext() ) throw new NoSuchElementException()
                remaining--
                return getRadialGraph( CENTER, SPREAD ) 
            }
            void remove() { throw new UnsupportedOperationException() }
        }
    }
        
    /** Return an iterator for a scattered graph model */
    Iterator<SubGraph> scatterGraphIterator( int count ) {
        assert count > 0
        return new Iterator<SubGraph>() {
            int remaining = count
            boolean hasNext() { remaining > 0 }
            SubGraph next() { 
                if (!hasNext() ) throw new NoSuchElementException()
                remaining--
                return getScatterGraph( CENTER, SPREAD ) 
            }
            void remove() { throw new UnsupportedOperationException() }
        }
    }

    /** Return an iterator for a sprawled graph model */
    Iterator<SubGraph> sprawlGraphIterator( int count ) {
        assert count > 0
        return new Iterator<SubGraph>() {
            int remaining = count
            boolean hasNext() { remaining > 0 }
            SubGraph next() { 
                if (!hasNext() ) throw new NoSuchElementException()
                remaining--
                return getSprawlGraph( CENTER, SPREAD ) 
            }
            void remove() { throw new UnsupportedOperationException() }
        }
    }

    /** Return an iterator for a nexus graph model */
    Iterator<SubGraph> zaimGraphIterator( int count ) {
        assert count > 0
        return new Iterator<SubGraph>() {
            int position = 0, max = count
            boolean hasNext() { position < max }
            SubGraph next() { 
                if (!hasNext() ) throw new NoSuchElementException()
                switch (position++) {
                    case 0: return getRadialGraph( 20000, 50 )
                    case 1..100: return getScatterGraph( 300, 60 )
                    case 101..200: return getScatterGraph( 400, 80 )
                    case 201..300: return getScatterGraph( 500, 100 )
                    case 301..400: return getScatterGraph( 600, 120 )
                    default: return getScatterGraph( 700, 140 )
                }
            }
            void remove() { throw new UnsupportedOperationException() }
        }
    }
        
    /** Return a radial graph that contains a random spread of edges */
    def getRadialGraph( int center, int spread ) {
        return getRadialGraph( randomSize( center, spread ) )
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

    /** Return a scatter graph that contains a random spread of edges */
    def getScatterGraph( int center, int spread ) {
        return getScatterGraph( randomSize( center, spread ) )
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
    
    /** Return a sprawl graph that contains a random spread of edges */
    def getSprawlGraph( int center, int spread ) {
        return getSprawlGraph( randomSize( center, spread ) )
    }
    
    /** Return a sprawling graph that contains N edges */
    def getSprawlGraph( int size=0 ) {
        size = size ?: randomSize()
        
        def sg = new SubGraph( size )
        sg.nodes.add( randomNode() )
        for (int i=1; i <= size; i++ ) {
            def left = sg.nodes.get( rand.nextInt( i ) )
            def right = randomNode()
            while ( right == left ) {
                LOGGER.info( "Skipping duplicate: {} == {}", left, right )
                right = randomNode()
            }
            sg.nodes.add( right )
            sg.edges.add( randomEdge( left, right ) )
        }
    }
    
    int randomSize() {
        return randomSize( CENTER, SPREAD )
    }

    int randomSize( int center, int spread ) {
        return Math.max(1, center + spread * rand.nextGaussian() )
    }

    def randomNode( ) {
        return new MyNode( random( NODES ), random( WORDS ), random( WORDS ), random( WORDS ), random( WORDS ) )
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
