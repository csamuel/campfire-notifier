package hudson.plugins.campfire;

import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * UserProperty class which contains a user's twitter id.
 * 
 * @author landir
 */
@ExportedBean(defaultVisibility = 999)
public class UserCampfireProperty extends UserProperty {

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String campfireId;

    public UserCampfireProperty() {
    }

    @DataBoundConstructor
    public UserCampfireProperty(String campfireId) {
        this.campfireId = campfireId;
    }

    public UserPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Exported
    public User getUser() {
        return user;
    }

    @Exported
    public String getCampfireId() {
        return campfireId;
    }

    public void setCampfireId(String campfireId) {
        this.campfireId = campfireId;
    }

    public static final class DescriptorImpl extends UserPropertyDescriptor {
        public DescriptorImpl() {
            super(UserCampfireProperty.class);
        }

        @Override
        public String getDisplayName() {
            return "Campfire User Name:";
        }

        @Override
        public UserCampfireProperty newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            if (formData.has("campfireId")) {
                return req.bindJSON(UserCampfireProperty.class, formData);
            } else {
                return new UserCampfireProperty();
            }
        }

        @Override
        public UserProperty newInstance(User user) {
            return null;
        }
    }
}
