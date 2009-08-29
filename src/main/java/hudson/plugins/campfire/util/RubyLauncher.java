package hudson.plugins.campfire.util;

import java.util.ArrayList;

import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This utility is a simple way to keep most of your code in ruby,
 * and must pass across a "root" object from java into a "root" object
 * on the ruby side (calling a single argument method you specify - the root ruby object is created for you).
 * Ruby code can live on the the classpath, next to your java.
 * This doesn't require BSF, or any mandatory dependencies other then jruby.jar.
 *
 * @author <a href="mailto:michael.neale@gmail.com">Michael Neale</a>
 */
public class RubyLauncher {

    /** this is the root object - to be used over and over */
    private IRubyObject rootRubyObject;
    private Ruby runtime;

    /**
     *
     * @param initialRequire The name of the .rb file that is your starting point (on your claspath).
     * @param rootRubyClass The name of the ruby class in the above .rb file, must have no-arg constructor (a new instance will be created).
     * @param rootMethod The name of the method to call in the above class when "call" is called.
     */
    public RubyLauncher(String initialRequire, String rootRubyClass, String rootMethod) {

      String bootstrap =
            "require \"" + initialRequire +  "\"\n"+
            "class Bootstrap \n" +
            "   def execute root_object  \n" +
            "       " + rootRubyClass + ".new." + rootMethod + "(root_object) \n" +
            "   end    \n" +
            "end \n" +
            "Bootstrap.new";

        // This list holds the directories where the Ruby scripts can be found; unless you have complete
        // control how jruby is launched, use absolute paths
        List<String> loadPaths = new ArrayList<String>();
	loadPaths.add(".");
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(null);
	runtime = JavaEmbedUtils.initialize( loadPaths );
	Thread.currentThread().setContextClassLoader(oldClassLoader);
        rootRubyObject = JavaEmbedUtils.newRuntimeAdapter().eval( runtime, bootstrap );

    }

    /**
     * This can be called over and over on the one instance.
     *
     * Pass your root java object to the root ruby object (which was created in the constructor, with the specified method).
     * If you want to get data out, best bet is to make the root object(s) wrappers for in/out objects.
     */
    public void call(Object obj) {
        JavaEmbedUtils.invokeMethod( runtime, rootRubyObject, "execute", new Object[] {obj},  IRubyObject.class );
    }

    public void call(Object[] parameters) {
       JavaEmbedUtils.invokeMethod( runtime, rootRubyObject, "execute", parameters, IRubyObject.class );
    }
    /**
      * Use this method when embedding ruby files within a jar. They won't be found on the classpath or LOAD_PATH
      * unless added relative to the location of the jar.
      *
      * e.g. jar structure:  /com/example/rubyfiles/my_class.rb
      * loadPaths.add(getPathToJar("/com/example/rubyfiles/");
      *
      * @param jar_internal_path Absolute path to your ruby files inside the jar file
      * @return String Path URL added to JRuby's LOAD_PATH
      */
    private String getPathToJar(String jar_internal_path)
    {
        java.net.URL url =  RubyLauncher.class.getResource(jar_internal_path);
        return url.getPath();
    }
}

