package com.akonizo.orientdb

class SubGraph {
    ArrayList<MyNode> nodes
    ArrayList<MyEdge> edges

    SubGraph( int size ) {
        nodes = new ArrayList<MyNode>( size+100 )
        edges = new ArrayList<MyEdge>( size+100 )
    }
}