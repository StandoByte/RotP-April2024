package com.github.standobyte.jrpg.network.packets.server;

import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.RpgMessages;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

public class UIMessagePacket {
    private final ITextComponent message;
    
    public UIMessagePacket(ITextComponent message) {
        this.message = message;
    }
    
    
    public static class Handler implements IModPacketHandler<UIMessagePacket> {

        @Override
        public void encode(UIMessagePacket msg, PacketBuffer buf) {
            buf.writeComponent(msg.message);
        }

        @Override
        public UIMessagePacket decode(PacketBuffer buf) {
            return new UIMessagePacket(buf.readComponent());
        }

        @Override
        public void handle(UIMessagePacket msg, Supplier<NetworkEvent.Context> ctx) {
            RpgMessages.addMessage(msg.message);
        }

        @Override
        public Class<UIMessagePacket> getPacketClass() {
            return UIMessagePacket.class;
        }
    }
}
