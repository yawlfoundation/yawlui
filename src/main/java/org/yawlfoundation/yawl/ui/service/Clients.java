package org.yawlfoundation.yawl.ui.service;

/**
 * @author Michael Adams
 * @date 7/12/2022
 */
public class Clients {

    private static final ResourceClient _resClient = new ResourceClient();
    private static final EngineClient _engClient = new EngineClient();
    private static final WorkletClient _wsClient = new WorkletClient();
    private static final DocStoreClient _docClient = new DocStoreClient();
    private static final LogClient _logClient = new LogClient();


    public static ResourceClient getResourceClient() { return _resClient; }

    public static EngineClient getEngineClient() { return _engClient; }

    public static WorkletClient getWorkletClient() { return _wsClient; }

    public static DocStoreClient getDocStoreClient() { return _docClient; }

    public static LogClient getLogClient() { return _logClient; }

}
