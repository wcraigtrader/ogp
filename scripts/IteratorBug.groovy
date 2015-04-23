#! /usr/bin/env groovy

class FooIterator implements Iterator<Integer> {
    
    int supplied
    int count
    
    FooIterator( int size ) {
        supplied = 0
        count = size
    }
    
    boolean hasNext() { return supplied < count }
    
    Integer next() {
        if (!hasNext() ) throw new NoSuchElementException()
        return nextFoo( supplied++ )
    }
    
    Integer nextFoo( int position ) { throw new UnsupportedOperationException() }
    
    void remove() { throw new UnsupportedOperationException() }
}

def getMyFooIterator( int size ) {
	return new FooIterator( size ) {
		Integer nextFoo( int pos ) {
			return Integer.valueOf( pos )
		}
	}
}

def foos = getMyFooIterator( 5 )
foos.each { println it }