/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2014 The OpenNMS Group, Inc.
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
import org.apache.commons.codec.digest.DigestUtils;
import org.opennms.netmgt.collectd.vmware.vijava.VmwarePerformanceValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
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
        System.out.println("Reading virtual machines and ESX hosts from " + host + " with " + user + "/pass(SHA-256) " + DigestUtils.sha256Hex(pass) + "\n");

        // Initialize connection with vCenter credentials
        ViJavaConnectTest viJavaConnectTest = new ViJavaConnectTest(host, user, pass);

        // Try to establish the connection to vCenter
        try {
            System.out.print("Try to connect VMware vCenter " + host + " ... ");

            // Establish connection
            serviceInstance = viJavaConnectTest.connect();

            // Give some information to test if connection and credentials work
            System.out.println("SUCCESS\n");
            System.out.println("VMware API Type:         " + serviceInstance.getAboutInfo().apiType);
            System.out.println("VMware API Version:      " + serviceInstance.getAboutInfo().apiVersion + " build " + serviceInstance.getAboutInfo().build);
            System.out.println("VMware operating system: " + serviceInstance.getAboutInfo().getOsType() + "\n");

            // Give some information about VMware systems
            System.out.println("Collect Host Systems");
            System.out.println("-----------------------");
            iterateVmwareHostSystems(serviceInstance);

            System.out.println("\nCollect Virtual Machines");
            System.out.println("------------------------");
            iterateVmwareVirtualMachines(serviceInstance);

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
     *
     * @param serviceInstance {@link  com.vmware.vim25.mo.ServiceInstance} with established vCenter connection
     * @throws RemoteException
     */
    private static void iterateVmwareHostSystems(ServiceInstance serviceInstance) throws RemoteException {
        ManagedEntity[] vmwareHostSystems;
        // Search for system type on vCenter as ManagedEntity array
        vmwareHostSystems = new InventoryNavigator(serviceInstance.getRootFolder()).searchManagedEntities(VMWARE_HOSTSYSTEM);

        System.out.println("Host systems found    : " + vmwareHostSystems.length);

        // Display name for each virtual machine or host system
        for (ManagedEntity entity : vmwareHostSystems) {
            HostSystem hostSystem = (HostSystem) entity;
            System.out.println("  ├─ ESX name: " + hostSystem.getName());
            System.out.println("  ├─── Power state    : " + hostSystem.getRuntime().getPowerState());

            HostNetworkSystem hostNetworkSystem = hostSystem.getHostNetworkSystem();
            if (hostNetworkSystem != null) {
                HostNetworkInfo hostNetworkInfo = hostNetworkSystem.getNetworkInfo();

                HostVirtualNic[] hostVirtualNics = hostNetworkInfo.getConsoleVnic();
                if (hostVirtualNics != null) {
                    for (HostVirtualNic hostVirtualNic : hostVirtualNics) {
                        System.out.println("  ├─── Console VNIC IP: " + hostVirtualNic.getSpec().getIp().getIpAddress());
                    }
                } else {
                    System.out.println("  ├─── Console VNIC IP: not supported");
                    hostVirtualNics = hostNetworkInfo.getVnic();
                    if (hostVirtualNics != null) {
                        for (HostVirtualNic hostVirtualNic : hostVirtualNics) {
                            System.out.println("  ├─── Virtual NIC IP : " + hostVirtualNic.getSpec().getIp().getIpAddress());
                        }
                    } else {
                        System.out.println("  ├─── Virtual NIC IP : not supported");
                    }
                }
            } else {
                System.out.println("  ├─── Network info   : not supported");
            }

            for (Network network : hostSystem.getNetworks()) {
                System.out.println("  ├─── Network name   : " + network.getSummary().getName());
            }

            VmwarePerformanceValues vmwarePerformanceValues = queryPerformanceValues(hostSystem, serviceInstance);
            System.out.println("  ├─── Metric vCenter : " + vmwarePerformanceValues.getValue("rescpu.maxLimited1.latest"));

/*            for (String metric : vmwarePerformanceValues.getKeys()) {
                if (vmwarePerformanceValues.hasInstances(metric)) {
                    for (String instance : vmwarePerformanceValues.getInstances(metric)) {
                        System.out.println(metric + "[" + instance + "]=" + vmwarePerformanceValues.getValue(metric, instance));
                    }
                } else {
                    System.out.println(metric + "=" + vmwarePerformanceValues.getValue(metric));
                }
            }*/
        }
    }

    /**
     * Search on a vCenter for VirtualMachines
     *
     * @param serviceInstance {@link  com.vmware.vim25.mo.ServiceInstance} with established vCenter connection
     * @throws RemoteException
     */
    private static void iterateVmwareVirtualMachines(ServiceInstance serviceInstance) throws RemoteException {
        ManagedEntity[] vmwareVirtualMachines;
        // Search for system type on vCenter as ManagedEntity array
        vmwareVirtualMachines = new InventoryNavigator(serviceInstance.getRootFolder()).searchManagedEntities(VMWARE_VIRTUALMACHINE);

        System.out.println("Virtual machines found: " + vmwareVirtualMachines.length);

        // Display name for each virtual machine or host system
        for (ManagedEntity entity : vmwareVirtualMachines) {
            System.out.println("  ├─ VM name: " + entity.getName());
        }
    }

    /**
     * This method queries performance values for a given managed entity.
     *
     * @param managedEntity the managed entity to query
     * @return the perfomance values
     * @throws RemoteException
     */
    private static VmwarePerformanceValues queryPerformanceValues(ManagedEntity managedEntity, ServiceInstance serviceInstance) throws RemoteException {

        VmwarePerformanceValues vmwarePerformanceValues = new VmwarePerformanceValues();

        int refreshRate = getPerformanceManager(serviceInstance).queryPerfProviderSummary(managedEntity).getRefreshRate();

        PerfQuerySpec perfQuerySpec = new PerfQuerySpec();
        perfQuerySpec.setEntity(managedEntity.getMOR());
        perfQuerySpec.setMaxSample(Integer.valueOf(1));

        perfQuerySpec.setIntervalId(refreshRate);

        PerfEntityMetricBase[] perfEntityMetricBases = getPerformanceManager(serviceInstance).queryPerf(new PerfQuerySpec[]{perfQuerySpec});

        if (perfEntityMetricBases != null) {
            for (int i = 0; i < perfEntityMetricBases.length; i++) {
                PerfMetricSeries[] perfMetricSeries = ((PerfEntityMetric) perfEntityMetricBases[i]).getValue();

                for (int j = 0; perfMetricSeries != null && j < perfMetricSeries.length; j++) {

                    if (perfMetricSeries[j] instanceof PerfMetricIntSeries) {
                        long[] longs = ((PerfMetricIntSeries) perfMetricSeries[j]).getValue();

                        if (longs.length == 1) {

                            PerfCounterInfo perfCounterInfo = getPerfCounterInfoMap(serviceInstance).get(perfMetricSeries[j].getId().getCounterId());
                            String instance = perfMetricSeries[j].getId().getInstance();
                            String name = getHumanReadableName(perfCounterInfo);

                            if (instance != null && !"".equals(instance)) {
                                vmwarePerformanceValues.addValue(name, instance, longs[0]);
                            } else {
                                vmwarePerformanceValues.addValue(name, longs[0]);
                            }
                        }
                    }
                }
            }
        }

        return vmwarePerformanceValues;
    }

    /**
     * Retrieves the performance manager for this instance.
     *
     * @return the performance manager
     */
    private static PerformanceManager getPerformanceManager(ServiceInstance serviceInstance) {
        PerformanceManager performanceManager = null;

        if (performanceManager == null) {
            performanceManager = serviceInstance.getPerformanceManager();
        }

        return performanceManager;
    }

    /**
     * Generates a human-readable name for a performance counter.
     *
     * @param perfCounterInfo the perfomance counter info object
     * @return a string-representation of the performance counter's name
     */
    private static String getHumanReadableName(PerfCounterInfo perfCounterInfo) {
        return perfCounterInfo.getGroupInfo().getKey() + "." + perfCounterInfo.getNameInfo().getKey() + "." + perfCounterInfo.getRollupType().toString();
    }

    /**
     * This method retrieves the performance counters available.
     *
     * @return a map of performance counters
     */
    private static Map<Integer, PerfCounterInfo> getPerfCounterInfoMap(ServiceInstance serviceInstance) {
        Map<Integer, PerfCounterInfo> m_perfCounterInfoMap = null;

        if (m_perfCounterInfoMap == null) {
            m_perfCounterInfoMap = new HashMap<Integer, PerfCounterInfo>();

            PerfCounterInfo[] perfCounterInfos = getPerformanceManager(serviceInstance).getPerfCounter();

            for (PerfCounterInfo perfCounterInfo : perfCounterInfos) {
                m_perfCounterInfoMap.put(perfCounterInfo.getKey(), perfCounterInfo);
            }
        }
        return m_perfCounterInfoMap;
    }
}
