package org.jenkinsci.plugins.android.connector.cli;

import hudson.Extension;
import hudson.cli.CLICommand;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.android.connector.AndroidDevice;
import org.jenkinsci.plugins.android.connector.AndroidDeviceList;

import javax.inject.Inject;

@Extension
public class ListCommand extends CLICommand {
    @Inject
    AndroidDeviceList devices;

    @Override
    public String getName() {
        return "android-list-devices";
    }

    @Override
    public String getShortDescription() {
        return "List Android devices attached to this Jenkins cluster";
    }

    @Override
    protected int run() throws Exception {
        Jenkins.getInstance().getInjector().injectMembers(this);

        for (AndroidDevice dev : devices.getDevices().values())
            stdout.printf("%s\t%s\n", dev.getUniqueDeviceId(), dev.getDeviceName());
        return 0;
    }
}
