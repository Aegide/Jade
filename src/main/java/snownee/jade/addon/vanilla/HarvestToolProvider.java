package snownee.jade.addon.vanilla;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mcp.mobius.waila.Waila;
import mcp.mobius.waila.addons.core.HUDHandlerBlocks;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITaggableList;
import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;
import snownee.jade.JadePlugin;
import snownee.jade.Renderables;

public class HarvestToolProvider implements IComponentProvider, ISelectiveResourceReloadListener {

	public static final HarvestToolProvider INSTANCE = new HarvestToolProvider();

	public static final Cache<TestCase, String> toolNames = CacheBuilder.newBuilder().build();
	public static final Cache<BlockState, TestCase> resultCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
	public static final List<TestCase> testTools = Lists.newLinkedList();
	public static final Map<ToolType, TestCase> toolTypeMap = Maps.newHashMap();

	private static final TestCase NO_TOOL = new TestCase(ItemStack.EMPTY, "no_tool", null);
	private static final TestCase UNBREAKABLE = new TestCase(ItemStack.EMPTY, "unbreakable", null);

	private static final ITextComponent UNBREAKABLE_TEXT = new TranslationTextComponent("jade.harvest_tool.unbreakable").mergeStyle(TextFormatting.DARK_RED);;

	static {
		/* off */
        registerTool(new ItemStack(Items.WOODEN_PICKAXE), "pickaxe", ToolType.PICKAXE)
            .addTool(Items.STONE_PICKAXE)
            .addTool(Items.IRON_PICKAXE)
            .addTool(Items.DIAMOND_PICKAXE)
            .addTool(Items.NETHERITE_PICKAXE);
        registerTool(new ItemStack(Items.WOODEN_AXE), "axe", ToolType.AXE);
        registerTool(new ItemStack(Items.WOODEN_SHOVEL), "shovel", ToolType.SHOVEL);
        registerTool(new ItemStack(Items.WOODEN_HOE), "hoe", ToolType.HOE);
        registerTool(new ItemStack(Items.SHEARS), "shears", null);
        /* on */
	}

	public static String getToolName(TestCase testCase) {
		try {
			return toolNames.get(testCase, () -> {
				if (I18n.hasKey("jade.harvest_tool." + testCase.name)) {
					return I18n.format("jade.harvest_tool." + testCase.name);
				} else {
					return StringUtils.capitalize(testCase.name);
				}
			});
		} catch (ExecutionException e) {
			Waila.LOGGER.catching(e);
			return testCase.name;
		}
	}

	@Nullable
	public static TestCase getTool(BlockState state, World world, BlockPos pos) {
		float hardness = state.getBlockHardness(world, pos);
		if (hardness < 0) {
			return UNBREAKABLE;
		}
		ToolType toolType = state.getHarvestTool();
		if (toolType != null) {
			TestCase testCase = toolTypeMap.get(toolType);
			if (testCase == null) {
				testCase = new TestCase(ItemStack.EMPTY, toolType.getName(), toolType);
				toolTypeMap.put(toolType, testCase);
			}
			return testCase;
		}
		if (state.getRequiresTool()) {
			for (TestCase testCase : testTools) {
				if (testCase.stack.isEmpty()) {
					continue;
				}
				if (testCase.stack.canHarvestBlock(state)) {
					return testCase;
				}
			}
		}
		return NO_TOOL;
	}

	public static synchronized TestCase registerTool(ItemStack stack, String name, @Nullable ToolType toolType) {
		TestCase testCase = new TestCase(stack, name, toolType);
		testTools.add(testCase);
		if (toolType != null) {
			toolTypeMap.put(toolType, testCase);
		}
		return testCase;
	}

	@Override
	public void appendHead(List<ITextComponent> tooltip, IDataAccessor accessor, IPluginConfig config) {
		if (config.get(JadePlugin.HARVEST_TOOL_NEW_LINE)) {
			return;
		}
		ITextComponent text = getText(accessor, config);
		if (text == null || text == UNBREAKABLE_TEXT) {
			return;
		}
		ITaggableList<ResourceLocation, ITextComponent> taggableList = (ITaggableList<ResourceLocation, ITextComponent>) tooltip;
		ITextComponent component = taggableList.getTag(HUDHandlerBlocks.OBJECT_NAME_TAG);
		if (component != null) {
			taggableList.setTag(HUDHandlerBlocks.OBJECT_NAME_TAG, Renderables.of(component, text));
		}
	}

