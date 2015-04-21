package com.akonizo.orientdb

class SubGraph {
    Collection<MyNode> nodes
    Collection<MyEdge> edges

    SubGraph( int size ) {
        nodes = new ArrayList<MyNode>( size+100 )
        edges = new ArrayList<MyEdge>( size+100 )
    }
}