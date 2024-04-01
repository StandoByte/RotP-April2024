package com.github.standobyte.jrpg.network.packets.server;

import java.util.HashMap;
import java.util.function.Supplier;

import com.github.standobyte.jojo.util.general.Vector2i;
import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class ChunkLevelPacket {
    private final ChunkPos chunkPos;
    private final int min;
    private final int max;
    
    public ChunkLevelPacket(ChunkPos chunkPos, int min, int max) {
        this.chunkPos = chunkPos;
        this.min = min;
        this.max = max;
    }
    
    
    public static class Handler implements IModPacketHandler<ChunkLevelPacket> {

        @Override
        public void encode(ChunkLevelPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.chunkPos.x);
            buf.writeInt(msg.chunkPos.z);
            buf.writeVarInt(msg.min);
            buf.writeVarInt(msg.max);
        }

        @Override
        public ChunkLevelPacket decode(PacketBuffer buf) {
            ChunkPos chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
            int min = buf.readVarInt();
            int max = buf.readVarInt();
            return new ChunkLevelPacket(chunkPos, min, max);
        }

        @Override
        public void handle(ChunkLevelPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ClientStuff.CHUNK_LEVELS.computeIfAbsent(ClientUtil.getClientWorld().dimension(), d -> new HashMap<>())
            .put(msg.chunkPos, new Vector2i(msg.min, msg.max));
        }

        @Override
        public Class<ChunkLevelPacket> getPacketClass() {
            return ChunkLevelPacket.class;
        }
    }
}
