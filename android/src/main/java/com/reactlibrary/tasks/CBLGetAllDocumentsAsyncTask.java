package com.reactlibrary.tasks;

import android.os.AsyncTask;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableArray;
import com.reactlibrary.ConversionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CBLGetAllDocumentsAsyncTask extends AsyncTask<Void, Void, WritableArray> {

    private Query mQuery;
    private Promise mPromise;

    public CBLGetAllDocumentsAsyncTask(Promise promise, Query query){
        mQuery=query;
        mPromise=promise;
    }

    @Override
    protected WritableArray doInBackground(Void... voids) {
        try {
            QueryEnumerator result = mQuery.run();
            WritableArray documents = ConversionUtil.toWritableArray( this.getQueryResults(result).toArray() );
            return documents;
        } catch (CouchbaseLiteException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(WritableArray documents) {
        super.onPostExecute(documents);
        if(documents != null) {
            mPromise.resolve(documents);
        } else {
            mPromise.reject("query", "Error running query");
        }
    }

    private ArrayList getQueryResults(QueryEnumerator result) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            Document doc = row.getDocument();
            if (doc != null) {
                HashMap<String, Object> props = this.serializeDocument(doc);
                if (props != null) {
                    list.add( props );
                }
            } else {
                HashMap<String, Object> item = new HashMap();
                item.put("key", row.getKey());
                item.put("value", row.getValue());
                list.add(item);
            }
        }
        return list;
    }

    private HashMap<String, Object> serializeDocument(Document document) {
        HashMap<String, Object> properties = new HashMap<>(document.getProperties());
        Map<String, Object> attachments = (Map<String, Object>)properties.get("_attachments");
        if (attachments != null) {
            HashMap<String, Object> mappedAttachments = new HashMap<>();
            for (Map.Entry<String, Object> entry : attachments.entrySet())
            {
                Map<String, Object> attData = new HashMap<>((Map<String, Object>)entry.getValue());
                String attName = entry.getKey();
                attData.put("url", document.getCurrentRevision().getAttachment(attName).getContentURL().toString());
                mappedAttachments.put(attName, attData);
            }
            properties.put("_attachments", mappedAttachments);
        }
        return properties;
    }
}
