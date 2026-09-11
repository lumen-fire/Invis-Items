package me.lumen.invisItems.commands.brigadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemStackArg implements CustomArgumentType<ItemStack, ItemStack> {
    public static final ItemStackArg ARGUMENT = new ItemStackArg();
    private ItemStackArg(){}
    private static final SimpleCommandExceptionType ERROR_ITEM_AIR = new SimpleCommandExceptionType(new LiteralMessage("Item cannot be air!"));
    @Override
    public @NotNull ItemStack parse(@NotNull StringReader reader) throws CommandSyntaxException {
        ItemStack itemStack = getNativeType().parse(reader);
        if (itemStack.getType() == Material.AIR){
            throw ERROR_ITEM_AIR.create();
        }
        return itemStack;
    }

    @Override
    public @NotNull ArgumentType<ItemStack> getNativeType() {
        return ArgumentTypes.itemStack();
    }
}
