/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package vmwareConDiag;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.mo.util.MorUtil;
import com.vmware.vim25.ws.WSClient;
import org.sblim.wbem.cim.*;
import org.sblim.wbem.client.CIMClient;
import org.sblim.wbem.client.PasswordCredential;
import org.sblim.wbem.client.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * <p>ViJavaConnectTest class.</p>
 *
 * @author <a href="mailto:ronny@opennms.org">Ronny Trommer</a>
 * @version $Id: $
 * @since 1.0-SNAPSHOT
 */
public class ViJavaConnectTest {
    /**
     * logging for VMware library VI Java
     */
    private final Logger logger = LoggerFactory.getLogger("OpenNMS.VMware." + ViJavaConnectTest.class.getName());

    private String m_hostname = null;

    private String m_username = null;

    private String m_password = null;

    private ServiceInstance m_serviceInstance = null;

    private Map<HostSystem, HostServiceTicket> m_hostServiceTickets = new HashMap<HostSystem, HostServiceTicket>();

    private Map<HostSystem, String> m_hostSystemCimUrls = new HashMap<HostSystem, String>();

    /**
     * Constructor for creating a instance for a given server and credentials.
     *
     * @param hostname the vCenter's hostname
     * @param username the username
     * @param password the password
     */
    public ViJavaConnectTest(String hostname, String username, String password) {
        this.m_hostname = hostname;
        this.m_username = username;
        this.m_password = password;
    }

    /**
     * Connects to the server.
     *
     * @throws java.net.MalformedURLException
     * @throws java.rmi.RemoteException
     */
    public ServiceInstance connect() throws MalformedURLException, RemoteException {
        relax();

        return new ServiceInstance(new URL("https://" + m_hostname + "/sdk"), m_username, m_password);
    }

    /**
     * Sets the timeout for server connections.
     *
     * @param timeout the timeout to be used for connecting
     * @return true, if the operation was successful
     */
    public boolean setTimeout(int timeout) {
        if (m_serviceInstance != null) {
            ServerConnection serverConnection = m_serviceInstance.getServerConnection();
            if (serverConnection != null) {
                VimPortType vimService = serverConnection.getVimService();
                if (vimService != null) {
                    WSClient wsClient = vimService.getWsc();
                    if (wsClient != null) {
                        wsClient.setConnectTimeout(timeout);
                        wsClient.setReadTimeout(timeout);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        if (m_serviceInstance == null) {
            // not connected
            return;
        } else {
            ServerConnection serverConnection = m_serviceInstance.getServerConnection();

            if (serverConnection == null) {
                // not connected
                return;
            } else {
                m_serviceInstance.getServerConnection().logout();
            }
        }
    }

    /**
     * This method is used to "relax" the policies concerning self-signed certificates.
     */
    protected void relax() {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            }
        }};

        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception exception) {
            logger.warn("Error setting relaxed SSL policy", exception);
        }

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
    }

    /**
     * Returns a managed entity for a given managed object Id.
     *
     * @param managedObjectId the managed object Id
     * @return the managed entity
     */
    public ManagedEntity getManagedEntityByManagedObjectId(String managedObjectId) {
        ManagedObjectReference managedObjectReference = new ManagedObjectReference();

        managedObjectReference.setType("ManagedEntity");
        managedObjectReference.setVal(managedObjectId);

        ManagedEntity managedEntity = MorUtil.createExactManagedEntity(m_serviceInstance.getServerConnection(), managedObjectReference);

        return managedEntity;
    }

    /**
     * Returns a virtual machine by a given managed object Id.
     *
     * @param managedObjectId the managed object Id
     * @return the virtual machine object
     */
    public VirtualMachine getVirtualMachineByManagedObjectId(String managedObjectId) {
        ManagedObjectReference managedObjectReference = new ManagedObjectReference();

        managedObjectReference.setType("VirtualMachine");
        managedObjectReference.setVal(managedObjectId);

        VirtualMachine virtualMachine = (VirtualMachine) MorUtil.createExactManagedEntity(m_serviceInstance.getServerConnection(), managedObjectReference);

        return virtualMachine;
    }

