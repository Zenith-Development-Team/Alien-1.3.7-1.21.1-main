package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;

import java.util.Optional;

public class AutoTrade extends Module {
    public AutoTrade() {
        super("AutoTrade", Category.Player);
        setChinese("自动交易");
    }
    public final SliderSetting repeat = add(new SliderSetting("Repeat", 2, 1, 15, 1));
    public final BooleanSetting autoClose = add(new BooleanSetting("AutoClose", true));
    @Override
    public void onUpdate() {
        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler handler) {
            int i = 0;
            boolean flag = true;

            TradeOfferList list = handler.getRecipes();
            for (int size = 0; size < list.size(); ++size) {
                if (i >= repeat.getValue()) return;
                TradeOffer tradeOffer = list.get(size);
                if (!tradeOffer.isDisabled()) {
                    if (Alien.TRADE.inWhitelist(tradeOffer.getSellItem().getItem().getTranslationKey())) {
                        while (i < repeat.getValue() && flag) {
                            flag = false;
                            TradedItem firstBuyItem = tradeOffer.getFirstBuyItem();
                            if (!firstBuyItem.itemStack().isEmpty()) {
                                int count = InventoryUtil.getItemCount(firstBuyItem.itemStack().getItem());
                                if (handler.getSlot(0).getStack().getItem() == firstBuyItem.itemStack().getItem()) {
                                    count += handler.getSlot(0).getStack().getCount();
                                }
                                if (count < firstBuyItem.count()) {
                                    continue;
                                }
                            }
                            Optional<TradedItem> secondBuyItem = tradeOffer.getSecondBuyItem();
                            if (secondBuyItem.isPresent()) {
                                TradedItem secondItem = secondBuyItem.get();
                                int count = InventoryUtil.getItemCount(secondItem.itemStack().getItem());
                                if (handler.getSlot(1).getStack().getItem() == secondItem.itemStack().getItem()) {
                                    count += handler.getSlot(1).getStack().getCount();
                                }
                                if (count < secondItem.count()) {
                                    continue;
                                }
                            }
                            mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(size));
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 2, 1, SlotActionType.QUICK_MOVE, mc.player);
                            flag = true;
                            i++;
                            //mc.player.currentScreenHandler.onSlotClick(3, 1, SlotActionType.QUICK_MOVE, mc.player);
                        }
                    }
                }
            }
            //CommandManager.sendChatMessage(handler.getSlot(0).getStack().getItem().getName().getString());
            if (autoClose.getValue() && i < repeat.getValue()) {
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                mc.currentScreen.close();
            }
        }
    }
}
