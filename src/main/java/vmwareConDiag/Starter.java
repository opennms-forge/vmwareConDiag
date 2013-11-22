/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
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

    private static Logger logger = LoggerFactory.getLogger(Starter.class);

    public static void main(String[] args) {
        String host = "";
        String user = "";
        String pass = "";

        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream("config.properties"));
            host = properties.getProperty("host");
            user = properties.getProperty("user");
            pass = properties.getProperty("pass");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            System.exit(1);
        }

        logger.info("Using host:'{}', user:'{}', pass (SHA-256):'{}' for connection", host, user, DigestUtils.sha256Hex(pass));
        ServiceInstance serviceInstance;

        ViJavaConnectTest viJavaConnectTest = new ViJavaConnectTest(host,user,pass);

        try {
            logger.info("Try to connect: '{}'", host);
            serviceInstance = viJavaConnectTest.connect();
            logger.info("Connect successfull");
            logger.info("VMware API Type:         '{}'",serviceInstance.getAboutInfo().apiType);
            logger.info("VMware API Version:      '{}' build '{}'", serviceInstance.getAboutInfo().apiVersion, serviceInstance.getAboutInfo().build);
            logger.info("VMware operating system: '{}'", serviceInstance.getAboutInfo().getOsType());

        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (RemoteException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        viJavaConnectTest.disconnect();
    }
}
