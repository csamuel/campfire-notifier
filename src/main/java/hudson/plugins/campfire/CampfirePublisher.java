package hudson.plugins.campfire;

import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.campfire.util.RubyLauncher;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Mailer;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Chris Samuel
 */
public class CampfirePublisher extends Publisher {

    private static final List<String> VALUES_REPLACED_WITH_NULL = Arrays.asList("", "(Default)",
            "(System Default)");

    private static final Logger LOGGER = Logger.getLogger(CampfirePublisher.class.getName());

    private String id;
    private String password;
    private String domain;
    private String room;
    private Boolean onlyOnFailureOrRecovery;
    private Boolean includeUrl;

    @SuppressWarnings("deprecation")
	private CampfirePublisher(String id, String password, String domain, String room, Boolean onlyOnFailureOrRecovery,
            Boolean includeUrl) {
        this.onlyOnFailureOrRecovery = onlyOnFailureOrRecovery;
        this.includeUrl = includeUrl;
        this.id = id;
        this.password = password;
        this.domain = domain;
        this.room = room;
    }

    @DataBoundConstructor
    public CampfirePublisher(String id, String password, String domain, String room, String onlyOnFailureOrRecovery,
            String includeUrl) {
        this(cleanToString(id), cleanToString(password), cleanToString(domain), cleanToString(room), cleanToBoolean(onlyOnFailureOrRecovery),
                cleanToBoolean(includeUrl));
    }

    private static String cleanToString(String string) {
        return VALUES_REPLACED_WITH_NULL.contains(string) ? null : string;
    }

    private static Boolean cleanToBoolean(String string) {
        return (VALUES_REPLACED_WITH_NULL.contains(string) || string == null) ? null : Boolean
                .valueOf(string);
    }

    public String getId() {
        return id;
    }

    public Boolean getIncludeUrl() {
        return includeUrl;
    }

    public Boolean getOnlyOnFailureOrRecovery() {
        return onlyOnFailureOrRecovery;
    }

    public String getPassword() {
        return password;
    }

    public String getDomain() {
        return domain;
    }

    public String getRoom() {
        return room;
    }


