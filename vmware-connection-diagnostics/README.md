client-cli
==========

This module contains the backup client command-line utility. It uses a
configuration file with to following contents:

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <local-backup-config>
        <backup-service-location>Location to the remote backup server</backup-service-location>
        <token>The security token</token>
        <database-host>The database host</database-host>
        <database-name>The database name</database-name>
        <database-username>The database username</database-username>
        <database-password>The database password</database-password>
        <database-port>The database port</database-port>
        <base-directory>The OpenNMS installation directory</base-directory>

        <!-- Directories to backup -->

        <directories>
            <directory>etc</directory>
            <directory>share</directory>
            <directory>dbdump</directory>
        </directories>

        <local-backup-directory>Local backup directory</local-backup-directory>
        <max-concurrent-uploads>8</max-concurrent-uploads>
        <pg-dump-location>Location of the pg_dump utility</pg-dump-location>
    </local-backup-config>


Building and running the client:
================================
Just invoke 'mvn install' to build the command line tool. Change to the module's target directory and
invoke the client with the '--help' argument to get an overview over its parameters:

    $ mvn install
    $ cd target
    $ java -jar client-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar --help

You can specify the '-v' or '-vv' option to get detailed debug or trace log output. To generate a
more-or-less working backup.xml configuration file for a given OpenNMS installation use the command 'configure'
and the options '--installdir'

    $ java -jar client-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar -vv configure --installdir /opt/opennms

This will create a working backup configuration file in your current working directory, but there are still
values to be set manually.

Now you can create a backup file with specifying the 'create' command:

    $ java -jar client-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar -vv create

You have now a file backup.<timestamp>.zip in your local backup directory. For now the upload isn't working, but
the command line option already exists. If the 'upload' command is given you have to specify the '--file' option
for the file to be uploaded:

    $ java -jar client-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar -vv upload --file backup.<Timestamp>.zip

You can also invoke all task together by invoking the command 'backup'. This will also issue the 'clean' command
which deletes the created backup file after uploading:

    $ java -jar client-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar -vv backup

