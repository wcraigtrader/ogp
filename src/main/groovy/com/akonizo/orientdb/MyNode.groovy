package com.akonizo.orientdb

class MyNode {

    String type
    String key
    String data
    String data1
    String data2
    String data3
    String data4
    String data5
    String data6
    String data7
    String data8
    
    MyNode( String t, String k, String[] words ) {
        this.type = t
        this.key = k
        this.data = words.join( ' ' )
    }

    Map<String,Object> getProps() {
        def map = [:]
        map['key'] = key
        map['data'] = data
        if ( data1 ) map['data1'] = data1
        if ( data2 ) map['data2'] = data2
        if ( data3 ) map['data3'] = data3
        if ( data4 ) map['data4'] = data4
        if ( data5 ) map['data5'] = data5
        if ( data6 ) map['data6'] = data6
        if ( data7 ) map['data7'] = data7
        if ( data8 ) map['data8'] = data8
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

