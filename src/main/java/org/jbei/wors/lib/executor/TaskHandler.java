package org.jbei.wors.lib.executor;

import org.jbei.wors.lib.common.logging.Logger;

/**
 * Runnable for running tasks
 *
 * @author Hector Plahar
 */
class TaskHandler implements Runnable {

    private final Task task;

    public TaskHandler(Task task) {
        this.task = task;
    }

    @Override
    public void run() {
        try {
            this.task.setStatus(TaskStatus.IN_PROGRESS);
            task.execute();
            this.task.setStatus(TaskStatus.COMPLETED);
        } catch (Throwable caught) {
            Logger.error(caught);
            this.task.setStatus(TaskStatus.EXCEPTION);
        }
    }
}
