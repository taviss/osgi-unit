/**
 * Copyright (c) 2016 Dr. Chris Senior.
 * See LICENSE.txt for licensing terms
 */
package com.asteroid.duck.osgi;

import com.asteroid.duck.osgi.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper methods and utilites used by the
 */
public class Utils {
    /** Logging */
    private static final Logger LOG = Logger.getLogger(Utils.class);
    /** A wildcard package suffix */
    public static final String WILDCARD = ".*";

    /**
     * Extract all the packages from the given class loader by using reflection to call the
     * #getPackages() method.
     *
     * @param loader the class loader we want the packages for
     *
     * @return an array of packages known to that class loader; or empty array.
     */
    public static Package[] getPackages(ClassLoader loader) {
        try {
            Method getpkgs = ClassLoader.class.getMethod("getPackages", new Class[]{});
            return (Package[]) getpkgs.invoke(loader, new Object[]{});

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return new Package[0];
    }

    /**
     * Attempt to figure out a list of "bootstrap" package names for the framework, using any of:
     * 1) the value of the `org.osgi.framework.system.packages` property
     * 2) the value of the `com.asteroid.duck.osgi.system.packages` property
     * @param pkgs
     * @return
     */
    public static List<String> getBootstrapPackages(Package[] pkgs) {
        ArrayList<String> results = new ArrayList<String>();
        parsePackages(System.getProperty("org.osgi.framework.system.packages", ""), results);
        parsePackages(System.getProperty(Constants.SYSTEM_PACKAGES, ""), results);
        if (pkgs != null) {
            for (Package pkg : pkgs) {
                results.add(pkg.getName());
            }
        }
        // print to debug
        if (LOG.isTraceEnabled()) {
            Iterator<String> iter = results.iterator();
            String debug = "Ignored packages=";
            while (iter.hasNext()) {
                debug += iter.next();
                if (iter.hasNext()) {
                    debug += ",";
                }
            }
            LOG.trace(debug);
        }
        return results;
    }

    /** Given a comma separated list of packages, separate them into single packages */
    private static void parsePackages(final String packages, final List<String> results) {
        LOG.debug(packages);
        String[] split = packages.split(",");
        for (String s : split) {
            // walk them to eradicate empty or null entries
            if (s != null && s.length() > 0) {
                results.add(s);
            }
        }
    }

    /**
     * Is the given bundle really a "fragment" in OSGi
     * @param bundle the bundle to test
     * @return true if a fragment
     */
    public static boolean isFragment(final Bundle bundle) {
        return getFragmentHost(bundle) != null;
    }

    /**
     * Get the Fragment-Host of the given bundle (assuming it is a fragment)
     * @param bundle the fragment bundle
     * @return the host bundle symbolic name (or null if this is not a fragment)
     */
    public static String getFragmentHost(final Bundle bundle) {
        return (String) bundle.getHeaders().get(org.osgi.framework.Constants.FRAGMENT_HOST);
    }

    /**
     * Find a bundle by it's symbolic name
     *
     * @param ctx                the context to search for the bundle
     * @param bundleSymbolicName the symbolic name to search for
     * @param fragmentHost       should we return a fragment host, if the target bundle is a fragment
     *
     * @return the bundle we are searching for
     */
    public static Bundle getBundle(BundleContext ctx, String bundleSymbolicName, boolean fragmentHost) {
        for (Bundle b : ctx.getBundles()) {
            if (bundleSymbolicName.equals(b.getSymbolicName())) {
                if (fragmentHost) {
                    // test if this is a fragment - and if so return the "host"
                    String fragHost = getFragmentHost(b);
                    if (fragHost != null) {
                        // recursively find the host
                        return getBundle(ctx, fragHost, false);
                    }
                }
                return b;
            }
        }
        return null;
    }

    /** Given a class name - extract the package name */
    public static String getPackageName(final String name) {
        int index = name.lastIndexOf('.');
        if (index > 0) {
            return name.substring(0, index);
        }
        return "";
    }

    /** Is this named package in a list of packages (that might include wildcards) */
    public static boolean packageMatch(final List<String> bootClassPackages, final String packageName) {
        if (packageName != null) {
            for (String bootPkg : bootClassPackages) {
                if (bootPkg.endsWith(WILDCARD)) {
                    // Extract start of package without wildcard
                    String packageStart = bootPkg.substring(0, bootPkg.length() - WILDCARD.length());
                    if (packageName.startsWith(packageStart)) {
                        // wildcard match
                        return true;
                    }
                } else if (bootPkg.equals(packageName)) {
                    // exact match
                    return true;
                }
            }
        }
        return false;
    }
}
