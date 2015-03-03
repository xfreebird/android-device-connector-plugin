package org.jenkinsci.plugins.android.connector;

import hudson.model.Computer;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Properties;

/**
 * @author Kohsuke Kawaguchi
 * @author Nicolae Ghimbovschi (Android adaptation)
 */
public class AndroidDevice implements Serializable, ModelObject {

    /** Device property key: product type. */
    static final String PROP_PRODUCT_TYPE = "ProductType";
    static final String PROP_PRODUCT_ID = "UniqueDeviceID";
    static final String PROP_PRODUCT_MODEL = "ro.product.model";
    static final String PROP_ALTERNATIVE_NAME = "AlternativeName";

    static final String PROP_PRODUCT_OS_VERSION = "ro.build.version.release";
    static final String DEVICE_DEFAULT_NAME = "Android Device";

    /**
     * Which computer is this connected to?
     */
    /*package*/ transient Computer computer;

    private final Properties props = new Properties();

    public AndroidDevice(Properties props) {
        this.props.putAll(props);
    }

    public Computer getComputer() {
        return computer;
    }

    public String getDisplayName() {
        return getDeviceName();
    }

    public String getDeviceName() {
        String deviceName = DEVICE_DEFAULT_NAME;

        if (props.containsKey(PROP_PRODUCT_MODEL)) {
            deviceName = props.getProperty(PROP_PRODUCT_MODEL);
        }

        if (props.containsKey(PROP_ALTERNATIVE_NAME)) {
            deviceName = props.getProperty(PROP_ALTERNATIVE_NAME);
        }

        return deviceName;
    }

    public String getUniqueDeviceId() {
        return props.getProperty(PROP_PRODUCT_ID);
    }

    public String getBuildVersionRelease() {
        return props.getProperty(PROP_PRODUCT_OS_VERSION);
    }

    public Properties getProps() {
        return props;
    }

    /**
     * Deploys a .apk file to this device.
     */
    public void deploy(File bundle, TaskListener listener) throws IOException, InterruptedException {
        Jenkins.getInstance().checkPermission(AndroidDeviceList.DEPLOY);
        computer.getChannel().call(new DeployTask(this, bundle, listener));
        computer.getChannel().syncLocalIO();    // TODO: verify if needed
    }

    public HttpResponse doDoDeploy(StaplerRequest req) throws IOException {
        // The web interface can only support uploading self-contained .ipa files,
        // not .app directory bundles, so give the temporary file a .ipa suffix
        File f = File.createTempFile("jenkins",".apk");
        StringWriter w = new StringWriter();
        try {
            req.getFileItem("apk").write(f);
            deploy(f,new StreamTaskListener(w));
            return HttpResponses.forwardToView(this,"ok").with("msg",w.toString());
        } catch (Exception e) {
            // failed to deploy
            throw HttpResponses.error(StaplerResponse.SC_INTERNAL_SERVER_ERROR,
                    new Error("Failed to deploy app: "+w,e));
        } finally {
            f.delete();
        }
    }

    private static final long serialVersionUID = 1L;
}
