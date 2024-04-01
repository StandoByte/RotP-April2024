package com.github.standobyte.jrpg.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.github.standobyte.jojo.client.InputHandler;
import com.github.standobyte.jojo.util.general.Vector2i;
import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.client.battleui.BattleScreen;
import com.github.standobyte.jrpg.client.battleui.range.MovementRangeCircle;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.party.Party;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovementInput;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.ClickInputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = ModMain.MOD_ID, value = Dist.CLIENT)
public class ClientStuff {
    @Nullable private static Battle currentBattle;
    public static Optional<Party> currentParty = Optional.empty();
    
    public static final Map<RegistryKey<World>, Map<ChunkPos, Vector2i>> CHUNK_LEVELS = new HashMap<>();
    // on the client, a chunk capability is only created for the chunk the player is currently in... or smth
    // point is, it doesn't fucking work
    
    @Nullable public static MovementRangeCircle movementRange;
    
    @Nullable
    public static Battle getCurrentBattle() {
        return currentBattle;
    }
    
    public static KeyBinding walkingSwitch;
    
    public static void initKeys() {
        ClientRegistry.registerKeyBinding(walkingSwitch = new KeyBinding(ModMain.MOD_ID + ".key.toggle_walking", GLFW.GLFW_KEY_Y, InputHandler.MAIN_CATEGORY));
    }
    
    public static void setBattle(@Nullable Battle battle) {
        currentBattle = battle;
        Minecraft mc = Minecraft.getInstance();
        if (battle != null) {
            BattleScreen.instance = new BattleScreen(battle);
            mc.setScreen(BattleScreen.instance);
        }
        else {
            if (BattleScreen.instance != null) {
                BattleScreen.instance = null;
                mc.setScreen(null);
            }
            if (movementRange != null) {
                MovementRangeCircle.remove();
            }
        }
    }
    
    @SubscribeEvent
    public static void clientTick(ClientTickEvent event) {
        switch (event.phase) {
        case START:
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                currentParty = Optional.empty();
            }
            
            if (currentBattle != null) {
                if (currentBattle.world == mc.level) {
                    currentBattle.tickBattle();
                    if (!BattleScreen.instance.isActive()) {
                        BattleScreen.instance.tick();
                    }
                }
                else {
                    setBattle(null);
                }
            }
            
            if (movementRange != null) {
                movementRange.tick();
            }
            
            break;
        case END:
            break;
        }
    }
    
    @SubscribeEvent
    public static void input(InputUpdateEvent event) {
        if (currentBattle != null) {
            MovementInput movementInput = event.getMovementInput();
            if (currentBattle.getCurrentTurnEntity() != event.getPlayer()) {
                movementInput.forwardImpulse = 0;
                movementInput.leftImpulse = 0;
                movementInput.jumping = false;
            }
            else if (movementRange != null) {
                Vector2f inputVec = event.getMovementInput().getMoveVector();
                inputVec = movementRange.limitMovement(event.getPlayer(), inputVec);
                movementInput.leftImpulse = inputVec.x;
                movementInput.forwardImpulse = inputVec.y;
            }
        }
    }
    
    
    
    public static Predicate<ItemStack> allowedItem = null;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouseClick(ClickInputEvent event) {
        if (ClientStuff.currentBattle != null) {
            if (allowedItem != null) {
                boolean canuse = allowedItem.test(ClientUtil.getClientPlayer().getItemInHand(event.getHand()));
                if (canuse) {
                    return;
                }
            }
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
    
    
}
