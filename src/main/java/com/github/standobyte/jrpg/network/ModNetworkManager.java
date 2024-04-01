package com.github.standobyte.jrpg.network;

import java.util.Optional;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;
import com.github.standobyte.jrpg.network.packets.client.ClLookVecPacket;
import com.github.standobyte.jrpg.network.packets.client.ClStandUserChoisePacket;
import com.github.standobyte.jrpg.network.packets.client.ClUseActionPacket;
import com.github.standobyte.jrpg.network.packets.server.BattleEndPacket;
import com.github.standobyte.jrpg.network.packets.server.BattleStartPacket;
import com.github.standobyte.jrpg.network.packets.server.ChunkLevelPacket;
import com.github.standobyte.jrpg.network.packets.server.GiveChoisePacket;
import com.github.standobyte.jrpg.network.packets.server.NextTurnPacket;
import com.github.standobyte.jrpg.network.packets.server.PartyPacket;
import com.github.standobyte.jrpg.network.packets.server.RPGDataPacket;
import com.github.standobyte.jrpg.network.packets.server.StaminaPointsPacket;
import com.github.standobyte.jrpg.network.packets.server.TurnBasedEffectsPacket;
import com.github.standobyte.jrpg.network.packets.server.UIMessagePacket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class ModNetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel serverChannel;
    private static SimpleChannel clientChannel;
    private static int packetIndex = 0;

    public static void init() {
        serverChannel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ModMain.RES_NAMESPACE, "server_channel"))
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .simpleChannel();
        clientChannel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ModMain.RES_NAMESPACE, "client_channel"))
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .simpleChannel();
        
        packetIndex = 0;
        registerMessage(serverChannel, new BattleStartPacket.Handler(),              Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new BattleEndPacket.Handler(),                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new PartyPacket.Handler(),                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new NextTurnPacket.Handler(),                 Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new RPGDataPacket.Handler(),                  Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new TurnBasedEffectsPacket.Handler(),         Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new ChunkLevelPacket.Handler(),               Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new UIMessagePacket.Handler(),                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new StaminaPointsPacket.Handler(),            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        registerMessage(serverChannel, new GiveChoisePacket.Handler(),               Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        packetIndex = 0;
        registerMessage(clientChannel, new ClUseActionPacket.Handler(),              Optional.of(NetworkDirection.PLAY_TO_SERVER));
        registerMessage(clientChannel, new ClLookVecPacket.Handler(),                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        registerMessage(clientChannel, new ClStandUserChoisePacket.Handler(),        Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    
    private static <MSG> void registerMessage(SimpleChannel channel, IModPacketHandler<MSG> handler, Optional<NetworkDirection> networkDirection) {
        if (packetIndex > 127) {
            throw new IllegalStateException("Too many packets (> 127) registered for a single channel!");
        }
        channel.registerMessage(packetIndex++, handler.getPacketClass(), handler::encode, handler::decode, handler::enqueueHandleSetHandled, networkDirection);
    }
    
    public static void sendToServer(Object msg) {
        clientChannel.sendToServer(msg);
    }

    public static void sendToClient(Object msg, ServerPlayerEntity player) {
        if (!(player instanceof FakePlayer)) {
            serverChannel.send(PacketDistributor.PLAYER.with(() -> player), msg);
        }
    }

    public static void sendToClientsTracking(Object msg, Entity entity) {
        serverChannel.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    public static void sendToClientsTrackingAndSelf(Object msg, Entity entity) {
        serverChannel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

}
