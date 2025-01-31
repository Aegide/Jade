package snownee.jade.impl.ui;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.overlay.ProgressTracker.TrackInfo;
import snownee.jade.overlay.WailaTickHandler;

public class ProgressElement extends Element {
	private final float progress;
	@Nullable
	private final Component text;
	private final ProgressStyle style;
	@Nullable
	private final BorderStyle borderStyle;
	private TrackInfo track;

	public ProgressElement(float progress, Component text, ProgressStyle style, BorderStyle borderStyle) {
		this.progress = Mth.clamp(progress, 0, 1);
		this.text = text;
		this.style = style;
		this.borderStyle = borderStyle;
	}

	@Override
	public Vec2 getSize() {
		int height = text == null ? 8 : 14;
		float width = 0;
		if (borderStyle != null) {
			width += borderStyle.width * 2;
		}
		if (text != null) {
			Font font = Minecraft.getInstance().font;
			width += font.width(text.getString()) + 3;
		}
		width = Math.max(20, width);
		if (getTag() != null) {
			track = WailaTickHandler.instance().progressTracker.createInfo(getTag(), progress, width);
			width = track.getWidth();
		}
		return new Vec2(width, height);
	}

	@Override
	public void render(PoseStack matrixStack, float x, float y, float maxX, float maxY) {
		Vec2 size = getCachedSize();
		int b = 0;
		if (borderStyle != null) {
			DisplayHelper.INSTANCE.drawBorder(matrixStack, x, y, maxX - 2, y + size.y - 2, borderStyle);
			b = borderStyle.width;
		}
		float progress = this.progress;
		if (track == null && getTag() != null) {
			track = WailaTickHandler.instance().progressTracker.createInfo(getTag(), progress, getSize().y);
		}
		if (track != null) {
			progress = track.tick(Minecraft.getInstance().getDeltaFrameTime());
		}
		style.render(matrixStack, x + b, y + b, maxX - x - b * 2 - 2, size.y - b * 2 - 2, progress, text);
	}

}
