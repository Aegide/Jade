package snownee.jade.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import snownee.jade.Jade;
import snownee.jade.util.DumpGenerator;

public class DumpHandlersCommand {

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(Commands.literal(Jade.MODID).then(Commands.literal("handlers").requires(source -> source.hasPermission(2)).executes(context -> {
			File file = new File("jade_handlers.md");
			try (FileWriter writer = new FileWriter(file)) {
				writer.write(DumpGenerator.generateInfoDump());
				context.getSource().sendSuccess(Component.translatable("command.jade.dump.success"), false);
				return 1;
			} catch (IOException e) {
				context.getSource().sendFailure(Component.literal(e.getClass().getSimpleName() + ": " + e.getMessage()));
				return 0;
			}
		})));
	}
}
