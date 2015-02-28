package org.jenkinsci.plugins.android.connector;

import hudson.FilePath;
import hudson.Launcher.LocalLauncher;
import hudson.Launcher.ProcStarter;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Deploys *.ipa to the device.
 *
 * @author Kohsuke Kawaguchi
 */
class DeployTask implements Callable<Void, IOException> {

    private final FilePath bundle;
    private final TaskListener listener;
    private final String deviceId;
    //private final FilePath rootPath;

    DeployTask(AndroidDevice device, File bundle, TaskListener listener) {
        this.bundle = new FilePath(bundle);
        this.listener = listener;
        this.deviceId = device.getUniqueDeviceId();
        //this.rootPath = device.getComputer().getNode().getRootPath();
    }

    public Void call() throws IOException {
        File t = Util.createTempDir();
        try {
            //adb -s <device_id> install -r /path/to/file.apk
            ArgumentListBuilder arguments = new ArgumentListBuilder("adb");
            arguments.add("-s", deviceId);
            arguments.add("install", "-r", bundle.getName());

            ProcStarter proc = new LocalLauncher(listener).launch()
                    .cmds(arguments)
                    .stdout(listener)
                    .pwd(bundle.getParent());
            int exit = proc.join();
            if (exit != 0)
                throw new IOException("Deployment of " + bundle + " failed: " + exit);

            return null;
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException().initCause(e);
        } finally {
            Util.deleteRecursive(t);
            listener.getLogger().flush();
        }
    }

}
