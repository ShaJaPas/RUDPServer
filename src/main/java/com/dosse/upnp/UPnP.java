package com.dosse.upnp;

public class UPnP {
	private static Gateway defaultGW = null;
	
	private static final GatewayFinder finder = new GatewayFinder() {
			public void gatewayFound(Gateway g) {
				synchronized (finder) {
					if (defaultGW == null)
						defaultGW = g; 
				} 
			}
		};
	
	public static void waitInit() {
		while (finder.isSearching()) {
			try {
				Thread.sleep(1L);
			} catch (InterruptedException interruptedException) {}
		} 
	}
	
	public static boolean isUPnPAvailable() {
		waitInit();
		return (defaultGW != null);
	}
	
	public static boolean openPortTCP(int port) {
		if (!isUPnPAvailable())
			return false; 
		return defaultGW.openPort(port, false);
	}
	
	public static boolean openPortUDP(int port) {
		if (!isUPnPAvailable())
			return false; 
		return defaultGW.openPort(port, true);
	}
	
	public static boolean closePortTCP(int port) {
		if (!isUPnPAvailable())
			return false; 
		return defaultGW.closePort(port, false);
	}
	
	public static boolean closePortUDP(int port) {
		if (!isUPnPAvailable())
			return false; 
		return defaultGW.closePort(port, true);
	}
	
	public static boolean isMappedTCP(int port) {
		if (!isUPnPAvailable())
			return false; 
		return defaultGW.isMapped(port, false);
	}
	
	public static boolean isMappedUDP(int port) {
		if (!isUPnPAvailable())
			return false; 
		return defaultGW.isMapped(port, false);
	}
	
	public static String getExternalIP() {
		if (!isUPnPAvailable())
			return null; 
		return defaultGW.getExternalIP();
	}
	
	public static String getLocalIP() {
		if (!isUPnPAvailable())
			return null; 
		return defaultGW.getLocalIP();
	}
}
