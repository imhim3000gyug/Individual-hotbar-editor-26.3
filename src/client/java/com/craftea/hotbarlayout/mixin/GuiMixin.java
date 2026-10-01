package com.craftea.hotbarlayout.mixin;

import com.craftea.hotbarlayout.HotbarLayoutData;
import com.craftea.hotbarlayout.HotbarLayoutMod;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class GuiMixin {
	private int currentSlotIndex = -1;
	private boolean isOffhand = false;

	@Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void hotbarLayoutEditor$redirectBlitSprite(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
		HotbarLayoutData data = HotbarLayoutMod.getConfig().getData();
		String path = sprite.getPath();
		if (path.equals("hud/hotbar")) {
			for (int i = 0; i < 9; ++i) {
				HotbarLayoutData.SlotData slot = data.getSlots()[i];
				if (!slot.visible) continue;
				int offsetX = data.getX() + slot.x;
				int offsetY = data.getY() + slot.y;
				int scissorX = x + offsetX + i * 20;
				if (i == 0) scissorX = x + offsetX;
				int scissorWidth = i == 0 || i == 8 ? 21 : 20;
				float scale = slot.scale;
				float pivotX = scissorX + scissorWidth / 2.0F;
				float pivotY = y + offsetY + 11.0F;
				graphics.pose().pushMatrix();
				graphics.pose().translate(pivotX * (1.0F - scale), pivotY * (1.0F - scale));
				graphics.pose().scale(scale, scale);
				graphics.enableScissor(scissorX, y + offsetY, scissorX + scissorWidth, y + offsetY + 22);
				graphics.blitSprite(pipeline, sprite, x + offsetX, y + offsetY, width, height);
				graphics.disableScissor();
				graphics.pose().popMatrix();
			}
		} else if (path.equals("hud/hotbar_selection")) {
			int slot = (x - (graphics.guiWidth() / 2 - 91 - 1)) / 20;
			if (slot >= 0 && slot <= 8 && data.getSlots()[slot].visible) {
				int offsetX = data.getX() + data.getSlots()[slot].x;
				int offsetY = data.getY() + data.getSlots()[slot].y;
				float scale = data.getSlots()[slot].scale;
				float pivotX = x + offsetX + width / 2.0F;
				float pivotY = y + offsetY + height / 2.0F;
				graphics.pose().pushMatrix();
				graphics.pose().translate(pivotX * (1.0F - scale), pivotY * (1.0F - scale));
				graphics.pose().scale(scale, scale);
				graphics.blitSprite(pipeline, sprite, x + offsetX, y + offsetY, width, height);
				graphics.pose().popMatrix();
			}
		} else if (path.equals("hud/hotbar_offhand_left") || path.equals("hud/hotbar_offhand_right")) {
			if (data.isOffhandVisible()) {
				int offsetX = data.getX() + data.getOffhandX();
				int offsetY = data.getY() + data.getOffhandY();
				graphics.blitSprite(pipeline, sprite, x + offsetX, y + offsetY, width, height);
			}
		} else {
			graphics.blitSprite(pipeline, sprite, x, y, width, height);
		}
	}

	@Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
	private void hotbarLayoutEditor$onRenderSlotHead(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed, CallbackInfo ci) {
		HotbarLayoutData data = HotbarLayoutMod.getConfig().getData();
		int screenCenter = graphics.guiWidth() / 2;
		this.currentSlotIndex = -1;
		this.isOffhand = false;
		if (x == screenCenter - 117 || x == screenCenter + 101) {
			this.isOffhand = true;
		} else {
			int slot = (x - screenCenter + 88) / 20;
			if (slot >= 0 && slot <= 8) this.currentSlotIndex = slot;
		}
		if (this.isOffhand && !data.isOffhandVisible()) {
			ci.cancel();
			return;
		}
		if (this.currentSlotIndex != -1 && !data.getSlots()[this.currentSlotIndex].visible) {
			ci.cancel();
			return;
		}
		graphics.pose().pushMatrix();
		if (this.isOffhand) {
			graphics.pose().translate(data.getX() + data.getOffhandX(), data.getY() + data.getOffhandY());
		} else if (this.currentSlotIndex != -1) {
			HotbarLayoutData.SlotData slot = data.getSlots()[this.currentSlotIndex];
			float scale = slot.scale;
			graphics.pose().translate(data.getX() + slot.x + (x + 10.0F) * (1.0F - scale),
					data.getY() + slot.y + (y + 10.0F) * (1.0F - scale));
			graphics.pose().scale(scale, scale);
		}
	}

	@Inject(method = "extractSlot", at = @At("RETURN"))
	private void hotbarLayoutEditor$onRenderSlotTail(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed, CallbackInfo ci) {
		if (!ci.isCancelled()) graphics.pose().popMatrix();
	}
}