/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.CouchDB;

import io.sbk.api.Parameters;
import io.sbk.api.Reader;
import io.sbk.api.impl.SbkLogger;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;

import java.io.EOFException;
import java.io.IOException;
import java.util.Map;


/**
 * Class for Reader.
 */
public class CouchDBReader implements Reader<byte[]> {
    final private CouchDbConnector db;
    final private Parameters params;
    private long key;
    private int cnt;

    public CouchDBReader(int id, Parameters params, CouchDBConfig config, CouchDbConnector db) throws IOException {
        this.key = CouchDB.generateStartKey(id);
        this.cnt = 0;
        this.params = params;
        this.db = db;
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        String k = Long.toString(key);
        try {
            Map<String, Object> map = db.get(Map.class, k);
            if (map != null) {
                key++;
                String data = (String) map.get("data");
                return data.getBytes();
            }
        } catch (DocumentNotFoundException ex) {
            throw new EOFException("Key : "+ k + "not found");
        }
        return null;
    }

    @Override
    public void close() throws  IOException {
    }
}