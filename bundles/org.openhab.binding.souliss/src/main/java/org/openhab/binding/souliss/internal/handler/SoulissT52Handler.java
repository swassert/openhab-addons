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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Thing;

/**
 * The {@link SoulissT52Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */
@NonNullByDefault
public class SoulissT52Handler extends SoulissT5nHandler {

    private float fVal = 0xF;

    public SoulissT52Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void setFloatValue(float valueOf) {
        super.setLastStatusStored();
        if (fVal != valueOf) {
            this.setState(QuantityType.valueOf(Float.toString(valueOf) + " °C"));
            fVal = valueOf;
        }
    }
}