    /**
     * Returns a host system by a given managed object Id.
     *
     * @param managedObjectId the managed object Id
     * @return the host system object
     */
    public HostSystem getHostSystemByManagedObjectId(String managedObjectId) {
        ManagedObjectReference managedObjectReference = new ManagedObjectReference();

        managedObjectReference.setType("HostSystem");
        managedObjectReference.setVal(managedObjectId);

        HostSystem hostSystem = (HostSystem) MorUtil.createExactManagedEntity(m_serviceInstance.getServerConnection(), managedObjectReference);

        return hostSystem;
    }

    /**
     * Queries a host system for Cim data.
     *
     * @param hostSystem       the host system to query
     * @param cimClass         the class of Cim objects to retrieve
     * @param primaryIpAddress the Ip address to use
     * @return the list of Cim objects
     * @throws RemoteException
     * @throws org.sblim.wbem.cim.CIMException
     *
     */
    public List<CIMObject> queryCimObjects(HostSystem hostSystem, String cimClass, String primaryIpAddress) throws ConnectException, RemoteException, CIMException {
        List<CIMObject> cimObjects = new ArrayList<CIMObject>();

        if (!m_hostServiceTickets.containsKey(hostSystem)) {
            m_hostServiceTickets.put(hostSystem, hostSystem.acquireCimServicesTicket());
        }

        HostServiceTicket hostServiceTicket = m_hostServiceTickets.get(hostSystem);

        if (!m_hostSystemCimUrls.containsKey(hostSystem)) {
            String ipAddress = primaryIpAddress;

            if (ipAddress == null) {
                ipAddress = getPrimaryHostSystemIpAddress(hostSystem);
            }


            if (ipAddress == null) {
                logger.warn("Cannot determine ip address for host system '{}'", hostSystem.getMOR().getVal());
                return cimObjects;
            }

            m_hostSystemCimUrls.put(hostSystem, "https://" + ipAddress + ":5989");
        }

        String cimAgentAddress = m_hostSystemCimUrls.get(hostSystem);

        String namespace = "root/cimv2";

        UserPrincipal userPr = new UserPrincipal(hostServiceTicket.getSessionId());
        PasswordCredential pwCred = new PasswordCredential(hostServiceTicket.getSessionId().toCharArray());

        CIMNameSpace ns = new CIMNameSpace(cimAgentAddress, namespace);
        CIMClient cimClient = new CIMClient(ns, userPr, pwCred);

        // very important to query esx5 hosts
        cimClient.useMPost(false);

        CIMObjectPath rpCOP = new CIMObjectPath(cimClass);

        Enumeration<?> rpEnm = cimClient.enumerateInstances(rpCOP);

        while (rpEnm.hasMoreElements()) {
            CIMObject rp = (CIMObject) rpEnm.nextElement();

            cimObjects.add(rp);
        }

        return cimObjects;
    }

    /**
     * Queries a host system for Cim data.
     *
     * @param hostSystem the host system to query
     * @param cimClass   the class of Cim objects to retrieve
     * @return the list of Cim objects
     * @throws RemoteException
     * @throws CIMException
     */
    public List<CIMObject> queryCimObjects(HostSystem hostSystem, String cimClass) throws ConnectException, RemoteException, CIMException {
        return queryCimObjects(hostSystem, cimClass, null);
    }

    /**
     * Searches for the primary ip address of a host system.
     * <p>The idea is to resolve the HostSystem's name and use the resulting IP if the IP is listed on the available addresses list,
     * otherwise, use the first ip listed on the available list.</p>
     *
     * @param hostSystem the host system to query
     * @return the primary ip address
     * @throws RemoteException
     */
    // TODO We should use the IP of the "Management Network" (i.e. the port that has enabled "Management Traffic" on the available vSwitches).
    //      Resolving the name of the HostSystem as the FQDN is the most closest thing for that.
    public String getPrimaryHostSystemIpAddress(HostSystem hostSystem) throws RemoteException {
        TreeSet<String> addresses = getHostSystemIpAddresses(hostSystem);
        String ipAddress = null;
        try {
            ipAddress = InetAddress.getByName(hostSystem.getName()).getHostAddress();
        } catch (Exception e) {
            logger.debug("Can't resolve the IP address from {}.", hostSystem.getName());
        }
        if (ipAddress == null) {
            return addresses.first();
        }
        return addresses.contains(ipAddress) ? ipAddress : addresses.first();
    }

