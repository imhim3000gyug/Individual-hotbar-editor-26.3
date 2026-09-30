package com.craftea.hotbarlayout;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class HotbarEditorScreen extends Screen {
	private static final Identifier HOTBAR_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar");
	private static final Identifier OFFHAND_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
	private final Screen parent;
	private HotbarLayoutData editData;
	private boolean dragging = false;
	private int dragSlot = -1;
	private int dragStartX;
	private int dragStartY;
	private int dragOrigX;
	private int dragOrigY;

	public HotbarEditorScreen(Screen parent) {
		super(Component.literal("Hotbar Layout Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.editData = HotbarLayoutMod.getConfig().getData().copy();
		int buttonWidth = 120;
		int buttonHeight = 20;
		int startX = 6;
		int startY = 6;
		int gap = 24;
		this.addRenderableWidget(Button.builder(Component.literal(this.getModeText()), button -> {
			this.editData.nextEditMode();
			button.setMessage(Component.literal(this.getModeText()));
		}).bounds(startX, startY, buttonWidth, buttonHeight).build());
		this.addRenderableWidget(Button.builder(Component.literal(this.getSnapText()), button -> {
			this.editData.toggleSnap();
			button.setMessage(Component.literal(this.getSnapText()));
		}).bounds(startX, startY + gap, buttonWidth, buttonHeight).build());
		this.addRenderableWidget(Button.builder(Component.literal("Reset All"), button -> this.editData.resetAll())
				.bounds(startX, startY + gap * 2, buttonWidth, buttonHeight).build());
		this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> {
			HotbarLayoutMod.getConfig().setData(this.editData);
			HotbarLayoutMod.getConfig().save();
			this.onClose();
		}).bounds(this.width - buttonWidth - 6, startY, buttonWidth, buttonHeight).build());
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
				.bounds(this.width - buttonWidth - 6, startY + gap, buttonWidth, buttonHeight).build());
	}

	private String getModeText() {
		return this.editData.getEditMode() == 0 ? "Mode: GROUP" : "Mode: INDIVIDUAL";
	}

	private String getSnapText() {
		return this.editData.isSnapEnabled() ? "Snap: ON" : "Snap: OFF";
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(null, 0, 0, this.width, this.height, Integer.MIN_VALUE);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int centerX = this.width / 2;
		int baseY = this.height - 22;
		int groupX = centerX - 91 + this.editData.getX();
		int groupY = baseY + this.editData.getY();
		for (int i = 0; i < 9; ++i) {
			HotbarLayoutData.SlotData slot = this.editData.getSlots()[i];
			if (!slot.visible && this.editData.getEditMode() != 1) continue;
			int slotX = groupX + i * 20 + slot.x;
			int slotY = groupY + slot.y;
			int slotWidth = i == 0 || i == 8 ? 21 : 20;
			int textureX = groupX + slot.x;
			int scissorX = textureX + i * 20;
			if (i == 0) scissorX = textureX;
			graphics.enableScissor(scissorX, slotY, scissorX + slotWidth, slotY + 22);
			graphics.blitSprite(null, HOTBAR_TEXTURE, textureX, slotY, 182, 22);
			graphics.disableScissor();
			if (!slot.visible) graphics.fill(null, slotX, slotY, slotX + slotWidth, slotY + 22, -1996554240);
			graphics.text(this.font, String.valueOf(i + 1), slotX + slotWidth / 2 - 3, slotY + 7, 0xFFFFFF, true);
		}
		int offhandX = groupX - 29 + this.editData.getOffhandX();
		int offhandY = groupY - 1 + this.editData.getOffhandY();
		graphics.blitSprite(null, OFFHAND_TEXTURE, offhandX, offhandY, 29, 24);
		if (!this.editData.isOffhandVisible()) graphics.fill(null, offhandX, offhandY, offhandX + 29, offhandY + 24, -1996554240);
		graphics.text(this.font, "O", offhandX + 11, offhandY + 8, 0xFFFFFF, true);
		String hint = this.getHintText();
		graphics.text(this.font, hint, centerX - this.font.width(hint) / 2, this.height / 2, 0xCCCCCC, true);
	}

	private String getHintText() {
		return this.editData.getEditMode() == 0
				? "Drag to move the entire hotbar"
				: "Drag individual slots. Right-click to toggle visibility";
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
		if (super.mouseClicked(event, isDouble)) return true;
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		int centerX = this.width / 2;
		int baseY = this.height - 22;
		int groupX = centerX - 91 + this.editData.getX();
		int groupY = baseY + this.editData.getY();
		if (this.editData.getEditMode() == 1) {
			int offhandX = groupX - 29 + this.editData.getOffhandX();
			int offhandY = groupY - 1 + this.editData.getOffhandY();
			if (mouseX >= offhandX && mouseX <= offhandX + 29 && mouseY >= offhandY && mouseY <= offhandY + 24) {
				if (button == 1) {
					this.editData.toggleOffhandVisible();
					this.applyPreview();
					return true;
				}
				if (button == 0) {
					this.beginDrag(9, mouseX, mouseY, this.editData.getOffhandX(), this.editData.getOffhandY());
					return true;
				}
			}
			for (int i = 8; i >= 0; --i) {
				HotbarLayoutData.SlotData slot = this.editData.getSlots()[i];
				int slotX = groupX + i * 20 + slot.x;
				int slotY = groupY + slot.y;
				int slotWidth = i == 0 || i == 8 ? 21 : 20;
				if (mouseX < slotX || mouseX > slotX + slotWidth || mouseY < slotY || mouseY > slotY + 22) continue;
				if (button == 1) {
					slot.visible = !slot.visible;
					this.applyPreview();
					return true;
				}
				if (button == 0) {
					this.beginDrag(i, mouseX, mouseY, slot.x, slot.y);
					return true;
				}
			}
		} else if (this.editData.getEditMode() == 0 && button == 0) {
			this.beginDrag(-1, mouseX, mouseY, this.editData.getX(), this.editData.getY());
			return true;
		}
		return false;
	}

	private void beginDrag(int slot, double mouseX, double mouseY, int originalX, int originalY) {
		this.dragging = true;
		this.dragSlot = slot;
		this.dragStartX = (int) mouseX;
		this.dragStartY = (int) mouseY;
		this.dragOrigX = originalX;
		this.dragOrigY = originalY;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (this.dragging && event.button() == 0) {
			int dx = (int) event.x() - this.dragStartX;
			int dy = (int) event.y() - this.dragStartY;
			if (this.editData.getEditMode() == 1 && this.dragSlot >= 0 && this.dragSlot <= 8) {
				this.editData.getSlots()[this.dragSlot].x = this.editData.snap(this.dragOrigX + dx);
				this.editData.getSlots()[this.dragSlot].y = this.editData.snap(this.dragOrigY + dy);
			} else if (this.editData.getEditMode() == 1 && this.dragSlot == 9) {
				this.editData.setOffhandX(this.editData.snap(this.dragOrigX + dx));
				this.editData.setOffhandY(this.editData.snap(this.dragOrigY + dy));
			} else if (this.editData.getEditMode() == 0) {
				this.editData.setX(this.editData.snap(this.dragOrigX + dx));
				this.editData.setY(this.editData.snap(this.dragOrigY + dy));
			}
			this.applyPreview();
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0) this.dragging = false;
		return super.mouseReleased(event);
	}

	private void applyPreview() {
		HotbarLayoutMod.getConfig().setData(this.editData.copy());
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			HotbarLayoutMod.getConfig().load();
			this.minecraft.gui.setScreen(this.parent);
		}
	}

	@Override
	public boolean isPauseScreen() { return false; }
}