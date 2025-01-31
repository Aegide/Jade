package snownee.jade.addon.core;

import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.Identifiers;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.util.PlatformProxy;

public enum RegistryNameProvider implements IBlockComponentProvider, IEntityComponentProvider {

	INSTANCE;

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		tooltip.add(config.getWailaConfig().getFormatting().registryName(PlatformProxy.getId(accessor.getBlock()).toString()));
	}

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		tooltip.add(config.getWailaConfig().getFormatting().registryName(PlatformProxy.getId(accessor.getEntity().getType()).toString()));
	}

	@Override
	public ResourceLocation getUid() {
		return Identifiers.CORE_REGISTRY_NAME;
	}

	@Override
	public boolean enabledByDefault() {
		return false;
	}

	@Override
	public int getDefaultPriority() {
		return TooltipPosition.HEAD + 100;
	}

}
