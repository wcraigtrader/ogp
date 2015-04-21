package com.akonizo.orientdb

class MyEdge {
    String type
    MyNode source
    MyNode target
    Date began
    Date ended

    MyEdge( String t, MyNode n1, MyNode n2, Date b=null, Date e=null ) {
        this.type = t
        this.source = n1
        this.target = n2
        this.began = b
        this.ended = e
    }

    String toString() {
        "$source --($type)--> $target"
    }
}