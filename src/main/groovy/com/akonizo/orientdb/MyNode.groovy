package com.akonizo.orientdb

class MyNode {

    String type
    String key
    String data

    MyNode( String t, String k, String[] words ) {
        this.type = t
        this.key = k
        this.data = words.join( ' ' )
    }

    Map<String,Object> getProps() {
        def map = [:]
        map['key'] = key
        map['data'] = data
        return map
    }

    int hashCode() {
        return 13 * type.hashCode() + key.hashCode()
    }
    
    boolean equals( Object other ) {
        if (other instanceof MyNode) {
            MyNode that = (MyNode) other
            return type == that.type && key == that.key
        }
        return false
    }
    
    String toString() {
        "MyNode($type, $key)"
    }
}

