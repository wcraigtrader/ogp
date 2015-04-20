package com.akonizo.orientdb

import java.net.URL

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Data {

    final static Logger LOGGER = LoggerFactory.getLogger( Data.class )

    static List<String> WORDS = null
    static NODES = ['foo', 'bar', 'baz', 'quux']
    static EDGES = [ 'sees', 'hears', 'smells', 'tastes' ]

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

    def getSubGraph( ) {
        return getSubGraph( fib( randomSize() ) )
    }

    def getSubGraph( int size ) {
        def sg = new SubGraph()
        0..size.each { sg.nodes.add( randomNode() ) }
        1..size.each { sg.edges.add( randomEdge( sg.nodes[0], sg.nodes[it] ) ) }
        return sg
    }

    String random( List<String> words ) {
        return words[ rand.nextInt( words.size ) ]
    }
    
    int randomSize() {
        return fib( rand.nextInt( 20 ) + 1 )
    }

    def randomNode( ) {
        return new Node( random( NODES ), random( WORDS), random( WORDS), random( WORDS ), random( WORDS ) )
    }
    
    def randomEdge( Node source, Node target ) {
        def type = random( EDGES )
        if ( type  == "tastes" ) {
            def now = new Date()
            return new Edge( type, source, target, now, now )
        }
        return new Edge( type, source, target )
    }
    
    /** Simple Fibonacci numbers */
    static int fib( int f ) {
        assert f <= 46
        if (f <= 1) return f
        return fib( f-1 ) + fib( f-2 )
    }
}

class SubGraph {
    List<Node> nodes
    List<Edge> edges

    SubGraph() {
        nodes = new ArrayList<Node>()
        edges = new ArrayList<Edge>()
    }
}

class Node {
    String type
    String key
    String data

    Node( String t, String k, String[] words ) {
        this.type = t
        this.key = k
        this.data = words.join( ' ' )
    }
}

class Edge {
    String type
    Node source
    Node target
    Date began
    Date ended

    Edge( String t, Node n1, Node n2, Date b=null, Date e=null ) {
        this.type = t
        this.source = n1
        this.target = n2
        this.began = b
        this.ended = e
    }
}