    @SuppressWarnings("unchecked")
    @Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
            return _perform(build, launcher, listener);
    }

    protected <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> boolean _perform(
    		B build, Launcher launcher, BuildListener listener) {
        if (shouldNotify(build)) {
            try {
                String message = createCampfireStatusMessage(build);
                System.out.println(message);
                RubyLauncher rubyLauncher = new RubyLauncher("ruby/boot", "Boot", "start");
                rubyLauncher.call(createNotificationMap(message));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to send Campfire message.", e);
            }
        }
        return true;

    }


	private <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String createCampfireStatusMessage(
            B build) throws IOException {
        String projectName = build.getProject().getName();
        String result = build.getResult().toString();
        String toblame = "";
        try {
            if (!build.getResult().equals(Result.SUCCESS)) {
                toblame = getUserString(build);
            }
        } catch (Exception e) {
        }
        String url = "";
        if (shouldIncludeUrl()) {
            url = ((DescriptorImpl) getDescriptor()).getUrl() + build.getUrl();
            
        }
        return String.format("%s\nBUILD %s \n%s #%d %s \n\n%s", toblame, result, projectName,
                build.number, url, build.getLog());

    }

    

	private Map<String, Object> createNotificationMap(String message) {
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("email", id);
        root.put("password", password);
        root.put("domain", domain);
        root.put("room_name", room);
        root.put("message", message);
        return root;
    }

    private <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getUserString(
            B build) throws IOException {
        StringBuilder userString = new StringBuilder("");
        Set<User> culprits = build.getCulprits();
        ChangeLogSet<? extends Entry> changeSet = build.getChangeSet();
        if (culprits.size() > 0) {
            for (User user : culprits) {
                UserCampfireProperty tid = user.getProperty(UserCampfireProperty.class);
                if (tid.getCampfireId() != null) {
                    userString.append("@").append(tid.getCampfireId()).append(" ");
                }
            }
        } else if (changeSet != null) {
            for (Entry entry : changeSet) {
                User user = entry.getAuthor();
                UserCampfireProperty tid = user.getProperty(UserCampfireProperty.class);
                if (tid.getCampfireId() != null) {
                    userString.append("@").append(tid.getCampfireId()).append(" ");
                }
            }
        }
        return userString.toString();
    }

    protected <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String createStatusWithoutURL(
            B build) {
        String projectName = build.getProject().getName();
        String result = build.getResult().toString();
        return String.format("%s:%s #%d", result, projectName, build.number);
    }

    protected <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String createStatusWithURL(
            B build) throws IOException {
        String projectName = build.getProject().getName();
        String result = build.getResult().toString();
        String absoluteBuildURL = ((DescriptorImpl) getDescriptor()).getUrl() + build.getUrl();
        String tinyUrl = absoluteBuildURL;
        return String.format("%s:%s #%d - %s", result, projectName, build.number, tinyUrl);
    }

    /**
     * Detrmine if this build represents a failure or recovery. A build failure
     * includes both failed and unstable builds. A recovery is defined as a
     * successful build that follows a build that was not successful. Always
     * returns false for aborted builds.
     *
     * @param build the Build object
     * @return true if this build represents a recovery or failure
     */
    protected <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> boolean isFailureOrRecovery(
            B build) {
        if (build.getResult() == Result.FAILURE || build.getResult() == Result.UNSTABLE) {
            return true;
        } else if (build.getResult() == Result.SUCCESS) {
            B previousBuild = build.getPreviousBuild();
            if (previousBuild != null && previousBuild.getResult() != Result.SUCCESS) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected boolean shouldIncludeUrl() {
        if (includeUrl != null) {
            return includeUrl.booleanValue();
        } else {
            return ((DescriptorImpl) getDescriptor()).includeUrl;
        }
    }

    /**
     * Determine if this build results should be tweeted. Uses the local
     * settings if they are provided, otherwise the global settings.
     *
     * @param build the Build object
     * @return true if we should tweet this build result
     */
    protected <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> boolean shouldNotify(
            B build) {
        if (onlyOnFailureOrRecovery == null) {
            if (((DescriptorImpl) getDescriptor()).onlyOnFailureOrRecovery) {
                return isFailureOrRecovery(build);
            } else {
                return true;
            }
        } else if (onlyOnFailureOrRecovery.booleanValue()) {
            return isFailureOrRecovery(build);
        } else {
            return true;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Publisher> {
        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

        public String id;
        public String password;
        public String domain;
        public String room;
        public String hudsonUrl;

      
        public boolean onlyOnFailureOrRecovery;
        public boolean includeUrl;

        public DescriptorImpl() {
            super(CampfirePublisher.class);
            load();
        }

        @Override
        public boolean configure(StaplerRequest req) throws FormException {
            // set the booleans to false as defaults
            includeUrl = false;
            onlyOnFailureOrRecovery = false;

            req.bindParameters(this, "campfire.");
            hudsonUrl = Mailer.descriptor().getUrl();
            save();
            return super.configure(req);
        }

        @Override
        public String getDisplayName() {
            return "Campfire";
        }

        public String getId() {
            return id;
        }

        public String getPassword() {
            return password;
        }

        public String getDomain() {
            return domain;
        }

        public String getRoom() {
            return room;
        }

        public String getUrl() {
            return hudsonUrl;
        }

        public boolean isIncludeUrl() {
            return includeUrl;
        }

        public boolean isOnlyOnFailureOrRecovery() {
            return onlyOnFailureOrRecovery;
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            if (hudsonUrl == null) {
                // if Hudson URL is not configured yet, infer some default
                hudsonUrl = Functions.inferHudsonURL(req);
                save();
            }
            return super.newInstance(req, formData);
        }

        
    }
}