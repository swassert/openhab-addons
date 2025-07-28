/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.souliss.internal.discovery;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.handler.SoulissGatewayHandler;
import org.openhab.binding.souliss.internal.protocol.CommonCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */
@NonNullByDefault
public class SoulissDiscoverJob implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(SoulissDiscoverJob.class);

    private int resendCounter = 0;

    private @Nullable SoulissGatewayHandler gwHandler;

    public SoulissDiscoverJob(@Nullable SoulissGatewayHandler soulissGwHandler) {
        this.gwHandler = soulissGwHandler;
    }

    @Override
    public void run() {
        var localGwHandler = this.gwHandler;
        if (localGwHandler != null) {
            ArrayList<Byte> macacoFrame = CommonCommands.buildDBStructFrame(localGwHandler.getGwConfig());
            localGwHandler.queueToDispatcher(macacoFrame);

            logger.debug("Sending request to gateway for souliss network - Counter={}", resendCounter);
        } else {
            logger.debug("Gateway null - Skipped");
        }
        resendCounter++;
    }
}
