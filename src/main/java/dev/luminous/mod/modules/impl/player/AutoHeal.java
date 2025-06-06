package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.eventbus.EventPriority;
import dev.luminous.api.events.impl.RotateEvent;
import dev.luminous.api.utils.entity.EntityUtil;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;


public class AutoHeal extends Module {

    public static AutoHeal INSTANCE;

    private final SliderSetting delay =
            add(new SliderSetting("Delay", 3, 0, 10));
    public final BooleanSetting down =
            add(new BooleanSetting("Down", true));
    private final BooleanSetting onlyDamaged =
            add(new BooleanSetting("OnlyDamaged", true));
    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround =
            add(new BooleanSetting("OnlyGround", true));
    private final BooleanSetting inventory =
            add(new BooleanSetting("InventorySwap", true));

    private final Timer delayTimer = new Timer();
    private boolean throwing = false;
    private int potions;

    public AutoHeal() {
        super("AutoHeal", Category.Player);
        setChinese("自动治疗");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        throwing = false;
    }

    @Override
    public void onUpdate() {
        throwing = checkThrow();
        potions = InventoryUtil.getPotionCount(StatusEffects.INSTANT_HEALTH.value());
        if (isThrow() && delayTimer.passedMs(delay.getValueInt() * 20L) && (!onlyGround.getValue() || mc.player.isOnGround())) {
            throwPotion();
        }
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
    }

    @Override
    public String getInfo() {
        return String.valueOf(potions);
    }

    public void throwPotion() {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;
        if (inventory.getValue() && (newSlot = InventoryUtil.findPotionInventorySlot(StatusEffects.INSTANT_HEALTH.value())) != -1) {
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            delayTimer.reset();
        } else if ((newSlot = InventoryUtil.findPotion(StatusEffects.INSTANT_HEALTH.value())) != -1) {
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.switchToSlot(oldSlot);
            delayTimer.reset();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void RotateEvent(RotateEvent event) {
        if (!down.getValue()) return;
        if (isThrow()) event.setPitch(88);
    }

    public boolean isThrow() {
        return throwing;
    }

    public boolean checkThrow() {
        if (isOff()) return false;
        if (mc.currentScreen instanceof ChatScreen) return false;
        if (mc.currentScreen != null) return false;
        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return false;
        }
        if (onlyDamaged.getValue() && mc.player.getHealth() >= 20) {
            return false;
        }
        return InventoryUtil.findPotion(StatusEffects.INSTANT_HEALTH.value()) != -1 || (inventory.getValue() && InventoryUtil.findPotionInventorySlot(StatusEffects.INSTANT_HEALTH.value()) != -1);
    }
}
