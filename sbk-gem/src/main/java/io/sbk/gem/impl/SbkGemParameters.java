/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem.impl;

import io.sbk.api.HelpException;
import io.sbk.api.impl.SbkDriversParameters;
import io.sbk.gem.GemConfig;
import io.sbk.gem.GemParameterOptions;
import io.sbk.gem.SshConnection;
import io.sbk.perl.PerlConfig;
import io.sbk.system.Printer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class SbkGemParameters extends SbkDriversParameters implements GemParameterOptions {

    final private GemConfig config;

    @Getter
    final private int timeoutMS;

    @Getter
    final private String[] optionsArgs;

    @Getter
    private String[] parsedArgs;

    @Getter
    private SshConnection[] connections;

    @Getter
    private String localHost;

    @Getter
    private int ramPort;


    public SbkGemParameters(String name, String[] drivers, GemConfig config, int ramport) {
        super(name, GemConfig.DESC, drivers);
        this.config = config;
        this.timeoutMS = config.timeoutSeconds * PerlConfig.MS_PER_SEC;
        this.ramPort = ramport;
        try {
            this.localHost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Printer.log.error(ex.toString());
            this.localHost = GemConfig.LOCAL_HOST;
        }
        addOption("nodes", true, "remote hostnames separated by ',' , default: "+config.nodes);
        addOption("gemuser", true, "ssh user name of the remote hosts, default: " + config.gemuser);
        addOption("gempass", true, "ssh user password of the remote hosts, default: " + config.gempass);
        addOption("gemport", true, "ssh port of the remote hosts, default: " + config.gemport);
        addOption("sbkdir", true, "directory path of sbk application, default: " + config.sbkDir);
        addOption("sbkcommand", true,
                "remote sbk command; command path is relative to 'sbkdir', default: " + config.sbkCommand);
        addOption("copy", true, "Copy the SBK package to remote hosts; default: "+ config.copy);
        addOption("localhost", true, "this local RAM host name, default: " + localHost);
        addOption("ramport", true, "RAM port number; default: " + ramPort);
        this.optionsArgs = new String[]{"-nodes", "-gemuser", "-gempass", "-gemport", "-sbkdir", "-sbkcommand",
                "-copy", "-localhost", "ramport"};
        this.parsedArgs = null;
    }

    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        super.parseArgs(args);
        final String nodeString = getOptionValue("nodes", config.nodes);
        String[] nodes = nodeString.split(",");
        config.gemuser = getOptionValue("gemuser", config.gemuser);
        config.gempass = getOptionValue("gempass", config.gempass);
        config.gemport = Integer.parseInt(getOptionValue("gemport", Integer.toString(config.gemport)));
        config.sbkDir = getOptionValue("sbkdir", config.sbkDir);
        config.sbkCommand = getOptionValue("sbkcommand", config.sbkCommand);
        localHost = getOptionValue("localhost", localHost);
        ramPort = Integer.parseInt(getOptionValue("ramport", Integer.toString(ramPort)));
        config.copy = Boolean.parseBoolean(getOptionValue("copy", Boolean.toString(config.copy)));

        parsedArgs = new String[]{"-nodes", nodeString, "-gemuser", config.sbkCommand, "-gempass", config.gempass, "-gemport",
                Integer.toString(config.gemport), "-sbkdir", config.sbkDir, "-sbkcommand", config.sbkCommand,
                "-copy", Boolean.toString(config.copy), "-localhost", localHost, "-ramport", Integer.toString(ramPort) };

        connections = new SshConnection[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            connections[i] = new SshConnection(nodes[i], config.gemuser, config.gempass, config.gemport,
                    config.remoteDir);
        }

        if (StringUtils.isEmpty(config.sbkDir)) {
            String errMsg = "The SBK application directory not supplied!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isDirectory(Paths.get(config.sbkDir))) {
            String errMsg = "The SBK application directory: "+config.sbkDir +" not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (StringUtils.isEmpty(config.sbkCommand)) {
            String errMsg = "The SBK application/command not supplied!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        final String sbkFullCommand = config.sbkDir + File.separator + config.sbkCommand;
        Path sbkCommandPath = Paths.get(sbkFullCommand);

        if (!Files.exists(sbkCommandPath)) {
            String errMsg = "The sbk executable command: "+sbkFullCommand+" not found!";
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        if (!Files.isExecutable(sbkCommandPath)) {
            String errMsg = "The executable permissions are not found for command: "+sbkFullCommand;
            Printer.log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

    }

    @Override
    public String getSbkDir() {
        return config.sbkDir;
    }

    @Override
    public String getSbkCommand() {
        return config.sbkCommand;
    }

    @Override
    public boolean isCopy() {
        return config.copy;
    }
}
