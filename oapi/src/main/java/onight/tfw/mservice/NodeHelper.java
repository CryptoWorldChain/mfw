package onight.tfw.mservice;

import java.net.InetAddress;
import java.net.UnknownHostException;

import onight.tfw.outils.conf.PropHelper;

import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.osgi.framework.BundleContext;

import jnr.posix.POSIXFactory;

public class NodeHelper {

	private static PropHelper prop;
	
	public static PropHelper getPropInstance(){
		if(prop==null){
			synchronized(NodeHelper.class){
				if(prop==null){
					prop=new PropHelper(null);
				}
			}
		}
		return prop;
	}
	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
		}
		return "localhost";
	}


	public static String getCurrNodeID() {
		
		return getPropInstance().get("otrans.node.id", POSIXFactory.getPOSIX().getpid() + "." + getHostName());
		// mss = new MSessionSets(params.get("otrans.node.id",
		// context.getBundle().getBundleId() + "." +
		// ));
//		return "";
	}
}
