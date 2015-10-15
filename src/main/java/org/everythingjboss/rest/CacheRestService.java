package org.everythingjboss.rest;

import infinispan.org.jboss.logging.Logger;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.everythingjboss.rest.security.HotrodCallbackHandler;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SecurityConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ServerConfiguration;

import javax.json.Json;
import javax.json.JsonObject;

@ApplicationScoped
@Path("/")
public class CacheRestService implements Serializable {

    private static final long serialVersionUID = 4020979469167748304L;

    private RemoteCacheManager cm;
    private RemoteCache<String, String> cache;
    private static Logger logger = Logger.getLogger(CacheRestService.class);

    @PUT
    @POST
    @Path("initialize")
    public Response initialize(
            @QueryParam("authenticate") Boolean authenticate,
            @QueryParam("ssl") Boolean ssl,
            @QueryParam("username") String username,
            @QueryParam("password") String password, String servers) {

        String message = "The cache manager is started with JDG servers : %s";
        logger.debug("The servers list passed as argument is : " + servers);

        if(servers == null || servers.equals("")) servers="127.0.0.1:11222";
        
        // Check and close existing remote cache manager
        if (cm != null && cm.isStarted()) {
            logger.debug("Stopping existing cache manager");
            cm.stop();
        }
            
        // Check if the cache manager has already started for the application
        // and start a new one if one doesn't already exist
        if (cm == null || !cm.isStarted()) {

            // Regular configuration builder without any security
            ConfigurationBuilder configBuilder = new ConfigurationBuilder()
                    .addServers(servers);

            // Security configuration builder for Authentication and Encryption
            SecurityConfigurationBuilder securityconfigBuilder = configBuilder
                    .security();

            /*
             * For this to work, enable Authentication in the JDG server's
             * clustered.xml or standlone.xml file 
             * Add the following under the <hotrod-connector> element 
             * <authentication security-realm="ApplicationRealm"> 
             *      <sasl server-name="myhotrodserver" mechanisms="DIGEST-MD5" 
             *            qop="auth"/>
             * </authentication>
             */

            if (authenticate || ssl) {
                if (authenticate) {
                    logger.debug("Applying authentication conf for Hotrod client");
                    securityconfigBuilder
                            .authentication()
                            .enable()
                            .serverName("myhotrodserver")
                            .saslMechanism("DIGEST-MD5")
                            .callbackHandler(
                                    new HotrodCallbackHandler(username,
                                            "ApplicationRealm", password
                                                    .toCharArray()));
                }

                /*
                 * For this work, enable SSL in the JDG server's clustered.xml
                 * or standlone.xml file 
                 * 1. For 2-way SSL, add the following under <hotrod-connector>  
                 *    <encryption security-realm="ApplicationRealm"
                 *      require-ssl-client-auth="true"/>  
                 * 2. Add the following to <security-realm name="ApplicationRealm">
                 *    element 
                 *    <server-identities> 
                 *      <ssl> 
                 *          <keystore 
                 *              path="jdg-server.keystore" 
                 *              relative-to="jboss.server.config.dir"
                 *              keystore-password="jdg-server^Pas$" /> 
                 *      </ssl>
                 *    </server-identities> 
                 * 3. Add the following under <authentication> element under 
                 *                  <security-realm name="ApplicationRealm"> 
                 *    <truststore path="jdg-server.truststore" 
                 *             relative-to="jboss.server.config.dir"
                 *             keystore-password="jdg-server-trust^Pas$" /> 
                 * 4. Restart the server(s)
                 */
                if (ssl) {
                    logger.debug("Applying SSL conf for Hotrod client");
                    securityconfigBuilder
                            .ssl()
                            .enable()

                            // For this to work, copy the .keystore file to the
                            // $JBOSS_HOME/standalone/configuration folder
                            .keyStoreFileName(
                                    System.getProperty("jboss.server.config.dir")
                                            + "/jdg-client.keystore")
                            .keyStorePassword("jdg-client^Pas$".toCharArray())

                            // For this to work, copy the .truststore file to
                            // the $JBOSS_HOME/standalone/configuration folder
                            .trustStoreFileName(
                                    System.getProperty("jboss.server.config.dir")
                                            + "/jdg-client.truststore")
                            .trustStorePassword(
                                    "jdg-client-trust^Pas$".toCharArray());
                }
                cm = new RemoteCacheManager(securityconfigBuilder.build(),true);
            } else {
                cm = new RemoteCacheManager(configBuilder.build(),true);
            }
        }

        // Get the list of servers from the started cache manager
        List<ServerConfiguration> serverConfigs = cm.getConfiguration().servers();
        ArrayList<String> hosts = new ArrayList<String>();
        for (ServerConfiguration serverConfig : serverConfigs) {
            hosts.add(serverConfig.host() + ":" + serverConfig.port());
        }

        message = String.format(message, hosts);
        logger.debug("The servers list for the cacheManager is : " + hosts);

        if (cm != null && cm.isStarted()) {
            return Response.status(200).type("text/plain")
                           .entity(message)
                           .build();
        } else {
            return cacheManagerNotStarted();
        }
    }

    @GET
    @Path("{cacheName}/{key}")
    public Response get(@PathParam("cacheName") String cacheName,
            @PathParam("key") String key) {

        if (cm == null || !cm.isStarted()) {
            return cacheManagerNotStarted();
        }

        JsonObject input = Json.createObjectBuilder()
                               .add("cacheName", cacheName)
                               .add("key", key).build();

        logger.debug("Input to get method : " + input.toString());

        String value;
        try {
            cache = cm.getCache(cacheName);
            value = cache.get(key);
        } catch (Exception e) {
            return Response.status(404)
                           .entity("Exception using the cache manager : "
                                   + getStackTraceAsString(e)).build();
        }

        this.cache = cm.getCache(cacheName);
        return Response.status(200).type("text/plain").entity(value).build();
    }

    @PUT
    @POST
    @Path("{cacheName}/{key}")
    public Response put(@PathParam("cacheName") String cacheName,
            @PathParam("key") String key, String value) {

        if (cm == null || !cm.isStarted()) {
            return cacheManagerNotStarted();
        }

        JsonObject input = Json.createObjectBuilder()
                               .add("cacheName", cacheName)
                               .add("key", key)
                               .add("value", value)
                               .build();

        logger.debug("Input to put function : " + input.toString());

        try {
            cache = cm.getCache(cacheName);
            cache.put(key, value);
        } catch (Exception e) {
            return Response.status(404)
                           .entity("Exception using the cache manager : "
                                   + getStackTraceAsString(e)).build();
        }

        return Response.status(200).type("text/plain")
                       .entity("Successfully put: "+ value +" for key: " + key)
                       .build();
    }

    private Response cacheManagerNotStarted() {
        return Response.status(404).type("text/plain")
                       .entity("Remote cache manager is not started")
                       .build();
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

}
