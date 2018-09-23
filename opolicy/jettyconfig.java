package org.apache.felix.http.jetty.internal;

import java.io.InputStream;
import java.util.ArrayList;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

class ConfigMetaTypeProvider
  implements MetaTypeProvider
{
  private final Bundle bundle;
  
  public ConfigMetaTypeProvider(Bundle bundle)
  {
    this.bundle = bundle;
  }
  
  public String[] getLocales()
  {
    return null;
  }
  
  public ObjectClassDefinition getObjectClassDefinition(String id, String locale)
  {
    if (!"org.apache.felix.http".equals(id)) {
      return null;
    }
    final ArrayList<AttributeDefinition> adList = new ArrayList();
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.host", "Host Name", "IP Address or Host Name of the interface to which HTTP and HTTPS bind. The default is \"0.0.0.0\" indicating all interfaces.", "0.0.0.0", this.bundle.getBundleContext().getProperty("org.apache.felix.http.host")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.enable", "Enable HTTP", "Whether or not HTTP is enabled. Defaults to true thus HTTP enabled.", true, this.bundle.getBundleContext().getProperty("org.apache.felix.http.enable")));
    
    adList.add(new AttributeDefinitionImpl("org.osgi.service.http.port", "HTTP Port", "Port to listen on for HTTP requests. Defaults to 8080.", 8080, this.bundle.getBundleContext().getProperty("org.osgi.service.http.port")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.timeout", "Connection Timeout", "Time limit for reaching an timeout specified in milliseconds. This property applies to both HTTP and HTTP connections. Defaults to 60 seconds.", 60000, this.bundle.getBundleContext().getProperty("org.apache.felix.http.timeout")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.enable", "Enable HTTPS", "Whether or not HTTPS is enabled. Defaults to false thus HTTPS disabled.", false, this.bundle.getBundleContext().getProperty("org.apache.felix.https.enable")));
    
    adList.add(new AttributeDefinitionImpl("org.osgi.service.http.port.secure", "HTTPS Port", "Port to listen on for HTTPS requests. Defaults to 433.", 433, this.bundle.getBundleContext().getProperty("org.osgi.service.http.port.secure")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.keystore", "Keystore", "Absolute Path to the Keystore to use for HTTPS. Only used if HTTPS is enabled in which case this property is required.", null, this.bundle.getBundleContext().getProperty("org.apache.felix.https.keystore")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.keystore.password", "Keystore Password", "Password to access the Keystore. Only used if HTTPS is enabled.", null, this.bundle.getBundleContext().getProperty("org.apache.felix.https.keystore.password")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.keystore.key.password", "Key Password", "Password to unlock the secret key from the Keystore. Only used if HTTPS is enabled.", null, this.bundle.getBundleContext().getProperty("org.apache.felix.https.keystore.key.password")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.truststore", "Truststore", "Absolute Path to the Truststore to use for HTTPS. Only used if HTTPS is enabled.", null, this.bundle.getBundleContext().getProperty("org.apache.felix.https.truststore")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.truststore.password", "Truststore Password", "Password to access the Truststore. Only used if HTTPS is enabled.", null, this.bundle.getBundleContext().getProperty("org.apache.felix.https.truststore.password")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.clientcertificate", "Client Certificate", "Requirement for the Client to provide a valid certificate. Defaults to none.", 1, new String[] { "none" }, 0, new String[] { "No Client Certificate", "Client Certificate Wanted", "Client Certificate Needed" }, new String[] { "none", "wants", "needs" }, this.bundle.getBundleContext().getProperty("org.apache.felix.https.clientcertificate")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.context_path", "Context Path", "The Servlet Context Path to use for the Http Service. If this property is not configured it defaults to \"/\". This must be a valid path starting with a slash and not ending with a slash (unless it is the root context).", "/", this.bundle.getBundleContext().getProperty("org.apache.felix.http.context_path")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.mbeans", "Register MBeans", "Whether or not to use register JMX MBeans from the servlet container (Jetty). If this is enabled Jetty Request and Connector statistics are also enabled. The default is to not enable JMX.", false, this.bundle.getBundleContext().getProperty("org.apache.felix.http.mbeans")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.session.timeout", "Session Timeout", "Default lifetime of an HTTP session specified in a whole number of minutes. If the timeout is 0 or less, sessions will by default never timeout. The default is 0.", 0, this.bundle.getBundleContext().getProperty("org.apache.felix.http.session.timeout")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.jetty.threadpool.max", "Thread Pool Max", "Maximum number of jetty threads. Using the default -1 uses Jetty's default (200).", -1, this.bundle.getBundleContext().getProperty("org.apache.felix.http.jetty.threadpool.max")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.jetty.acceptors", "Acceptors", "Number of acceptor threads to use, or -1 for a default value. Acceptors accept new TCP/IP connections. If 0, then the selector threads are used to accept connections.", -1, this.bundle.getBundleContext().getProperty("org.apache.felix.http.jetty.acceptors")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.jetty.selectors", "Selectors", "Number of selector threads, or <=0 for a default value. Selectors notice and schedule established connection that can make IO progress.", -1, this.bundle.getBundleContext().getProperty("org.apache.felix.http.jetty.selectors")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.jetty.headerBufferSize", "Header Buffer Size", "Size of the buffer for request and response headers. Default is 16KB.", 16384, this.bundle.getBundleContext().getProperty("org.apache.felix.http.jetty.headerBufferSize")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.jetty.requestBufferSize", "Request Buffer Size", "Size of the buffer for requests not fitting the header buffer. Default is 8KB.", 8192, this.bundle.getBundleContext().getProperty("org.apache.felix.http.jetty.requestBufferSize")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.jetty.responseBufferSize", "Response Buffer Size", "Size of the buffer for responses. Default is 24KB.", 24576, this.bundle.getBundleContext().getProperty("org.apache.felix.http.jetty.responseBufferSize")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.jetty.maxFormSize", "Maximum Form Size", "Size of Body for submitted form content. Default is 200KB.", 204800, this.bundle.getBundleContext().getProperty("org.apache.felix.http.jetty.maxFormSize")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.debug", "Debug Logging", "Whether to write DEBUG level messages or not. Defaults to false.", false, this.bundle.getBundleContext().getProperty("org.apache.felix.http.debug")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.path_exclusions", "Path Exclusions", "Contains a list of context path prefixes. If a Web Application Bundle is started with a context path matching any of these prefixes, it will not be deployed in the servlet container.", 1, new String[] { "/system" }, Integer.MAX_VALUE, null, null, getStringArray(this.bundle.getBundleContext().getProperty("org.apache.felix.http.path_exclusions"))));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.jetty.ciphersuites.excluded", "Excluded Cipher Suites", "List of cipher suites that should be excluded. Default is none.", 1, null, Integer.MAX_VALUE, null, null, getStringArray(this.bundle.getBundleContext().getProperty("org.apache.felix.https.jetty.ciphersuites.excluded"))));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.jetty.ciphersuites.included", "Included Cipher Suites", "List of cipher suites that should be included. Default is none.", 1, null, Integer.MAX_VALUE, null, null, getStringArray(this.bundle.getBundleContext().getProperty("org.apache.felix.https.jetty.ciphersuites.included"))));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.http.jetty.sendServerHeader", "Send Server Header", "If enabled, the server header is sent.", false, this.bundle.getBundleContext().getProperty("org.apache.felix.http.jetty.sendServerHeader")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.jetty.protocols.included", "Included Protocols", "List of SSL protocols to include by default. Protocols may be any supported by the Java platform such as SSLv2Hello, SSLv3, TLSv1, TLSv1.1, or TLSv1.2. Any listed protocol not supported is silently ignored. Default is none assuming to use any protocol enabled and supported on the platform.", 1, null, Integer.MAX_VALUE, null, null, getStringArray(this.bundle.getBundleContext().getProperty("org.apache.felix.https.jetty.protocols.included"))));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.jetty.protocols.excluded", "Excluded Protocols", "List of SSL protocols to exclude. This property further restricts the enabled protocols by explicitly disabling. Any protocol listed in both this property and the Included protocols property is excluded. Default is none such as to accept all protocols enabled on platform or explicitly listed by the Included protocols property.", 1, null, Integer.MAX_VALUE, null, null, getStringArray(this.bundle.getBundleContext().getProperty("org.apache.felix.https.jetty.protocols.excluded"))));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.proxy.load.balancer.connection.enable", "Enable Proxy/Load Balancer Connection", "Whether or not the Proxy/Load Balancer Connection is enabled. Defaults to false thus disabled.", false, this.bundle.getBundleContext().getProperty("org.apache.felix.proxy.load.balancer.connection.enable")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.jetty.renegotiateAllowed", "Renegotiation allowed", "Whether TLS renegotiation is allowed (true by default)", false, this.bundle.getBundleContext().getProperty("org.apache.felix.https.jetty.renegotiateAllowed")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.jetty.session.cookie.httpOnly", "Session Cookie httpOnly", "Session Cookie httpOnly (true by default)", true, this.bundle.getBundleContext().getProperty("org.apache.felix.https.jetty.session.cookie.httpOnly")));
    
    adList.add(new AttributeDefinitionImpl("org.apache.felix.https.jetty.session.cookie.secure", "Session Cookie secure", "Session Cookie secure (false by default)", false, this.bundle.getBundleContext().getProperty("org.apache.felix.https.jetty.session.cookie.secure")));
    
    new ObjectClassDefinition()
    {
      private final AttributeDefinition[] attrs = (AttributeDefinition[])adList.toArray(new AttributeDefinition[adList.size()]);
      
      public String getName()
      {
        return "Apache Felix Jetty Based Http Service";
      }
      
      public InputStream getIcon(int arg0)
      {
        return null;
      }
      
      public String getID()
      {
        return "org.apache.felix.http";
      }
      
      public String getDescription()
      {
        return "Configuration for the embedded Jetty Servlet Container.";
      }
      
      public AttributeDefinition[] getAttributeDefinitions(int filter)
      {
        return filter == 2 ? null : this.attrs;
      }
    };
  }
  
  private String[] getStringArray(String value)
  {
    if (value != null) {
      return value.trim().split(",");
    }
    return null;
  }
  
  private static class AttributeDefinitionImpl
    implements AttributeDefinition
  {
    private final String id;
    private final String name;
    private final String description;
    private final int type;
    private final String[] defaultValues;
    private final int cardinality;
    private final String[] optionLabels;
    private final String[] optionValues;
    
    AttributeDefinitionImpl(String id, String name, String description, String defaultValue, String overrideValue)
    {
      this(id, name, description, 1, new String[] { defaultValue == null ? null : defaultValue }, 0, null, null, new String[] { overrideValue == null ? null : overrideValue });
    }
    
    AttributeDefinitionImpl(String id, String name, String description, int defaultValue, String overrideValue)
    {
      this(id, name, description, 3, new String[] { String.valueOf(defaultValue) }, 0, null, null, new String[] { overrideValue == null ? null : overrideValue });
    }
    
    AttributeDefinitionImpl(String id, String name, String description, boolean defaultValue, String overrideValue)
    {
      this(id, name, description, 11, new String[] { String.valueOf(defaultValue) }, 0, null, null, new String[] { overrideValue == null ? null : overrideValue });
    }
    
    AttributeDefinitionImpl(String id, String name, String description, int type, String[] defaultValues, int cardinality, String[] optionLabels, String[] optionValues, String overrideValue)
    {
      this(id, name, description, type, defaultValues, cardinality, optionLabels, optionValues, new String[] { overrideValue == null ? null : overrideValue });
    }
    
    AttributeDefinitionImpl(String id, String name, String description, int type, String[] defaultValues, int cardinality, String[] optionLabels, String[] optionValues, String[] overrideValues)
    {
      this.id = id;
      this.name = name;
      this.description = description;
      this.type = type;
      if (overrideValues != null) {
        this.defaultValues = overrideValues;
      } else {
        this.defaultValues = defaultValues;
      }
      this.cardinality = cardinality;
      this.optionLabels = optionLabels;
      this.optionValues = optionValues;
    }
    
    public int getCardinality()
    {
      return this.cardinality;
    }
    
    public String[] getDefaultValue()
    {
      return this.defaultValues;
    }
    
    public String getDescription()
    {
      return this.description;
    }
    
    public String getID()
    {
      return this.id;
    }
    
    public String getName()
    {
      return this.name;
    }
    
    public String[] getOptionLabels()
    {
      return this.optionLabels;
    }
    
    public String[] getOptionValues()
    {
      return this.optionValues;
    }
    
    public int getType()
    {
      return this.type;
    }
    
    public String validate(String arg0)
    {
      return null;
    }
  }
}