	@Override
	public void appendBody(List<ITextComponent> tooltip, IDataAccessor accessor, IPluginConfig config) {
		ITextComponent text = getText(accessor, config);
		if (text != UNBREAKABLE_TEXT) {
			if (!config.get(JadePlugin.HARVEST_TOOL_NEW_LINE)) {
				return;
			}
			if (text == null) {
				return;
			}
		}
		tooltip.add(0, text);
	}

	public ITextComponent getText(IDataAccessor accessor, IPluginConfig config) {
		if (!config.get(JadePlugin.HARVEST_TOOL)) {
			return null;
		}
		BlockState state = accessor.getBlockState();
		TestCase testCase = NO_TOOL;
		resultCache.invalidateAll();
		try {
			testCase = resultCache.get(state, () -> getTool(state, accessor.getWorld(), accessor.getPosition()));
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		if (testCase == UNBREAKABLE) {
			return UNBREAKABLE_TEXT;
		}
		if (testCase == NO_TOOL) {
			return null;
		}
		if (!state.getRequiresTool() && !config.get(JadePlugin.EFFECTIVE_TOOL)) {
			return null;
		}
		int level = state.getHarvestLevel();
		String name = "";
		ItemStack tool = testCase.toolMap.get(level);
		if (tool == null) {
			tool = testCase.stack;
			if (level > 0) {
				name = " " + level;
			}
		}
		boolean canHarvest = ForgeHooks.canHarvestBlock(state, accessor.getPlayer(), accessor.getWorld(), accessor.getPosition());
		String sub;
		if (state.getRequiresTool()) {
			sub = canHarvest ? "§a✔" : "§4✕";
		} else {
			ItemStack held = accessor.getPlayer().getHeldItemMainhand();
			sub = (canHarvest && ForgeHooks.isToolEffective(accessor.getWorld(), accessor.getPosition(), held)) ? "§a✔" : "";
		}
		int offsetY = config.get(JadePlugin.HARVEST_TOOL_NEW_LINE) ? 0 : -3;
		if (name.isEmpty()) {
			return Renderables.of(Renderables.item(tool, 0.75f, offsetY), Renderables.sub(sub));
		} else {
			return Renderables.of(Renderables.item(tool, 0.75f, offsetY), Renderables.sub(sub), Renderables.offsetText(name, 3, offsetY + 3));
		}

		//        name = getToolName(testCase);
		//        boolean canHarvest = ForgeHooks.canHarvestBlock(accessor.getBlockState(), accessor.getPlayer(), accessor.getWorld(), accessor.getPosition());
		//        if (level > 0) {
		//            String levelStr = "jade.harvest_tool." + testCase.name + "." + level;
		//            if (I18n.hasKey(levelStr)) {
		//                levelStr = I18n.format(levelStr);
		//            } else {
		//                levelStr = String.valueOf(level);
		//            }
		//            tooltip.add(new TranslationTextComponent("jade.harvest_tool.fmt", name, levelStr).mergeStyle(canHarvest ? TextFormatting.GREEN : TextFormatting.DARK_RED));
		//        } else {
		//            tooltip.add(new StringTextComponent(name).mergeStyle(canHarvest ? TextFormatting.GREEN : TextFormatting.DARK_RED));
		//        }
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
		if (resourcePredicate.test(VanillaResourceType.LANGUAGES)) {
			toolNames.invalidateAll();
			resultCache.invalidateAll();
		}
	}

	public static class TestCase {
		private final ItemStack stack;
		private final String name;
		private final ToolType toolType;
		private final Int2ObjectMap<ItemStack> toolMap = new Int2ObjectOpenHashMap<>();

		public TestCase(ItemStack stack, String name, @Nullable ToolType toolType) {
			this.stack = stack;
			this.name = name;
			this.toolType = toolType;
			addTool(stack);
		}

		public TestCase addTool(Item tool) {
			return addTool(new ItemStack(tool));
		}

		public TestCase addTool(ItemStack stack) {
			int level = 0;
			if (toolType != null) {
				level = stack.getHarvestLevel(toolType, null, null);
			}
			toolMap.put(level, stack);
			return this;
		}
	}

}
