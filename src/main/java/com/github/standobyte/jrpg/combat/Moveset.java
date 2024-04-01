package com.github.standobyte.jrpg.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.combat.action.ai.AiGoalWrappingAction;
import com.github.standobyte.jrpg.event.JrpgTurnMovesetEvent;
import com.github.standobyte.jrpg.init.ActionType;
import com.github.standobyte.jrpg.init.ModCombatActions;
import com.github.standobyte.jrpg.init.StandRPGActions;
import com.github.standobyte.jrpg.network.NetworkUtil;
import com.github.standobyte.jrpg.stats.EntityRPGData;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;

public class Moveset {
    public final Category rootCategory;
    public final LivingEntity entity;
    public final Battle battle;
    private final Map<ResourceLocation, CombatAction> allActionsMap = new HashMap<>();
    
    public Moveset(Category root, LivingEntity entity, Battle battle) {
        this.rootCategory = root;
        this.entity = entity;
        this.battle = battle;
        fillActionsMap(root);
    }
    
    private void fillActionsMap(Category category) {
        category.elementsOrdered.forEach(element -> element.either(
                action -> this.allActionsMap.put(action.actionTypeId, action), 
                this::fillActionsMap));
    }
    
    public CombatAction getActionFromId(ResourceLocation actionId) {
        return allActionsMap.get(actionId);
    }
    
    public Iterable<CombatAction> getAllActions() {
        return allActionsMap.values();
    }
    
    
    public static Moveset createNewTurnMoveset(LivingEntity entity, Battle battle) {
        Moveset.Builder moveset = new Moveset.Builder(entity, battle);
        Moveset.Builder combat = moveset.subcategoryStart(new TranslationTextComponent("category.jrpg.abilities"));
        
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (player.isCreative() && player.hasPermissions(2) || "Dev".equals(player.getDisplayName().getString()) /* for debug purposes*/) {
                combat.addAction(ModCombatActions.CREATIVE_KILL);
                combat.addAction(ModCombatActions.CREATIVE_KILL_ALL);
            }
            
            combat.addAction(ModCombatActions.PLAYER_MELEE_ATTACK);
        }
        
        ItemStack mainHandItem = entity.getMainHandItem();
        ItemStack offHandItem = entity.getOffhandItem();
        
        if (entity instanceof PlayerEntity && !mainHandItem.isEmpty() && mainHandItem.getItem() instanceof BowItem
                && !entity.getProjectile(mainHandItem).isEmpty()) {
            combat.addAction(ModCombatActions.PLAYER_RANGED_ATTACK);
        }
        
        if (!mainHandItem.isEmpty() && mainHandItem.getItem().isShield(mainHandItem, entity) ||
                !offHandItem.isEmpty() && offHandItem.getItem().isShield(offHandItem, entity)) {
            combat.addAction(ModCombatActions.PLAYER_SHIELD_BLOCK);
        }
        
        IStandPower.getStandPowerOptional(entity).ifPresent(stand -> {
            if (stand.hasPower()) {
                ResourceLocation standId = stand.getType().getRegistryName();
                float standLevel = EntityRPGData.get(entity).map(data -> data.getStandLevel(standId)).orElse(0f);
                Moveset.Builder standCategory = combat.subcategoryStart(
                        new TranslationTextComponent("category.jrpg.name_ellipsis", stand.getName()));
                
                List<Object2IntMap.Entry<ActionType<?>>> standMoves = new ArrayList<>();
                Object2IntMap<ActionType<?>> standLeveling = StandRPGActions.LEVELING.get(standId);
                if (standLeveling != null) {
                    for (Object2IntMap.Entry<ActionType<?>> entry : standLeveling.object2IntEntrySet()) {
                        if (entry.getIntValue() <= standLevel) {
                            standMoves.add(entry);
                        }
                    }
                }
                for (Object2IntMap.Entry<ActionType<?>> entry : StandRPGActions.COMMON_ACTIONS.object2IntEntrySet()) {
                    if (entry.getIntValue() <= standLevel) {
                        standMoves.add(entry);
                    }
                }
                
                standMoves = standMoves.stream().sorted(Comparator.comparingInt(Object2IntMap.Entry::getIntValue))
                .collect(Collectors.toCollection(ArrayList::new));
                
                List<ActionType<?>> filtered = new ArrayList<>();
                for (Object2IntMap.Entry<ActionType<?>> entry : standMoves) {
                    if (!filtered.contains(entry.getKey())) {
                        filtered.add(entry.getKey());
                    }
                }
                
                for (ActionType<?> type : filtered) {
                    standCategory.addAction(type);
                }
            }
        });
        
        if (entity instanceof MobEntity) {
            MobEntity mob = (MobEntity) entity;
            if (AiGoalWrappingAction.goalMatching(mob, pg -> pg.getGoal() instanceof MeleeAttackGoal).isPresent()) {
                combat.addAction(ModCombatActions.MOB_MELEE_ATTACK);
            }
        }
        
        combat.addAction(ModCombatActions.USE_ITEM);
        
        moveset.addAction(ModCombatActions.SKIP);
        
