package com.craftea.hotbarlayout;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.Locale;

public class HotbarEditorScreen extends Screen {
	private static final Identifier HOTBAR_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar");
	private static final Identifier OFFHAND_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
	private final Screen parent;
	private HotbarLayoutData editData;
	private Button visibilityButton;
	private EditBox scaleField;
	private boolean dragging = false;
	private int dragSlot = -1;
	private int selectedSlot = -1;
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
		this.selectedSlot = -1;
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
		this.visibilityButton = this.addRenderableWidget(Button.builder(Component.literal(this.getVisibilityText()), button -> {
			if (this.selectedSlot == 9) {
				this.editData.toggleOffhandVisible();
			} else if (this.selectedSlot >= 0 && this.selectedSlot < 9) {
				this.editData.getSlots()[this.selectedSlot].visible = !this.editData.getSlots()[this.selectedSlot].visible;
			} else {
				return;
			}
			this.applyPreview();
			button.setMessage(Component.literal(this.getVisibilityText()));
		}).bounds(startX, startY + gap * 3, buttonWidth, buttonHeight).build());
		this.scaleField = this.addRenderableWidget(new EditBox(this.font, startX, startY + gap * 4 + 12,
				buttonWidth, buttonHeight, Component.literal("Scale (0.10-10)")));
		this.scaleField.setHint(Component.literal("Scale (0.10-10)"));
		this.scaleField.setMaxLength(5);
		this.scaleField.setValue("1.00");
		this.scaleField.setResponder(this::updateSelectedScale);
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

	private String getVisibilityText() {
		if (this.selectedSlot == 9) {
			return this.editData.isOffhandVisible() ? "Hide Offhand" : "Show Offhand";
		}
		if (this.selectedSlot >= 0 && this.selectedSlot < 9) {
			return (this.editData.getSlots()[this.selectedSlot].visible ? "Hide Slot " : "Show Slot ") + (this.selectedSlot + 1);
		}
		return "Select a Slot";
	}

	private void updateSelectedScale(String value) {
		if (this.selectedSlot < 0 || this.selectedSlot >= 9
				|| !value.matches("\\d{0,2}(\\.\\d{0,2})?")) return;
		try {
			float scale = Float.parseFloat(value);
			if (scale >= 0.10F && scale <= 10.0F) {
				this.editData.getSlots()[this.selectedSlot].setScale(scale);
				this.applyPreview();
			}
		} catch (NumberFormatException ignored) {
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(RenderPipelines.GUI, 0, 0, this.width, this.height, Integer.MIN_VALUE);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(this.font, "Scale (0.10-10)", 6, 109, 0xCCCCCC, true);
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
			float scale = slot.scale;
			float pivotX = slotX + slotWidth / 2.0F;
			float pivotY = slotY + 11.0F;
			graphics.pose().pushMatrix();
			graphics.pose().translate(pivotX * (1.0F - scale), pivotY * (1.0F - scale));
			graphics.pose().scale(scale, scale);
			graphics.enableScissor(scissorX, slotY, scissorX + slotWidth, slotY + 22);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, textureX, slotY, 182, 22);
			graphics.disableScissor();
			if (!slot.visible) graphics.fill(RenderPipelines.GUI, slotX, slotY, slotX + slotWidth, slotY + 22, -1996554240);
			graphics.text(this.font, String.valueOf(i + 1), slotX + slotWidth / 2 - 3, slotY + 7, 0xFFFFFF, true);
			if (this.selectedSlot == i) this.drawSelection(graphics, slotX, slotY, slotWidth, 22);
			graphics.pose().popMatrix();
		}
		int offhandX = groupX - 29 + this.editData.getOffhandX();
		int offhandY = groupY - 1 + this.editData.getOffhandY();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OFFHAND_TEXTURE, offhandX, offhandY, 29, 24);
		if (!this.editData.isOffhandVisible()) graphics.fill(RenderPipelines.GUI, offhandX, offhandY, offhandX + 29, offhandY + 24, -1996554240);
		graphics.text(this.font, "O", offhandX + 11, offhandY + 8, 0xFFFFFF, true);
		if (this.selectedSlot == 9) this.drawSelection(graphics, offhandX, offhandY, 29, 24);
		String hint = this.getHintText();
		graphics.text(this.font, hint, centerX - this.font.width(hint) / 2, this.height / 2, 0xCCCCCC, true);
	}

	private String getHintText() {
		return this.editData.getEditMode() == 0
				? "Drag to move the entire hotbar"
				: "Drag to move. Select a slot, then use its visibility button";
	}

	private void drawSelection(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		int color = 0xFFFF4040;
		graphics.fill(RenderPipelines.GUI, x, y, x + width, y + 1, color);
		graphics.fill(RenderPipelines.GUI, x, y + height - 1, x + width, y + height, color);
		graphics.fill(RenderPipelines.GUI, x, y, x + 1, y + height, color);
		graphics.fill(RenderPipelines.GUI, x + width - 1, y, x + width, y + height, color);
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
				this.beginDrag(9, mouseX, mouseY, this.editData.getOffhandX(), this.editData.getOffhandY());
				return true;
			}
			for (int i = 8; i >= 0; --i) {
				HotbarLayoutData.SlotData slot = this.editData.getSlots()[i];
				int slotX = groupX + i * 20 + slot.x;
				int slotY = groupY + slot.y;
				int slotWidth = i == 0 || i == 8 ? 21 : 20;
				double pivotX = slotX + slotWidth / 2.0;
				double pivotY = slotY + 11.0;
				double halfWidth = slotWidth * slot.scale / 2.0;
				double halfHeight = 22 * slot.scale / 2.0;
				if (mouseX < pivotX - halfWidth || mouseX > pivotX + halfWidth
						|| mouseY < pivotY - halfHeight || mouseY > pivotY + halfHeight) continue;
				this.beginDrag(i, mouseX, mouseY, slot.x, slot.y);
				return true;
			}
		} else if (this.editData.getEditMode() == 0) {
			this.beginDrag(-1, mouseX, mouseY, this.editData.getX(), this.editData.getY());
			return true;
		}
		return false;
	}

	private void beginDrag(int slot, double mouseX, double mouseY, int originalX, int originalY) {
		this.dragging = true;
		this.dragSlot = slot;
		if (this.editData.getEditMode() == 1) {
			this.selectedSlot = slot;
			this.visibilityButton.setMessage(Component.literal(this.getVisibilityText()));
			this.scaleField.setValue(slot >= 0 && slot < 9
					? String.format(Locale.ROOT, "%.2f", this.editData.getSlots()[slot].scale)
					: "1.00");
		}
		this.dragStartX = (int) mouseX;
		this.dragStartY = (int) mouseY;
		this.dragOrigX = originalX;
		this.dragOrigY = originalY;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (this.dragging) {
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
		this.dragging = false;
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