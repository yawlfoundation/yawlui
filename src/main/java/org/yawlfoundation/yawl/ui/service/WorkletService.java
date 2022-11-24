package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.RaiseExceptionDialog;
import org.yawlfoundation.yawl.worklet.admin.AdministrationTask;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 24/11/2022
 */
public class WorkletService {

    public static enum Target { Case, Item }

    private final WorkletClient _client = new WorkletClient();
    private static final Set<WorkletServiceListener> _listeners = new HashSet<>();

    public WorkletService() { }


    // from CaseView
    public void raiseExternalException(String id) {
        raiseExternalException(Target.Case, id, null);
    }


    // from ItemView
    public void raiseExternalException(WorkItemRecord wir) {
        raiseExternalException(Target.Item, wir.getID(), wir.getCaseID());
    }


    public static boolean addListener(WorkletServiceListener listener) {
        return _listeners.add(listener);
    }


    public boolean  removeListener(WorkletServiceListener listener) {
        return _listeners.remove(listener);
    }


    // for Case=level, id == caseID, id2 == null
    // for item-level, id == itemID, id2 == caseID
    private void raiseExternalException(Target target, String id, String id2) {
        String title = String.format("Raise External Exception for %s: %s",
                target.name(), id);
        List<String> triggers = getExternalTriggers(target, id);
        RaiseExceptionDialog dialog = new RaiseExceptionDialog(title, triggers);
        dialog.getOKButton().addClickListener(e -> {
            if (dialog.validate()) {
                if (dialog.isNewException()) {
                    String newTitle = dialog.getNewTitle();
                    String scenario = dialog.getNewScenario();
                    String process = dialog.getNewProcess();
                    String caseID = target == Target.Case ? id : id2;
                    String itemID = target == Target.Case ? null : id;
                    addNewExceptionTask(target, caseID, itemID, newTitle, scenario, process);
                }
                else {
                    String selected = dialog.getSelection();
                    raiseException(target, id, selected);
                }
                dialog.close();
                notifyListeners();
                Announcement.success("Exception raised");
            }
        });
        dialog.open();
    }


    private void raiseException(Target target, String id, String trigger) {
        try {
            if (target == Target.Case) {
                _client.raiseCaseExternalException(id, trigger);
            }
            else {
                _client.raiseItemExternalException(id, trigger);
            }
        }
        catch (IOException e) {
            Announcement.error("Failed to raise Exception: " + e.getMessage());
        }
    }


    private void addNewExceptionTask(Target target, String caseID, String itemID,
                                     String title, String scenario, String process) {
        AdministrationTask task = target == Target.Case ?
                new AdministrationTask(caseID, title, scenario, process,
                        AdministrationTask.TASKTYPE_CASE_EXTERNAL_EXCEPTION) :
                new AdministrationTask(caseID, itemID, title, scenario, process,
                        AdministrationTask.TASKTYPE_ITEM_EXTERNAL_EXCEPTION);

        try {
            _client.addWorkletAdministrationTask(task);
        }
        catch (IOException e) {
            Announcement.error("Failed to raise Exception: " + e.getMessage());
        }
    }


    private List<String> getExternalTriggers(Target target, String id) {
        try {
            return target == Target.Case ?
                    _client.getExternalTriggersForCase(id) :
                    _client.getExternalTriggersForItem(id);
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }


    private void notifyListeners() {
        _listeners.forEach(WorkletServiceListener::actionCompleted);
    }

}
