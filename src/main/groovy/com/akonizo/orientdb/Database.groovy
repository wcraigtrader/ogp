package com.akonizo.orientdb

import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.Parameter
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx
import com.tinkerpop.blueprints.impls.orient.OrientVertexType

public class Database {

    /** Database Time Zone */
    public static final String TIMEZONE = "UTC"

    /** Datebase Time Format */
    public static final String DATETIMEFORMAT= "yyyy-MM-dd'T'HH:mm:ssXXX"

    /** Delete an existing database */
    static public void delete_database(String dbpath) {
        if (!(dbpath.startsWith('plocal:'))) {
            return
        }

        OrientGraphFactory factory = null

        /* Gracefully wipe an existing database, if any */
        try {
            factory = new OrientGraphFactory(dbpath)
            if (factory.exists()) {
                factory.drop()
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            if (factory != null) {
                factory.close()
            }
        }
    }

    /** Create an empty database */
    static public void create_database(String dbpath) {
        OrientGraphFactory factory = new OrientGraphFactory(dbpath, 'admin', 'admin' )
        OrientGraphNoTx g = null

        try {
            g = factory.getNoTx()
            OCommandSQL cmd = new OCommandSQL()

            cmd.setText("alter database TIMEZONE " + TIMEZONE)
            g.command(cmd).execute()

            cmd.setText("alter database DATETIMEFORMAT " + DATETIMEFORMAT)
            g.command(cmd).execute()
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            g?.shutdown()
            factory?.close()
        }
    }

    /** Populate the schema for an empty database */
    static public void create_schema(String dbpath) {

        OrientGraphFactory factory = new OrientGraphFactory(dbpath, 'admin', 'admin')
        OrientGraphNoTx g = null

        try {
            OrientVertexType v
            OrientEdgeType e

            g = factory.getNoTx()

            v = g.createVertexType("node", "V")
            v.createProperty("key", OType.STRING)
            v.createProperty("created", OType.DATETIME)
            v.createProperty("updated", OType.DATETIME)
            v.createProperty("data", OType.STRING )

            v = g.createVertexType( "foo", "node" )
            v = g.createVertexType( "bar", "node" )
            v = g.createVertexType( "baz", "node" )
            v = g.createVertexType( "quux", "node" )


            e = g.createEdgeType("edge", "E")
            e.createProperty("began", OType.DATETIME)
            e.createProperty("ended", OType.DATETIME)

            e = g.createEdgeType("sees", "edge" )
            e = g.createEdgeType("hears", "edge" )
            e = g.createEdgeType("feels", "edge" )
            e = g.createEdgeType("smells", "edge" )
            e = g.createEdgeType("tastes", "edge" )
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            g?.shutdown()
            factory?.close()
        }
    }

    /** Populate the indexes for a database */
    static public void create_indexes(String dbpath) {
        create_indexes(dbpath, true)
    }

    /** Populate the indexes for a database */
    static public void create_indexes(String dbpath, boolean hashed) {

        final Parameter<?, ?> UNIQUE_INDEX = new Parameter<String, String>("type", "UNIQUE_HASH_INDEX") // was UNIQUE

        OrientGraphFactory factory = new OrientGraphFactory(dbpath, 'admin', 'admin' )
        OrientGraphNoTx g = null

        try {
            g = factory.getNoTx()

            g.createKeyIndex("key", Vertex.class, new Parameter<String, String>("class", "foo"), UNIQUE_INDEX)
            g.createKeyIndex("key", Vertex.class, new Parameter<String, String>("class", "bar"), UNIQUE_INDEX)
            g.createKeyIndex("key", Vertex.class, new Parameter<String, String>("class", "baz"), UNIQUE_INDEX)
            g.createKeyIndex("key", Vertex.class, new Parameter<String, String>("class", "quux"), UNIQUE_INDEX)

        } catch (Exception e) {
            e.printStackTrace()

        } finally {
            g?.shutdown()
            factory?.close()
        }
    }
}
