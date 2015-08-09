package com.herodigital.wcm.ext.renditions;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    public void start(BundleContext context) throws Exception {
        log.info(context.getBundle().getSymbolicName() + " started");
    }
    
    public void stop(BundleContext context) throws Exception {
        log.info(context.getBundle().getSymbolicName() + " stopped");
    }
}