    /**
     * Searches for all ip addresses of a host system
     *
     * @param hostSystem the host system to query
     * @return the ip addresses of the host system, the first one is the primary
     * @throws RemoteException
     */
    public TreeSet<String> getHostSystemIpAddresses(HostSystem hostSystem) throws RemoteException {
        TreeSet<String> ipAddresses = new TreeSet<String>();

        HostNetworkSystem hostNetworkSystem = hostSystem.getHostNetworkSystem();

        if (hostNetworkSystem != null) {
            HostNetworkInfo hostNetworkInfo = hostNetworkSystem.getNetworkInfo();
            if (hostNetworkInfo != null) {
                HostVirtualNic[] hostVirtualNics = hostNetworkInfo.getConsoleVnic();
                if (hostVirtualNics != null) {
                    for (HostVirtualNic hostVirtualNic : hostVirtualNics) {
                        ipAddresses.add(hostVirtualNic.getSpec().getIp().getIpAddress());
                    }
                }
                hostVirtualNics = hostNetworkInfo.getVnic();
                if (hostVirtualNics != null) {
                    for (HostVirtualNic hostVirtualNic : hostVirtualNics) {
                        ipAddresses.add(hostVirtualNic.getSpec().getIp().getIpAddress());
                    }
                }
            }
        }
        return ipAddresses;
    }

    /**
     * Searches for all ip addresses of a virtual machine
     *
     * @param virtualMachine the virtual machine to query
     * @return the ip addresses of the virtual machine, the first one is the primary
     * @throws RemoteException
     */
    public TreeSet<String> getVirtualMachineIpAddresses(VirtualMachine virtualMachine) throws RemoteException {
        TreeSet<String> ipAddresses = new TreeSet<String>();

        // add the Ip address reported by VMware tools, this should be primary
        if (virtualMachine.getGuest().getIpAddress() != null)
            ipAddresses.add(virtualMachine.getGuest().getIpAddress());

        // if possible, iterate over all virtual networks networks and add interface Ip addresses
        if (virtualMachine.getGuest().getNet() != null) {
            for (GuestNicInfo guestNicInfo : virtualMachine.getGuest().getNet()) {
                if (guestNicInfo.getIpAddress() != null) {
                    for (String ipAddress : guestNicInfo.getIpAddress()) {
                        ipAddresses.add(ipAddress);
                    }
                }
            }
        }

        return ipAddresses;
    }

    /**
     * Searches for a managed entity by a given type.
     *
     * @param type the type string to search for
     * @return the list of managed entities found
     * @throws RemoteException
     */
    public ManagedEntity[] searchManagedEntities(String type) throws RemoteException {
        return (new InventoryNavigator(m_serviceInstance.getRootFolder())).searchManagedEntities(type);
    }

    /**
     * Return the major API version for this management server
     *
     * @return the major API version
     */
    public int getMajorApiVersion() {
        if (m_serviceInstance != null) {
            String apiVersion = m_serviceInstance.getAboutInfo().getApiVersion();

            String[] arr = apiVersion.split("\\.");

            if (arr.length > 1) {
                int apiMajorVersion = Integer.valueOf(arr[0]);

                if (apiMajorVersion < 4) {
                    apiMajorVersion = 3;
                }

                return apiMajorVersion;
            } else {
                logger.error("Cannot parse vCenter API version '{}'", apiVersion);

                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Returns the value of a given cim object and property.
     *
     * @param cimObject    the Cim object
     * @param propertyName the property's name
     * @return the value
     */
    public String getPropertyOfCimObject(CIMObject cimObject, String propertyName) {
        if (cimObject == null) {
            return null;
        } else {
            CIMProperty cimProperty = cimObject.getProperty(propertyName);
            if (cimProperty == null) {
                return null;
            } else {
                CIMValue cimValue = cimProperty.getValue();
                if (cimValue == null) {
                    return null;
                } else {
                    Object object = cimValue.getValue();
                    if (object == null) {
                        return null;
                    } else {
                        return object.toString();
                    }
                }
            }
        }
    }
}

