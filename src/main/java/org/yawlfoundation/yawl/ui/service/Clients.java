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


    public static ResourceClient getResourceClient() { return _resClient; }

    public static EngineClient getEngineClient() { return _engClient; }

    public static WorkletClient getWorkletClient() { return _wsClient; }

    public static DocStoreClient getDocStoreClient() { return _docClient; }

}
