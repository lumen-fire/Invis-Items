package me.lumen.invisItems.commands.brigadier;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SlotArgumentWrapper {
    private final List<Function<EquipmentSlot, ArgumentBuilder<CommandSourceStack, ?>>> arguments = new ArrayList<>();
    private ArgumentBuilder<CommandSourceStack, ?> base;
    private @Nullable EquipmentSlotExecution execution;

    public static @NotNull SlotArgumentWrapper create(){
        return new SlotArgumentWrapper();
    }

    public SlotArgumentWrapper(){}

    public SlotArgumentWrapper base(ArgumentBuilder<CommandSourceStack, ?> base){
        this.base = base;
        return this;
    }

    public SlotArgumentWrapper then(Function<EquipmentSlot, ArgumentBuilder<CommandSourceStack, ?>> node){
        arguments.add(node);
        return this;
    }

    public SlotArgumentWrapper execution(EquipmentSlotExecution execution){
        this.execution = execution;
        return this;
    }

    public ArgumentBuilder<CommandSourceStack, ?> build(){
        for (EquipmentSlot slot : EquipmentSlot.values()){
            LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(slot.name().toLowerCase());
            if (execution != null){
                builder.executes(context -> execution.execute(context, slot));
            }
            for (Function<EquipmentSlot, ArgumentBuilder<CommandSourceStack, ?>> argBuilder : arguments){
                builder.then(argBuilder.apply(slot));
            }
            base.then(builder);
        }
        return base;
    }

    public interface EquipmentSlotExecution {
        int execute(CommandContext<CommandSourceStack> context, EquipmentSlot slot) throws CommandSyntaxException;
    }
}
