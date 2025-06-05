package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.DurabilityEvent;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Inject(method = "<init>(Lnet/minecraft/item/ItemConvertible;I)V", at = @At("RETURN"))
    private void hookInitItem(ItemConvertible item, int count, CallbackInfo ci) {
        ItemStack self = (ItemStack)(Object)this;
        DurabilityEvent durabilityEvent = new DurabilityEvent(self.getDamage());
        Alien.EVENT_BUS.post(durabilityEvent);
        if (durabilityEvent.isCancelled()) {
            self.setDamage(durabilityEvent.getDamage());
        }
    }

    @Inject(method = "fromNbtOrEmpty(Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"))
    private static void hookFromNbt(RegistryWrapper.WrapperLookup registries, NbtCompound nbt, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (!stack.isEmpty()) {
            DurabilityEvent durabilityEvent = new DurabilityEvent(stack.getDamage());
            Alien.EVENT_BUS.post(durabilityEvent);
            if (durabilityEvent.isCancelled()) {
                stack.setDamage(durabilityEvent.getDamage());
            }
        }
    }
}
