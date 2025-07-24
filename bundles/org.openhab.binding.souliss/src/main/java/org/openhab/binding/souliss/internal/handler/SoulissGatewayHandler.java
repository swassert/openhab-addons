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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.SoulissBindingConstants;
import org.openhab.binding.souliss.internal.SoulissUDPConstants;
import org.openhab.binding.souliss.internal.config.GatewayConfig;
import org.openhab.binding.souliss.internal.discovery.DiscoverResult;
import org.openhab.binding.souliss.internal.discovery.SoulissGatewayDiscovery;
import org.openhab.binding.souliss.internal.protocol.CommonCommands;
import org.openhab.binding.souliss.internal.protocol.SendDispatcherRunnable;
import org.openhab.binding.souliss.internal.protocol.UDPListenDiscoverRunnable;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SoulissGatewayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */
@NonNullByDefault
public class SoulissGatewayHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(SoulissGatewayHandler.class);

    private @Nullable ExecutorService udpExecutorService;

    private @Nullable Future<?> udpListenerJob;
    private @Nullable ScheduledFuture<?> pingScheduler;
    private @Nullable ScheduledFuture<?> subscriptionScheduler;
    private @Nullable ScheduledFuture<?> healthScheduler;

    boolean bGatewayDetected = false;

    private @Nullable SoulissGatewayDiscovery discoveryService;

    public @Nullable DiscoverResult discoverResult = null;

    public boolean thereIsAThingDetection = true;

    private Bridge bridge;

    private int nodes = 0;
    private int maxTypicalXnode = 24;
    private int countPingKo = 0;

    private GatewayConfig gwConfig = new GatewayConfig();

    private @Nullable SendDispatcherRunnable soulissSendDispatcherRunnable;

    public GatewayConfig getGwConfig() {
        return gwConfig;
    }

    public SoulissGatewayHandler(Bridge br) {
        super(br);
        bridge = br;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // do nothing
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(SoulissGatewayDiscovery.class);
    }

    @Override
    public void initialize() {
        gwConfig = getConfigAs(GatewayConfig.class);

        logger.debug("Starting UDP server on Souliss Default Port for Topics (Publish&Subcribe)");

        // new runnable udp listener
        var udpServerDefaultPortRunnableClass = new UDPListenDiscoverRunnable(this.bridge, this.discoverResult);
        // and exec on thread
        var localUdpListenerJob = this.udpListenerJob;
        if (localUdpListenerJob == null || localUdpListenerJob.isCancelled()) {
            var localUdpExecutorService = this.udpExecutorService;
            localUdpExecutorService = Executors
                    .newSingleThreadExecutor(new NamedThreadFactory(getThing().getUID().getAsString()));
            localUdpExecutorService.submit(udpServerDefaultPortRunnableClass);
        }

        soulissSendDispatcherRunnable = new SendDispatcherRunnable(this.bridge);
        scheduler.scheduleWithFixedDelay(soulissSendDispatcherRunnable, 15,
                SoulissBindingConstants.SEND_DISPATCHER_MIN_DELAY_CYCLE_IN_MILLIS, TimeUnit.MILLISECONDS);

        // JOB PING
        var soulissGatewayJobPingRunnable = new SoulissGatewayJobPing(this.bridge);
        pingScheduler = scheduler.scheduleWithFixedDelay(soulissGatewayJobPingRunnable, 2, this.gwConfig.pingInterval,
                TimeUnit.SECONDS);
        // JOB SUBSCRIPTION
        var soulissGatewayJobSubscriptionRunnable = new SoulissGatewayJobSubscription(bridge);
        subscriptionScheduler = scheduler.scheduleWithFixedDelay(soulissGatewayJobSubscriptionRunnable, 5,
                this.gwConfig.subscriptionInterval, TimeUnit.SECONDS);

        // JOB HEALTH OF NODES
        var soulissGatewayJobHealthyRunnable = new SoulissGatewayJobHealthy(this.bridge);
        healthScheduler = scheduler.scheduleWithFixedDelay(soulissGatewayJobHealthyRunnable, 5,
                this.gwConfig.healthyInterval, TimeUnit.SECONDS);
    }

    public void dbStructAnswerReceived() {
        ArrayList<Byte> macacoFrame = CommonCommands.buildTypicalRequestFrame(this.gwConfig, nodes);
        queueToDispatcher(macacoFrame);
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    public int getNodes() {
        var maxNode = 0;
        for (Thing thing : getThing().getThings()) {
            if (thing.getThingTypeUID().equals(SoulissBindingConstants.TOPICS_THING_TYPE)) {
                continue;
            }
            var cfg = thing.getConfiguration();
            var props = cfg.getProperties();
            var pNode = props.get("node");
            if (pNode != null) {
                var thingNode = Integer.parseInt(pNode.toString());

                if (thingNode > maxNode) {
                    maxNode = thingNode;
                }
            }
            // at the end the length of the list will be equal to the number of present nodes
        }
        return maxNode + 1;
    }

    public void setMaxTypicalXnode(int maxTypicalXnode) {
        this.maxTypicalXnode = maxTypicalXnode;
    }

    public int getMaxTypicalXnode() {
        return maxTypicalXnode;
    }

    /**
     * The {@link gatewayDetected} is used to notify that UDPServer decoded a Ping Response from gateway
     */

    public void gatewayDetected() {
        updateStatus(ThingStatus.ONLINE);
        // reset counter
        countPingKo = 0;
    }

    public void pingSent() {
        if (++countPingKo > 3) {
            var bridgeHandler = bridge.getHandler();
            if (bridgeHandler != null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Gateway " + bridgeHandler.getThing().getUID() + " do not respond to " + countPingKo + " ping");
            }
        }
    }

    public void setThereIsAThingDetection() {
        thereIsAThingDetection = true;
    }

    public void resetThereIsAThingDetection() {
        thereIsAThingDetection = false;
    }

    public @Nullable SoulissGatewayDiscovery getDiscoveryService() {
        return this.discoveryService;
    }

    public void setDiscoveryService(SoulissGatewayDiscovery discoveryService) {
        this.discoveryService = discoveryService;
    }

    /*
     * Queue command to Dispatcher (for securesend retransmission)
     */
    public final void queueToDispatcher(ArrayList<Byte> macacoFrame) {
        ArrayList<Byte> buf = CommonCommands.buildVNetFrame(macacoFrame, gwConfig.gatewayLanAddress,
                (byte) gwConfig.userIndex, (byte) gwConfig.nodeIndex);
        byte[] merd = CommonCommands.toByteArray(buf);

        InetAddress serverAddr;
        try {
            serverAddr = gwConfig.gatewayWanAddress.isEmpty() ? InetAddress.getByName(gwConfig.gatewayLanAddress)
                    : InetAddress.getByName(gwConfig.gatewayWanAddress);
            var packet = new DatagramPacket(merd, merd.length, serverAddr,
                    SoulissUDPConstants.SOULISS_GATEWAY_DEFAULT_PORT);
            var localSendDispatcher = this.soulissSendDispatcherRunnable;
            if (localSendDispatcher != null) {
                localSendDispatcher.put(packet, logger);
            }
        } catch (IOException e) {
            logger.warn("Error: {} ", e.getMessage());
        }
    }

    @Override
    public void dispose() {
        var localPingScheduler = this.pingScheduler;
        if (localPingScheduler != null) {
            localPingScheduler.cancel(true);
        }
        var localSubscriptionScheduler = this.subscriptionScheduler;
        if (localSubscriptionScheduler != null) {
            localSubscriptionScheduler.cancel(true);
        }
        var localHealthScheduler = this.healthScheduler;
        if (localHealthScheduler != null) {
            localHealthScheduler.cancel(true);
        }
        var localUdpListenerJob = this.udpListenerJob;
        if (localUdpListenerJob != null) {
            localUdpListenerJob.cancel(true);
        }
        var localUdpExecutorService = this.udpExecutorService;
        if (localUdpExecutorService != null) {
            localUdpExecutorService.shutdownNow();
        }

        super.dispose();
    }

    public void setBridgeStatus(boolean isOnline) {
        logger.debug("setBridgeStatus(): Setting Bridge to {}", isOnline ? ThingStatus.ONLINE : ThingStatus.OFFLINE);

        updateStatus(isOnline ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }
}
