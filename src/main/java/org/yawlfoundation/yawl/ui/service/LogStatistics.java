package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

/**
 * @author Michael Adams
 * @date 24/10/2025
 */
public class LogStatistics {

    private String _specID;
    private long _from = -1;
    private long _to = -1;
    private long _rowKey;
    private int _started;
    private int _completed;
    private int _cancelled;
    private String _maxCompletion;
    private String _minCompletion;
    private String _avgCompletion;
    private String _maxCancelled;
    private String _minCancelled;
    private String _avgCancelled;

    public LogStatistics() { }

    public LogStatistics(String xml) {
        this();
        fromXNode(new XNodeParser().parse(xml));
    }


    public void fromXNode(XNode root) {
        _specID = root.getAttributeValue("id");
        _rowKey = Long.parseLong(root.getAttributeValue("key"));
        _started = Integer.parseInt(root.getChildText("started"));
        _completed = Integer.parseInt(root.getChildText("completed"));
        _cancelled = Integer.parseInt(root.getChildText("cancelled"));
        _maxCompletion = root.getChildText("completionMaxtime");
        _minCompletion = root.getChildText("completionMintime");
        _avgCompletion = root.getChildText("completionAvgtime");
        _maxCancelled = root.getChildText("cancelledMaxtime");
        _minCancelled = root.getChildText("cancelledMintime");
        _avgCancelled = root.getChildText("cancelledAvgtime");
    }


    public String getSpecID() {
        return _specID;
    }

    public long getRowKey() {
        return _rowKey;
    }

    public int getStarted() {
        return _started;
    }

    public int getCompleted() {
        return _completed;
    }

    public int getCancelled() {
        return _cancelled;
    }

    public String getMaxCompletion() {
        return _maxCompletion;
    }

    public String getMinCompletion() {
        return _minCompletion;
    }

    public String getAvgCompletion() {
        return _avgCompletion;
    }

    public String getMaxCancelled() {
        return _maxCancelled;
    }

    public String getMinCancelled() {
        return _minCancelled;
    }

    public String getAvgCancelled() {
        return _avgCancelled;
    }

    public long getFrom() {
        return _from;
    }

    public void setFrom(long from) {
        _from = from;
    }

    public long getTo() {
        return _to;
    }

    public void setTo(long to) {
        _to = to;
    }
}
