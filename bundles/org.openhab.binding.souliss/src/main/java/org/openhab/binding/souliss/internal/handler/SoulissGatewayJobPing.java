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
package org.openhab.binding.souliss.internal.handler;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.protocol.CommonCommands;
import org.openhab.core.thing.Bridge;

/**
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */

@NonNullByDefault
public class SoulissGatewayJobPing implements Runnable {

    private @Nullable SoulissGatewayHandler gwHandler;

    public SoulissGatewayJobPing(Bridge bridge) {
        var bridgeHandler = bridge.getHandler();
        if (bridgeHandler != null) {
            gwHandler = (SoulissGatewayHandler) bridgeHandler;
        }
    }

    @Override
    public void run() {
        SoulissGatewayHandler localGwHandler = this.gwHandler;
        if (localGwHandler != null) {
            sendPing();
            localGwHandler.pingSent();
        }
    }

    private void sendPing() {
        // sending ping packet
        var localGwHandler = this.gwHandler;
        if (localGwHandler != null && localGwHandler.getGwConfig().gatewayLanAddress.length() > 0) {
            ArrayList<Byte> macacoFrame = CommonCommands.buildPingFrame(localGwHandler.getGwConfig());
            localGwHandler.queueToDispatcher(macacoFrame);
            // ping packet sent
        }
    }
}
