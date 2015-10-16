package com.akonizo.orientdb

import groovy.util.logging.Slf4j

import com.orientechnologies.orient.core.command.OCommandOutputListener
import com.orientechnologies.orient.core.db.tool.ODatabaseExport
import com.orientechnologies.orient.core.index.OPropertyIndexDefinition
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.Parameter
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx
import com.tinkerpop.blueprints.impls.orient.OrientVertexType

@Slf4j
public class Database {

    /** Database Time Zone */
    public static final String TIMEZONE = "UTC"

    /** Datebase Time Format */
    public static final String DATETIMEFORMAT= "yyyy-MM-dd'T'HH:mm:ssXXX"

    /** Edge Indexing */
    private static final INDEX_EDGES = true

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
            log.error("Dropping database ${dbpath}", e )
        } finally {
            factory?.close()
        }
    }

    /** Create an empty database */
    static public void create_database(String dbpath, boolean indexEdges=false ) {
        OrientGraphFactory factory = new OrientGraphFactory(dbpath, 'admin', 'admin' )
        OrientGraphNoTx g = null

        try {
            g = factory.getNoTx()
            OCommandSQL cmd = new OCommandSQL()

            cmd.setText("alter database TIMEZONE ${TIMEZONE}" )
            g.command(cmd).execute()

            cmd.setText("alter database DATETIMEFORMAT ${DATETIMEFORMAT}" )
            g.command(cmd).execute()

            if (indexEdges) {
                cmd.setText("alter database custom useLightweightEdges=false" )
                g.command(cmd).execute()

                cmd.setText("alter database custom useVertexFieldsForEdgeLabels=true" )
                g.command(cmd).execute()
            }

        } catch (Exception e) {
            log.error("Creating database ${dbpath}", e )
        } finally {
            g?.shutdown()
            factory?.close()
        }
    }

    /** Populate the schema for an empty database */
    static public void create_schema(String dbpath, boolean indexEdges=false) {

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
            v.createProperty("data1", OType.STRING )
            v.createProperty("data2", OType.STRING )
            v.createProperty("data3", OType.STRING )
            v.createProperty("data4", OType.STRING )
            v.createProperty("data5", OType.STRING )
            v.createProperty("data6", OType.STRING )
            v.createProperty("data7", OType.STRING )
            v.createProperty("data8", OType.STRING )

            v = g.createVertexType( "bar", "node" )
            v = g.createVertexType( "baz", "node" )
            v = g.createVertexType( "quux", "node" )

            if (indexEdges) {
                e = g.getEdgeType("E" )
                e.createProperty( "in", OType.LINK )
                e.createProperty( "out", OType.LINK )
            }

            e = g.createEdgeType("edge", "E")
            e.createProperty("began", OType.DATETIME)
            e.createProperty("ended", OType.DATETIME)

            e = g.createEdgeType("sees", "edge" )
            e = g.createEdgeType("hears", "edge" )
            e = g.createEdgeType("feels", "edge" )
            e = g.createEdgeType("smells", "edge" )
            e = g.createEdgeType("tastes", "edge" )

        } catch (Exception e) {
            log.error("Creating schema", e )
        } finally {
            g?.shutdown()
            factory?.close()
        }
    }

    /** Populate the indexes for a database */
    static public void create_indexes(String dbpath, boolean indexEdges=false) {

        final Parameter<?,?> UNIQUE_INDEX = new Parameter<String, String>('type', 'UNIQUE_HASH_INDEX')
        final Parameter<?,?> COLLATE_CI = new Parameter<String, String>('collate', 'ci')
        Parameter<?,?> classname

        OrientGraphFactory factory = new OrientGraphFactory(dbpath, 'admin', 'admin' )
        OrientGraphNoTx g = null

        OCommandSQL cmd = new OCommandSQL()

        try {
            g = factory.getNoTx()

            classname = new Parameter<String, String>("class", "foo")
            g.createKeyIndex("key", Vertex.class, classname, UNIQUE_INDEX)
            classname = new Parameter<String, String>("class", "bar")
            g.createKeyIndex("key", Vertex.class, classname, UNIQUE_INDEX)

            create_unique_ci_index( g, 'baz', 'key' )
            create_unique_ci_index( g, 'quux', 'key' )

            if (indexEdges) {
                cmd.setText("create index sees.unique on sees (out,in) unique" )
                g.command(cmd).execute()

                cmd.setText("create index hears.unique on hears (out,in) unique" )
                g.command(cmd).execute()

                cmd.setText("create index feels.unique on feels (out,in) unique" )
                g.command(cmd).execute()

                cmd.setText("create index smells.unique on smells (out,in) unique" )
                g.command(cmd).execute()

                cmd.setText("create index tastes.unique on tastes (out,in) unique" )
                g.command(cmd).execute()
            }

        } catch (Exception e) {
            log.error("Creating indexes", e )

        } finally {
            g?.shutdown()
            factory?.close()
        }
    }

    static public void create_unique_ci_index( OrientBaseGraph g, String classname, String key ) {
        def index = new OPropertyIndexDefinition( classname, key, OType.STRING )
        index.setCollate( 'ci' )

        def db = g.rawGraph
        def im = db.metadata.indexManager
        def schema = db.metadata.schema
        def cls = schema.getOrCreateClass(classname, schema.getClass('V') )
        im.createIndex("${classname}.${key}", 'UNIQUE_HASH_INDEX', index, cls.getPolymorphicClusterIds(), null, new ODocument() )
    }

    static public void export_database( String dbpath, String output ) {
        OrientGraphFactory factory = new OrientGraphFactory(dbpath, 'admin', 'admin' )
        OrientGraphNoTx g = null

        OCommandSQL cmd = new OCommandSQL()

        try {
            OCommandOutputListener listener = new OCommandOutputListener() {
                @Override
                public void onMessage(String iText) {
                }
            }

            g = factory.getNoTx()

            ODatabaseExport export = new ODatabaseExport(g.rawGraph, output, listener)
            export.includeRecords = false
            export.includeClusterDefinitions = false
            export.compressionLevel = 0
            export.exportDatabase()
            export.close()

        } catch (Exception e) {
            log.error("Exporting database", e )
        } finally {
            g?.shutdown()
            factory?.close()
        }
    }
}
