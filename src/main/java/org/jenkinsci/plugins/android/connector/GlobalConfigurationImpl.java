package org.jenkinsci.plugins.android.connector;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

/**
 * Created by nghimbovschi on 3/2/15.
 */
@Extension
public final class GlobalConfigurationImpl extends GlobalConfiguration {
    public GlobalConfigurationImpl() {
        load();
    }

    private String mapping;

    @DataBoundConstructor
    public GlobalConfigurationImpl(String mapping) {
        super();
        load();

        this.mapping = mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getMapping() {
        return this.mapping;
    }

    @Override
    public String getDisplayName() {
        return "My Mapper";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {


        req.bindJSON(this, formData);
        save();

        return super.configure(req, formData);
    }

    private static final Logger LOGGER = Logger.getLogger(GlobalConfigurationImpl.class.getName());


}
