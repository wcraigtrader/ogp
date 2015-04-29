package com.akonizo.orientdb

import groovy.util.logging.Slf4j

import org.apache.commons.math3.random.*

@Slf4j
class Data {

    static final String WORDLIST = "/words.txt"

    /** Center of Gaussian distribution */
    static final int CENTER = 1000

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
        log.debug( "Constructed with ${r}" )
        this.rand = r

        if (WORDS == null) {
            loadWords()
        }
    }

    def loadWords() {
        log.debug( "Loading dictionary" )
        WORDS = new ArrayList<String>( 100000 )
        this.getClass().getResourceAsStream( WORDLIST ).eachLine { WORDS.add( it ) }
        log.info( "Loaded ${WORDS.size} words" )
    }

    /** Return an iterator for a graph model */
    Iterator<SubGraph> getGraphs( String model, int count, int center=CENTER, int spread=SPREAD ) {
        switch ( model ) {
            case 'radial':
                return radialGraphIterator( count, center, spread )
            case 'scatter':
                return scatterGraphIterator( count, center, spread )
            case 'sprawl':
                return sprawlGraphIterator( count, center, spread )
            case 'mixed' :
                return mixedGraphIterator( count )
            case 'light':
                return lightEdgeGraphIterator( count )
            case 'heavy':
                return heavyEdgeGraphIterator( count )
            case 'special':
                return specialGraphIterator( count )
            default:
                throw new Exception( "Unrecognized graph model ($model)" )
        }
    }

    /** Return an iterator for a radial graph model */
    Iterator<SubGraph> radialGraphIterator( int count, int center=CENTER, int spread=SPREAD ) {
        return new SubGraphIterator( count, center, spread ) {
                    SubGraph nextGraph( int position ) {
                        return getRadialGraph( center, spread )
                    }
                }
    }

    /** Return an iterator for a scattered graph model */
    Iterator<SubGraph> scatterGraphIterator( int count, int center=CENTER, int spread=SPREAD ) {
        return new SubGraphIterator( count, center, spread ) {
                    SubGraph nextGraph( int position ) {
                        return getScatterGraph( center, spread )
                    }
                }
    }

    /** Return an iterator for a sprawled graph model */
    Iterator<SubGraph> sprawlGraphIterator( int count, int center=CENTER, int spread=SPREAD ) {
        return new SubGraphIterator( count, center, spread ) {
                    SubGraph nextGraph( int position ) {
                        return getSprawlGraph( center, spread )
                    }
                }
    }

    /** Return an iterator for a mixed graph model */
    Iterator<SubGraph> mixedGraphIterator( int count ) {
        return new SubGraphIterator( count ) {
                    SubGraph nextGraph( int position ) {
                        if ( position == 1 ) return getRadialGraph( 20000, 50 )
                        return getScatterGraph( 500+50*((int) (position-1)/50 ), 5 )
                    }
                }
    }

    /** Return an iterator for a lightweight edge graph model */
    Iterator<SubGraph> lightEdgeGraphIterator( int count ) {
        return new SubGraphIterator( count ) {
                    SubGraph nextGraph( int position ) {
                        return getRadialGraph( 500+100*((int) (position-1)/100 ), 'sees' )
                    }
                }
    }

    /** Return an iterator for a heavyweight edge graph model */
    Iterator<SubGraph> heavyEdgeGraphIterator( int count ) {
        return new SubGraphIterator( count ) {
                    SubGraph nextGraph( int position ) {
                        return getRadialGraph( 500+100*((int) (position-1)/100 ), 'tastes' )
                    }
                }
    }

    /** Return an iterator for a heavyweight edge graph model */
    Iterator<SubGraph> specialGraphIterator( int count ) {
        return new SubGraphIterator( count ) {
                    SubGraph nextGraph( int position ) {
                        return getRadialGraph( 500, 'tastes' )
                    }
                }
    }

    /** Return a radial graph that contains a random spread of edges */
    SubGraph getRadialGraph( int center, int spread, String et ) {
        return getRadialGraph( randomSize( center, spread ), et )
    }

    /** Return a radial graph that contains N edges */
    SubGraph getRadialGraph( int size=0, String et=null ) {
        size = size ?: randomSize()
        def etype = et ?: random( EDGES )
        if (etype == 'tastes') {
            log.debug( "Creating edges with timestamps" )
        }

        def sg = new SubGraph( size )
        def center = randomNode()
        sg.add( center )
        for (int i=0; i < size; i++ ) {
            def petal = randomNode()
            while (petal == center) {
                log.info( "Skipping duplicate: ${center} == ${petal}" )
                petal = randomNode()
            }
            sg.add( petal )
            sg.add( randomEdge( center, petal, etype ) )
        }
        return sg
    }

    /** Return a scatter graph that contains a random spread of edges */
    SubGraph getScatterGraph( int center, int spread ) {
        return getScatterGraph( randomSize( center, spread ) )
    }

    /** Return a scatter graph that contains N edges */
    SubGraph getScatterGraph( int size=0 ) {
        size = size ?: randomSize()

        def sg = new SubGraph( size )
        while (size > 0) {
            def center = randomNode()
            sg.add(  center )
            def edges = Math.min(  size, rand.nextInt( 5 )+1 )
            for (int i=0; i<edges;) {
                def petal = randomNode()
                if (petal == center) {
                    log.info( "Skipping duplicate: ${center} == ${petal}" )
                    continue
                }
                sg.add( petal )
                sg.add( randomEdge( center, petal ) )
                i++
            }
            size -= edges
        }
        return sg
    }

    SubGraph getOldScatterGraph( int size=0 ) {
        size = size ?: randomSize()

        def sg = new SubGraph( size )
        for (int i=0; i < size; i++ ) {
            def left = randomNode()
            def right = randomNode()
            while (left == right) {
                log.info( "Skipping duplicate: ${left} == ${right}" )
                right = randomNode()
            }
            if (!(left in sg.nodes )) {
                sg.add( left )
            }
            if (!(right in sg.nodes )) {
                sg.add( right )
            }
            sg.add( randomEdge( left, right ) )
        }
        return sg
    }

    /** Return a sprawl graph that contains a random spread of edges */
    SubGraph getSprawlGraph( int center, int spread ) {
        return getSprawlGraph( randomSize( center, spread ) )
    }

    /** Return a sprawling graph that contains N edges */
    SubGraph getSprawlGraph( int size=0 ) {
        size = size ?: randomSize()

        def sg = new SubGraph( size )
        sg.add( randomNode() )
        for (int i=1; i <= size; i++ ) {
            def left = sg.nodes[ rand.nextInt( i ) ]
            def right = randomNode()
            while ( left == right ) {
                log.info( "Skipping duplicate: ${left} == ${right}" )
                right = randomNode()
            }
            sg.add( right )
            sg.add( randomEdge( left, right ) )
        }
        return sg
    }

    int randomSize() {
        return randomSize( CENTER, SPREAD )
    }

    int randomSize( int center, int spread ) {
        return Math.max(1, center + spread * rand.nextGaussian() )
    }

    def randomNode( String t=null ) {
        String type = t ?: random( NODES )
        def node = new MyNode( type, randomKey(), randomData() )
        if (type == "foo" ) {
            node.data1 = randomData( 10 )
            node.data2 = randomData( 20 )
            node.data3 = randomData( 30 )
            node.data4 = randomData( 40 )
            node.data5 = randomData( 50 )
            node.data6 = randomData( 60 )
            node.data7 = randomData( 70 )
            node.data8 = randomData( 80 )
        }
        return node
    }

    def randomEdge( MyNode source, MyNode target, String t=null ) {
        assert source != target
        def type = t ?: random( EDGES )
        if ( type  == "tastes" ) {
            def now = new Date()
            return new MyEdge( type, source, target, now, now )
        }
        return new MyEdge( type, source, target )
    }

    String randomKey() {
        return random( WORDS ) + '-' + random( WORDS )
    }

    String randomData( int size=8 ) {
        return ( [random( WORDS)]* size ).join( ' ' )
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

class SubGraphIterator implements Iterator<SubGraph> {

    int supplied
    int count
    int center
    int spread

    SubGraphIterator( int size, int c=Data.CENTER, int s=Data.SPREAD ) {
        supplied = 0
        count = size
        center = c
        spread = s
    }

    boolean hasNext() {
        return supplied < count
    }

    SubGraph next() {
        if (!hasNext() ) throw new NoSuchElementException()
        return nextGraph( ++supplied )
    }

    SubGraph nextGraph( int position ) {
        throw new UnsupportedOperationException()
    }

    void remove() {
        throw new UnsupportedOperationException()
    }
}
