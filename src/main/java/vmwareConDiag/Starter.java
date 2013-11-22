/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenNMS(R). If not, see:
 * http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org> http://www.opennms.org/ http://www.opennms.com/
 * *****************************************************************************
 */
package vmwareConDiag;

import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * This starter provides parsing and handling command line parameter
 *
 * @author ronny@opennms.org
 */
public class Starter {

    /**
     * Config file for vCenter host and credentials
     */
    private static final String CONFIG_PROPERTIES = "config.properties";

    /**
     * Host property with vCenter IP or FQDN
     */
    private static final String PROP_HOST = "host";

    /**
     * User property for vCenter login
     */
    private static final String PROP_USER = "user";

    /**
     * Password property with vCenter user password
     */
    private static final String PROP_PASS = "pass";

    /**
     * Constant for empty string
     */
    private static final String EMPTY_STRING = "";

    /**
     * vCenter query string for host systems
     */
    private static final String VMWARE_HOSTSYSTEM = "HostSystem";

    /**
     * vCenter query string for virtual machines
     */
    private static final String VMWARE_VIRTUALMACHINE = "VirtualMachine";

    /**
     * Initialize logging
     */
    private static Logger logger = LoggerFactory.getLogger(Starter.class);

    /**
     * Main method to test if connection to a vCenter can established. It loads also a config.properties with
     * user credentials for vCenter to establish the connection using the ViJavaConnectionTest.
     * After established connection some "about information" from the vCenter are received and the connection
     * will be closed.
     *
     * @param args - No args evaluated
     */
    public static void main(String[] args) {
        // Init vCenter credentials
        String host = EMPTY_STRING;
        String user = EMPTY_STRING;
        String pass = EMPTY_STRING;

        // Used for read in config.properties
        Properties properties = new Properties();

        // vCenter service instance used if a vCenter connection could be established
        ServiceInstance serviceInstance;

        // Load config.properties from current directory
        try {
            properties.load(new FileInputStream(CONFIG_PROPERTIES));
            host = properties.getProperty(PROP_HOST);
            user = properties.getProperty(PROP_USER);
            pass = properties.getProperty(PROP_PASS);
        } catch (IOException e) {
            logger.error("Couldn't read configuration property ['{}']. Error message: '{}'", CONFIG_PROPERTIES, e.getMessage());
            logger.debug("Stack trace: '{}'", CONFIG_PROPERTIES, e.getMessage(), e.getStackTrace());

            // No vCenter credentials --> Error exit
            System.exit(1);
        }

        // Could read config.properties.
        logger.info("Using host:'{}', user:'{}', pass (SHA-256):'{}' for connection", host, user, DigestUtils.sha256Hex(pass));

        // Initialize connection with vCenter credentials
        ViJavaConnectTest viJavaConnectTest = new ViJavaConnectTest(host, user, pass);

        // Try to establish the connection to vCenter
        try {
            logger.info("Try to connect: '{}'", host);

            // Establish connection
            serviceInstance = viJavaConnectTest.connect();

            // Give some information to test if connection and credentials work
            logger.info("Connect successfull");
            logger.info("VMware API Type:         '{}'", serviceInstance.getAboutInfo().apiType);
            logger.info("VMware API Version:      '{}' build '{}'", serviceInstance.getAboutInfo().apiVersion, serviceInstance.getAboutInfo().build);
            logger.info("VMware operating system: '{}'", serviceInstance.getAboutInfo().getOsType());

            // Give some information about VMware systems
            iterateVmwareSystems(serviceInstance, VMWARE_HOSTSYSTEM);
            iterateVmwareSystems(serviceInstance, VMWARE_VIRTUALMACHINE);

        } catch (MalformedURLException e) {
            logger.error("Malformed URL exception occurred. Error message: '{}'", e.getMessage());
            logger.debug("Stack trace: '{}'", e.getStackTrace());

            // Connection not possible --> Error exit
            System.exit(1);
        } catch (RemoteException e) {
            logger.error("Remote exception occurred. Error message: '{}'", e.getMessage());
            logger.debug("Stack trace: '{}'", e.getStackTrace());

            // Connection not possible --> Error exit
            System.exit(1);
        }

        // Disconnect vCenter connection
        viJavaConnectTest.disconnect();
    }

    /**
     * Search on a vCenter for a specific system type i.e. VirtualMachine or Host System.
     * @param serviceInstance {@link  com.vmware.vim25.mo.ServiceInstance} with established vCenter connection
     * @param systemType Search type for managed entity, i.e. VirtualMachine or Host System as {@link java.lang.String}
     * @throws RemoteException
     */
    private static void iterateVmwareSystems(ServiceInstance serviceInstance, String systemType) throws RemoteException {
        ManagedEntity[] vmwareSystems;
        // Search for system type on vCenter as ManagedEntity array
        vmwareSystems = new InventoryNavigator(serviceInstance.getRootFolder()).searchManagedEntities(systemType);

        logger.info("Systems for type '{}' found: '{}'", systemType, vmwareSystems.length);
        logger.info("Show system names:");

        // Display name for each virtual machine or host system
        for (ManagedEntity entity : vmwareSystems) {
            logger.info("-> Type: '{}' :: Name: '{}'", systemType, entity.getName());
        }
    }
}
