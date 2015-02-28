package org.jenkinsci.plugins.android.connector;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import hudson.Extension;
import hudson.Launcher.LocalLauncher;
import hudson.model.Computer;
import hudson.model.ModelObject;
import hudson.model.RootAction;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.util.ArgumentListBuilder;
import hudson.util.IOException2;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Maintains the list of Android Devices connected to all the slaves.
 *
 * @author Kohsuke Kawaguchi
 * @author Nicolae Ghimbovschi (Android adaptation)
 */
@Extension
public class AndroidDeviceList implements RootAction, ModelObject {
    private volatile Multimap<Computer,AndroidDevice> devices = LinkedHashMultimap.create();

    /**
     * List of all the devices.
     */
    public Multimap<Computer,AndroidDevice> getDevices() {
        return Multimaps.unmodifiableMultimap(devices);
    }

    /**
     * Refresh all slaves in concurrently.
     */
    public void updateAll(TaskListener listener) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

        Map<Future<List<AndroidDevice>>,Computer> futures = newHashMap();

        for (Computer c : Jenkins.getInstance().getComputers()) {
            try {
                futures.put(c.getChannel().callAsync(new FetchTask(listener)), c);
            } catch (Exception e) {
                e.printStackTrace(listener.error("Failed to list up Android devices on"+c.getName()));
            }
        }

        Multimap<Computer,AndroidDevice> devices = LinkedHashMultimap.create();
        for (Entry<Future<List<AndroidDevice>>, Computer> e : futures.entrySet()) {
            Computer c = e.getValue();
            try {
                List<AndroidDevice> devs = e.getKey().get();
                for (AndroidDevice d : devs)
                    d.computer = c;
                devices.putAll(c, devs);
            } catch (Exception x) {
                x.printStackTrace(listener.error("Failed to list up Android devices on "+c.getName()));
            }
        }

        this.devices = devices;
    }

    public void update(Computer c, TaskListener listener) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

        List<AndroidDevice> r = Collections.emptyList();
        if (c.isOnline()) {// ignore disabled slaves
            try {
                r = c.getChannel().call(new FetchTask(listener));
                for (AndroidDevice dev : r) dev.computer = c;
            } catch (Exception e) {
                e.printStackTrace(listener.error("Failed to list up Android devices"));
            }
        }

        synchronized (this) {
            Multimap<Computer,AndroidDevice> clone = LinkedHashMultimap.create(devices);
            clone.removeAll(c);
            clone.putAll(c, r);
            devices = clone;
        }
    }

    public synchronized void remove(Computer c) {
        Multimap<Computer,AndroidDevice> clone = LinkedHashMultimap.create(devices);
        clone.removeAll(c);
        devices = clone;
    }

    public String getIconFileName() {
        return "/plugin/android-device-connector/icons/24x24/android.png";
    }

    public String getDisplayName() {
        return "Connected Android Devices";
    }

    public String getUrlName() {
        if (Jenkins.getInstance().hasPermission(READ))
            return "android-devices";
        else
            return null;
    }

    @RequirePOST
    public HttpResponse doRefresh() {
        updateAll(StreamTaskListener.NULL);
        return HttpResponses.redirectToDot();
    }

    /**
     * Maps {@link AndroidDevice} to URL space.
     */
    public AndroidDevice getDynamic(String token) {
        return getDevice(token);
    }

    public AndroidDevice getDevice(String udid) {
        for (AndroidDevice dev : devices.values())
            if (udid.equalsIgnoreCase(dev.getUniqueDeviceId()) || udid.equals(dev.getDeviceName()))
                return dev;
        return null;
    }

    /**
     * Retrieves {@link AndroidDevice}s connected to a machine.
     */
    private static class FetchTask implements Callable<List<AndroidDevice>,IOException> {
        private final TaskListener listener;

        private final String mAdbDevicesRegex = "(.*)\\t+(.*)";
        private final String mAdbDeviceShellGetPropRegex = "\\[(.*)\\]: \\[(.*)\\]";

        private FetchTask(TaskListener listener) {
            this.listener = listener;
        }

        public List<AndroidDevice> call() throws IOException {

            List<AndroidDevice> androidDevicesList = newArrayList();

            //adb devices
            ArgumentListBuilder adbDevicesCommand = new ArgumentListBuilder("adb");
            adbDevicesCommand.add("devices");

            String connectedDevices = executeCommand(listener.getLogger(), adbDevicesCommand);
            Properties androidDevices = extractOutputProperties(listener.getLogger(), connectedDevices, mAdbDevicesRegex);

            Set keys = androidDevices.keySet();
            for(Object deviceId:keys){
                //adb -s <device_id> shell getprop
                ArgumentListBuilder adbDevicePropsCommand = new ArgumentListBuilder("adb");
                adbDevicePropsCommand.add("-s", (String)deviceId);
                adbDevicePropsCommand.add("shell", "getprop");

                String deviceProperties = executeCommand(listener.getLogger(), adbDevicePropsCommand);
                Properties androidDeviceProperties = extractOutputProperties(listener.getLogger(), deviceProperties, mAdbDeviceShellGetPropRegex);

                androidDeviceProperties.put("UniqueDeviceID", deviceId);
                androidDevicesList.add(new AndroidDevice(androidDeviceProperties));
            }

            return androidDevicesList;
        }

        public String executeCommand(PrintStream logger, ArgumentListBuilder arguments) throws IOException {

            try {
                logger.println("Executing " + arguments);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int exit = new LocalLauncher(listener).launch()
                        .cmds(arguments).stdout(out).stderr(logger).join();

                if (exit != 0) {
                    logger.println(arguments + " failed to execute:" + exit);
                    logger.write(out.toByteArray());
                    logger.println();
                    return "";
                }

                return new String(out.toByteArray(), "ISO-8859-1");
            } catch (InterruptedException e) {
                throw new IOException2("Interrupted while listing up devices",e);
            }
        }

        private Properties extractOutputProperties(PrintStream logger, String output, String regex) throws  IOException {
            Properties props = new Properties();
            Pattern regexPattern = Pattern.compile(regex);
            Matcher matcher = regexPattern.matcher(output);

            while(matcher.find()) {
                props.put(matcher.group(1), matcher.group(2));
            }

            return props;
        }
    }

    public static final PermissionGroup GROUP = new PermissionGroup(AndroidDeviceList.class,Messages._AndroidDeviceList_PermissionGroup_Title());
    public static final Permission READ = new Permission(GROUP,"Read",Messages._AndroidList_ReadPermission(),Jenkins.READ);
    public static final Permission DEPLOY = new Permission(GROUP,"Deploy",Messages._AndroidList_DeployPermission(),Jenkins.ADMINISTER);
}
