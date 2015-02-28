package org.jenkinsci.plugins.android.connector;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class PeriodicLister extends AsyncPeriodicWork {
    @Inject
    private AndroidDeviceList deviceList;

    public PeriodicLister() {
        super("Android Device List Updater");

        logger.setLevel(Level.WARNING);
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        deviceList.updateAll(listener);
    }

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.MINUTES.toMillis(1);
    }
}