        MinecraftForge.EVENT_BUS.post(new JrpgTurnMovesetEvent(entity, moveset));
        
        return moveset.build();
    }
    
    
    public static class Builder {
        public final LivingEntity entity;
        public final Battle battle;
        private final ITextComponent name;
        private final Builder parent;
        private final List<Object> children = new ArrayList<>();
        private Category.SpecialCategory special = null;
//        private final List<ActionType<?>> actions = new ArrayList<>();
//        private final List<Moveset.Builder> children = new ArrayList<>();
        
        public Builder(LivingEntity entity, Battle battle) {
            this(null, null, entity, battle);
        }
        
        private Builder(ITextComponent name, Builder parent, LivingEntity entity, Battle battle) {
            this.name = name;
            this.parent = parent;
            this.entity = entity;
            this.battle = battle;
        }
        
        public Builder addAction(ActionType<? extends CombatAction> actionType) {
            if (actionType != null) {
                this.children.add(actionType);
            }
            return this;
        }
        
        public Builder special(Category.SpecialCategory special) {
            this.special = special;
            return this;
        }
        
        public Builder subcategoryStart(ITextComponent name) {
            for (Object child : children) {
                if (child instanceof Builder && ((Builder) child).name.equals(name)) {
                    return ((Builder) child);
                }
            }
            
            Builder childBuilder = new Builder(name, this, entity, battle);
            this.children.add(childBuilder);
            return childBuilder;
        }
        
        public Builder subcategoryEnd() {
            if (parent == null) {
                throw new IllegalStateException("This is not a sub-category!");
            }
            return parent;
        }
        
        public Moveset build() {
            return new Moveset(buildCategory(), entity, battle);
        }
        
        protected Category buildCategory() {
            Category root = new Category(this.name);
            for (Object child : children) {
                if (child instanceof ActionType) {
                    ActionType<?> type = (ActionType<?>) child;
                    CombatAction action = type.createAction(entity, battle);
                    if (action != null) {
                        root.addElement(new Category.Element(action));
                    }
                }
                else if (child instanceof Moveset.Builder) {
                    Moveset.Builder builder = (Moveset.Builder) child;
                    Moveset.Category subCategory = builder.buildCategory();
                    root.addElement(new Category.Element(subCategory));
                }
            }
            root.special = this.special;
            return root;
        }
    }
    
    public static class Category {
        public SpecialCategory special = null;
        @Nullable public final ITextComponent name;
        public final List<Element> elementsOrdered = new ArrayList<>();
        private final List<CombatAction> actions = new ArrayList<>();
        private final List<Category> children = new ArrayList<>();
        
        public Category(ITextComponent name) {
            this.name = name;
        }
        
        private void addElement(Element element) {
            this.elementsOrdered.add(element);
            element.either(actions::add, children::add);
        }
        
        
        public void toBuf(PacketBuffer buf) {
            NetworkUtil.writeOptionally(buf, name, text -> buf.writeComponent(text));
            NetworkUtil.writeCollection(buf, elementsOrdered, element -> element.toBuf(buf), false);
            NetworkUtil.writeOptionally(buf, special, element -> buf.writeEnum(element));
        }
        
        public static Category fromBuf(PacketBuffer buf, LivingEntity entity, Battle battle) {
            Category root = new Category(NetworkUtil.readOptional(buf, () -> buf.readComponent()).orElse(StringTextComponent.EMPTY));
            NetworkUtil.readCollection(buf, () -> Element.fromBuf(buf, entity, battle)).forEach(root::addElement);
            NetworkUtil.readOptional(buf, () -> buf.readEnum(SpecialCategory.class)).ifPresent(type -> root.special = type);
            
            return root;
        }
        
        
        public static class Element {
            public final CombatAction action;
            public final Category subCategory;
            
            public Element(CombatAction action) {
                this.action = action;
                this.subCategory = null;
            }
            
            public Element(Category subCategory) {
                this.action = null;
                this.subCategory = subCategory;
            }
            
            public void either(Consumer<CombatAction> action, Consumer<Category> subCategory) {
                if (this.action != null) {
                    action.accept(this.action);
                }
                else if (this.subCategory != null) {
                    subCategory.accept(this.subCategory);
                }
            }
            
            
            private void toBuf(PacketBuffer buf) {
                if (subCategory != null) {
                    buf.writeBoolean(true);
                    subCategory.toBuf(buf);
                }
                else {
                    buf.writeBoolean(false);
                    buf.writeResourceLocation(action.actionTypeId);
                }
            }
            
            private static Element fromBuf(PacketBuffer buf, LivingEntity entity, Battle battle) {
                boolean isCategory = buf.readBoolean();
                if (isCategory) {
                    Category category = Category.fromBuf(buf, entity, battle);
                    return new Element(category);
                }
                else {
                    ResourceLocation actionTypeId = buf.readResourceLocation();
                    ActionType<? extends CombatAction> type = ActionType.registry().getValue(actionTypeId);
                    if (type != null) {
                        CombatAction action = type.createAction(entity, battle);
                        return new Element(action);
                    }
                }
                return null;
            }
        }
        
        
        public enum SpecialCategory {
        }
    }
}
