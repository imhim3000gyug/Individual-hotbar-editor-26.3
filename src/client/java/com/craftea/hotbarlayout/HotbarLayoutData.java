package com.craftea.hotbarlayout;

public class HotbarLayoutData {
	private int x = 0;
	private int y = 0;
	private int offhandX = 0;
	private int offhandY = 0;
	private SlotData[] slots = new SlotData[9];
	private boolean snapEnabled = true;
	private boolean offhandVisible = true;
	private transient int editMode = 0;

	public HotbarLayoutData() {
		for (int i = 0; i < 9; ++i) this.slots[i] = new SlotData();
	}

	public int getX() { return this.x; }
	public void setX(int x) { this.x = x; }
	public int getY() { return this.y; }
	public void setY(int y) { this.y = y; }
	public int getOffhandX() { return this.offhandX; }
	public void setOffhandX(int x) { this.offhandX = x; }
	public int getOffhandY() { return this.offhandY; }
	public void setOffhandY(int y) { this.offhandY = y; }
	public boolean isOffhandVisible() { return this.offhandVisible; }
	public void toggleOffhandVisible() { this.offhandVisible = !this.offhandVisible; }
	public SlotData[] getSlots() { return this.slots; }
	public boolean isSnapEnabled() { return this.snapEnabled; }
	public void toggleSnap() { this.snapEnabled = !this.snapEnabled; }
	public int getEditMode() { return this.editMode; }
	public void setEditMode(int mode) { this.editMode = mode; }
	public void nextEditMode() { this.editMode = (this.editMode + 1) % 2; }

	public int snap(int value) {
		return this.snapEnabled ? Math.round((float) value / 4.0F) * 4 : value;
	}

	public void resetAll() {
		this.x = 0;
		this.y = 0;
		this.offhandX = 0;
		this.offhandY = 0;
		this.offhandVisible = true;
		for (SlotData slot : this.slots) {
			slot.x = 0;
			slot.y = 0;
			slot.visible = true;
		}
	}

	public HotbarLayoutData copy() {
		HotbarLayoutData copy = new HotbarLayoutData();
		copy.x = this.x;
		copy.y = this.y;
		copy.offhandX = this.offhandX;
		copy.offhandY = this.offhandY;
		copy.snapEnabled = this.snapEnabled;
		copy.offhandVisible = this.offhandVisible;
		for (int i = 0; i < 9; ++i) {
			copy.slots[i].x = this.slots[i].x;
			copy.slots[i].y = this.slots[i].y;
			copy.slots[i].visible = this.slots[i].visible;
		}
		return copy;
	}

	public static class SlotData {
		public int x = 0;
		public int y = 0;
		public boolean visible = true;
	}
}