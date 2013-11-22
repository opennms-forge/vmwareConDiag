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

import com.vmware.vim25.VimPortType;
import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.ws.WSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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
    private final Logger logger = LoggerFactory.getLogger("vmwareConDiag." + ViJavaConnectTest.class.getName());

    /**
     * vCenter IP or FQDN
     */
    private String hostname = null;

    /**
     * vCenter username for login
     */
    private String username = null;

    /**
     * vCenter user password for login
     */
    private String password = null;

    /**
     * vCenter service instance with established connection
     */
    private ServiceInstance serviceInstance = null;

    /**
     * Constructor for creating a instance for a given server and credentials.
     *
     * @param hostname the vCenter's hostname
     * @param username the username
     * @param password the password
     */
    public ViJavaConnectTest(String hostname, String username, String password) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
    }

    /**
     * Connects to the server.
     *
     * @throws java.net.MalformedURLException
     * @throws java.rmi.RemoteException
     */
    public ServiceInstance connect() throws MalformedURLException, RemoteException {
        relax();

        return new ServiceInstance(new URL("https://" + hostname + "/sdk"), username, password);
    }

    /**
     * Sets the timeout for server connections.
     *
     * @param timeout the timeout to be used for connecting
     * @return true, if the operation was successful
     */
    public boolean setTimeout(int timeout) {
        if (serviceInstance != null) {
            ServerConnection serverConnection = serviceInstance.getServerConnection();
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
        if (serviceInstance == null) {
            // not connected
            return;
        } else {
            ServerConnection serverConnection = serviceInstance.getServerConnection();

            if (serverConnection == null) {
                // not connected
                return;
            } else {
                serviceInstance.getServerConnection().logout();
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
}

