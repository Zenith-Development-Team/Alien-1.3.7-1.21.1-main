package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.utils.entity.EntityUtil;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.mod.gui.clickgui.ClickGuiScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class AutoPot extends Module {

    public static AutoPot INSTANCE;

    private final SliderSetting delay =
            add(new SliderSetting("Delay", 5, 0, 10).setSuffix("s"));

    private final BooleanSetting resistance =
            add(new BooleanSetting("Resistance", true));
    private final BooleanSetting speed =
            add(new BooleanSetting("Speed", false));
    private final BooleanSetting strength =
            add(new BooleanSetting("Strength", false));
    private final BooleanSetting slowFalling =
            add(new BooleanSetting("SlowFalling", false));

    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround =
            add(new BooleanSetting("OnlyGround", true));
    private final BooleanSetting inventory =
            add(new BooleanSetting("InventorySwap", true));

    private final Timer delayTimer = new Timer();

    public AutoPot() {
        super("AutoPot", Category.Player);
        setChinese("自动药水");
        INSTANCE = this;
    }

    private boolean throwing = false;

    @Override
    public void onDisable() {
        throwing = false;
    }

    @Override
    public void onUpdate() {
        if (!onlyGround.getValue() || mc.player.isOnGround() && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0, -1, 0)))) {
            if (resistance.getValue() && (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2)) {
                throwing = checkThrow(StatusEffects.RESISTANCE.value());
                if (isThrow() && delayTimer.passedMs(delay.getValue() * 1000)) {
                    throwPotion(StatusEffects.RESISTANCE.value());
                    return;
                }
            }
            if (speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                throwing = checkThrow(StatusEffects.SPEED.value());
                if (isThrow() && delayTimer.passedMs(delay.getValue() * 1000)) {
                    throwPotion(StatusEffects.SPEED.value());
                    return;
                }
            }
            if (strength.getValue() && (!mc.player.hasStatusEffect(StatusEffects.STRENGTH))) {
                throwing = checkThrow(StatusEffects.STRENGTH.value());
                if (isThrow() && delayTimer.passedMs(delay.getValue() * 1000)) {
                    throwPotion(StatusEffects.STRENGTH.value());
                    return;
                }
            }
            if (slowFalling.getValue() && (!mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING))) {
                throwing = checkThrow(StatusEffects.SLOW_FALLING.value());
                if (isThrow() && delayTimer.passedMs(delay.getValue() * 1000)) {
                    throwPotion(StatusEffects.SLOW_FALLING.value());
                    return;
                }
            }
        }
    }

    public void throwPotion(StatusEffect targetEffect) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;
        if (inventory.getValue() && (newSlot = InventoryUtil.findPotionInventorySlot(targetEffect)) != -1) {
            Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90);
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            if (AntiCheat.INSTANCE.snapBack.getValue()) {
                Alien.ROTATION.snapBack();
            }
            delayTimer.reset();
        } else if ((newSlot = InventoryUtil.findPotion(targetEffect)) != -1) {
            Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90);
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.switchToSlot(oldSlot);
            if (AntiCheat.INSTANCE.snapBack.getValue()) {
                Alien.ROTATION.snapBack();
            }
            delayTimer.reset();
        }
    }

    public boolean isThrow() {
        return throwing;
    }

    public boolean checkThrow(StatusEffect targetEffect) {
        if (isOff()) return false;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof ClickGuiScreen) && !(mc.currentScreen instanceof GameMenuScreen)) {
            return false;
        }
        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return false;
        }
        if (InventoryUtil.findPotion(targetEffect) == -1 && (!inventory.getValue() || InventoryUtil.findPotionInventorySlot(targetEffect) == -1))
            return false;
        return true;
    }
}
