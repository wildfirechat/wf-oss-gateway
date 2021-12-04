/*
 * Copyright (c) 2012-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.server;

import com.xiaoleilu.loServer.LoServer;
import com.xiaoleilu.loServer.ServerSetting;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.action.UploadFileAction;
import io.moquette.BrokerConstants;
import io.moquette.server.config.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.DBUtil;

import java.io.File;
import java.io.IOException;

/**
 * Launch a configured version of the server.
 */
public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    public static void main(String[] args) throws IOException {
        final IConfig config = defaultConfig();

        initMediaServerConfig(config);
        DBUtil.init(config);
        UploadFileAction.USE_SM4 = Boolean.parseBoolean(config.getProperty("encrypt.use_sm4", "false"));

        int httpLocalPort = Integer.parseInt(config.getProperty(BrokerConstants.HTTP_LOCAL_PORT));
        final LoServer httpServer = new LoServer(httpLocalPort);
        try {
            httpServer.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::shutdown));
        System.out.println("Wildfire IM server start success...");
    }

    public static IConfig defaultConfig() {
        File defaultConfigurationFile = defaultConfigFile();
        LOG.info("Starting Moquette server. Configuration file path={}", defaultConfigurationFile.getAbsolutePath());
        IResourceLoader filesystemLoader = new FileResourceLoader(defaultConfigurationFile);
        return new ResourceLoaderConfig(filesystemLoader);
    }

    private static File defaultConfigFile() {
        String configPath = System.getProperty("wildfirechat.path", null);
        return new File(configPath, IConfig.DEFAULT_CONFIG);
    }

    private static void initMediaServerConfig(IConfig config) {
    	MediaServerConfig.MEDIA_ACCESS_KEY = config.getProperty(BrokerConstants.MEDIA_ACCESS_KEY, MediaServerConfig.MEDIA_ACCESS_KEY);
    	MediaServerConfig.MEDIA_SECRET_KEY = config.getProperty(BrokerConstants.MEDIA_SECRET_KEY, MediaServerConfig.MEDIA_SECRET_KEY);
    }
}
