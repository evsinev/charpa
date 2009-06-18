package com.googlecode.charpa.service;

import java.util.Map;
import java.util.List;
import java.io.File;

/**
 * SSH service for executing commands
 */
public interface ISecurityShellService {

    /**
     * Executes command on remote server by ssh
     *
     * @param aHostname              hostname
     * @param aPort                  ssh port
     * @param aUsername              username
     * @param aPassword              password
     * @param aLocalFile             local file
     * @param aRemoteDir             remote dir
     * @throws SecurityShellException on ssh error
     */
    void copyFileToRemoteHost(String aHostname
            , int aPort
            , String aUsername
            , String aPassword
            , File aLocalFile
            , String aRemoteDir)
            throws SecurityShellException;

    /**
     * Executes command on remote server by ssh
     *
     * @param aHostname              hostname
     * @param aPort                  ssh port
     * @param aUsername              username
     * @param aPassword              password
     * @param aEnv                   environment variables
     * @param aCommand               command to execute
     * @param aArguments             command's arguments
     * @param aCommandOutputListener output listener
     * @throws SecurityShellException on ssh error
     */
    void executeCommand(String aHostname
            , int aPort
            , String aUsername
            , String aPassword
            , Map<String, String> aEnv
            , String aCommand
            , List<String> aArguments
            , ICommandOutputListener aCommandOutputListener
    ) throws SecurityShellException;
}