package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.documentStore.DocumentStoreClient;
import org.yawlfoundation.yawl.documentStore.YDocument;
import org.yawlfoundation.yawl.ui.util.ApplicationProperties;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Michael Adams
 * @date 11/11/20
 */
public class DocStoreClient extends AbstractClient {

    private final DocumentStoreClient _client;


    public DocStoreClient() {
        super();

        String host = ApplicationProperties.getDocStoreHost();
        String port = ApplicationProperties.getDocStorePort();
        _client = new DocumentStoreClient(buildURI(host, port, "documentStore/"));
    }


    public YDocument getStoredDocument(long docID) throws IOException {
        return _client.getDocument(docID, getHandle());
    }


    public long putStoredDocument(YDocument doc) throws IOException {
        String id = _client.putDocument(doc, getHandle());
        if (successful(id)) {
            return Long.parseLong(StringUtil.unwrap(id));
        }
        throw new IOException(StringUtil.unwrap(id));
    }


    public void removeStoredDocument(long docID) throws IOException {
        _client.removeDocument(docID, getHandle());
    }


    private boolean successful(String xml) { return _client.successful(xml); }


    @Override
    protected void connect() throws IOException {
        if (connected()) return;

        _handle = _client.connect(getPair().left, getPair().right);
        if (! connected()) {
            _handle = _client.connect(getDefaults().left, getDefaults().right);
            if (!connected()) {
                throw new IOException("Failed to connect to YAWL Engine");
            }
        }
    }


    @Override
    protected boolean connected() throws IOException {
        return _handle != null && _client.checkConnection(_handle);
    }

    @Override
    public Map<String, String> getBuildProperties() throws IOException {
        return Collections.emptyMap();
    }


    @Override
    public void disconnect() throws IOException {
        connect();
        _client.disconnect(_handle);
    }
    
}
