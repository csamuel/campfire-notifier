package hudson.plugins.campfire;

import hudson.plugins.campfire.util.RubyLauncher;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class RubyLauncherTest {
    
    @Test
    public void testLaunch() throws NoSuchMethodException {
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("email", "dasnotifier@gmail.com");
        root.put("password", "Dtaivr2011");
        root.put("domain", "teamdas");
        root.put("room_name", "DAS");
        root.put("message", "Here we go!\n \tThis is great!");
        RubyLauncher launcher = new RubyLauncher("ruby/boot", "Boot", "start");
        launcher.call(root);
    }
}
