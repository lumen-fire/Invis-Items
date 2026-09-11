package me.lumen.invisItems.commands.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.lumen.invisItems.InvisItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BrigadierInvisItemCommand {
    private static final SimpleCommandExceptionType NOT_PLAYER = new SimpleCommandExceptionType(new LiteralMessage("This is a player only action!"));
    private static final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType(new LiteralMessage("No players found!"));
    private static final DynamicCommandExceptionType NO_ITEM_IN_SLOT = new DynamicCommandExceptionType(object ->
            new LiteralMessage("You have no item in slot " + object));
    private static final DynamicCommandExceptionType SLOT_NOT_EXISTS = new DynamicCommandExceptionType(object ->
            new LiteralMessage("You do not have slot " + object));
    private static final Dynamic2CommandExceptionType NO_ENTITIES_OPERATED_ON = new Dynamic2CommandExceptionType((a, b) ->
            new LiteralMessage("No entities were found that could have invisibility " + a + " slot " + b));

    public static void register(@NotNull JavaPlugin plugin){
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(COMMAND, "Give and apply items with invisibility")
        );
    }

    private static final ArgumentBuilder<CommandSourceStack, ?> INVIS_APPLY_NODE = SlotArgumentWrapper.create()
            .base(Commands.literal("apply")
                    .requires(Commands.restricted(commandSourceStack -> commandSourceStack.getSender().hasPermission("invisitems.apply")))
                    .executes(context -> {
                        if (!(context.getSource().getExecutor() instanceof Player player)) throw NOT_PLAYER.create();
                        ItemStack hand = getItemInSlot(player, EquipmentSlot.HAND);
                        if (InvisItems.getInstance().hasInvisibility(hand)) {
                            throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
                                    .serialize(Component.empty().append(hand.displayName()).append(Component.text(" already has invisibility", NamedTextColor.RED)))).create();
                        }
                        InvisItems.getInstance().applyInvisibility(hand);
                        sendApplyMessage(player, hand);
                        return Command.SINGLE_SUCCESS;
                    })
            )
            .execution((context, slot) -> {
                if (!(context.getSource().getExecutor() instanceof Player player)) throw NOT_PLAYER.create();
                ItemStack itemStack = getItemInSlot(player, slot);
                if (InvisItems.getInstance().hasInvisibility(itemStack)) {
                    throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
                            .serialize(Component.empty().append(itemStack.displayName()).append(Component.text(" already has invisibility", NamedTextColor.RED)))).create();
                }
                InvisItems.getInstance().applyInvisibility(itemStack);
                sendApplyMessage(player, itemStack);
                return Command.SINGLE_SUCCESS;
            })
            .then(slot ->
                    Commands.argument("entities", ArgumentTypes.entities())
                            .executes(context -> {
                                List<Entity> entities = context.getArgument("entities", EntitySelectorArgumentResolver.class).resolve(context.getSource());
                                int amount = InvisItems.getInstance().applyInvisibility(entities, slot);
                                if (amount == 0){
                                    throw NO_ENTITIES_OPERATED_ON.create("applied to", slot.name().toLowerCase());
                                }
                                String message = InvisItems.getInstance().getMessage("apply-to-entities")
                                        .replace("%amount%", Integer.toString(amount))
                                        .replace("%slot%", slot.name().toLowerCase());
                                context.getSource().getSender().sendMessage(Component.text(message, NamedTextColor.GREEN));
                                return amount;
                            })
            )
            .build();

    private static final ArgumentBuilder<CommandSourceStack, ?> REMOVE_INVIS_NODE = SlotArgumentWrapper.create()
            .base(Commands.literal("remove")
                    .requires(Commands.restricted(commandSourceStack -> commandSourceStack.getSender().hasPermission("invisitems.remove")))
                    .executes(context -> removeInvisibility(context, EquipmentSlot.HAND))
            )
            .execution(BrigadierInvisItemCommand::removeInvisibility)
            .then(slot -> Commands.argument("entities", ArgumentTypes.entities())
                    .executes(context -> {
                        List<Entity> entities = context.getArgument("entities", EntitySelectorArgumentResolver.class).resolve(context.getSource());
                        int amount = InvisItems.getInstance().removeInvisibility(entities, slot);
                        if (amount == 0){
                            throw NO_ENTITIES_OPERATED_ON.create("removed from", slot.name().toLowerCase());
                        }
                        String message = InvisItems.getInstance().getMessage("remove-from-entities")
                                .replace("%amount%", Integer.toString(amount))
                                .replace("%slot%", slot.name().toLowerCase());
                        context.getSource().getSender().sendMessage(Component.text(message, NamedTextColor.YELLOW));
                        return amount;
                    })
            )
            .build();

    private static final ArgumentBuilder<CommandSourceStack, ?> REPLACE_INVIS_ITEM_NODE = SlotArgumentWrapper.create()
            .base(Commands.literal("replace").requires(Commands.restricted(source -> source.getSender().hasPermission("invisitems.replace"))))
            .then(slot ->
                    Commands.argument("item", ItemStackArg.ARGUMENT)
                            .executes(context -> {
                                ItemStack itemStack = context.getArgument("item", ItemStack.class);
                                InvisItems.getInstance().applyInvisibility(itemStack);
                                if (!(context.getSource().getExecutor() instanceof Player player)) throw NOT_PLAYER.create();
                                if (!player.canUseEquipmentSlot(slot)) throw SLOT_NOT_EXISTS.create(slot.name().toLowerCase());
                                player.getInventory().setItem(slot, itemStack);
                                String message = InvisItems.getInstance().getMessage("set-slot-to-item").replace("%slot%", slot.name().toLowerCase());
                                Component component = Component.text(message, NamedTextColor.GREEN)
                                        .replaceText(TextReplacementConfig.builder().matchLiteral("%item%").replacement(itemStack.displayName()).build());
                                player.sendMessage(component);
                                return Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.argument("entities", ArgumentTypes.entities())
                                    .executes(context -> {
                                        ItemStack itemStack = context.getArgument("item", ItemStack.class);
                                        InvisItems.getInstance().applyInvisibility(itemStack);
                                        List<Entity> entities = context.getArgument("entities", EntitySelectorArgumentResolver.class).resolve(context.getSource());
                                        int i = InvisItems.setItem(slot, itemStack, entities);
                                        if (i == 0){
                                            throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
                                                    .serialize(Component.text("No entities were found that could have slot " + slot.name().toLowerCase() + " replaced with invisible ").append(itemStack.displayName()))).create();
                                        }
                                        String messageRaw = InvisItems.getInstance().getMessage("set-entity-slot-to-item")
                                                .replace("%amount%", Integer.toString(i))
                                                .replace("%slot%", slot.name().toLowerCase());
                                        Component message = Component.text(messageRaw, NamedTextColor.YELLOW)
                                                .replaceText(TextReplacementConfig.builder().matchLiteral("%item%").replacement(itemStack.displayName()).build());
                                        context.getSource().getSender().sendMessage(message);
                                        return i;
                                    })
                            )
            )
            .build();

    private static final LiteralCommandNode<CommandSourceStack> COMMAND = Commands.literal("invisitems")
            .requires(Commands.restricted(commandSourceStack -> commandSourceStack.getSender().hasPermission("invisitems.command")))
            .then(Commands.literal("give")
                    .requires(Commands.restricted(commandSourceStack -> commandSourceStack.getSender().hasPermission("invisitems.give")))
                    .then(Commands.argument("item", ItemStackArg.ARGUMENT)
                            .executes(context -> giveInvisItem(context, 1))
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                    .suggests((context, builder) -> {
                                        builder.suggest("1");
                                        builder.suggest("16");
                                        builder.suggest("64");
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> giveInvisItem(context, IntegerArgumentType.getInteger(context, "amount")))
                                    .then(Commands.argument("players", ArgumentTypes.players())
                                            .executes(BrigadierInvisItemCommand::giveInvisItemOthers)
                                    )
                            )
                    )
            )
            .then(INVIS_APPLY_NODE)
            .then(REMOVE_INVIS_NODE)
            .then(REPLACE_INVIS_ITEM_NODE)
            .then(Commands.literal("version")
                    .requires(Commands.restricted(commandSourceStack -> commandSourceStack.getSender().hasPermission("invisitems.version")))
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        String version = InvisItems.getInstance().getPluginMeta().getVersion();
                        String message = InvisItems.getInstance().getMessage("version").replace("%version%", version);
                        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
                        return Command.SINGLE_SUCCESS;
                    })
            )
            .then(Commands.literal("reload")
                    .requires(Commands.restricted(commandSourceStack -> commandSourceStack.getSender().hasPermission("invisitems.reload")))
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        InvisItems.getInstance().reloadConfig();
                        sender.sendMessage(Component.text(InvisItems.getInstance().getMessage("reload"), NamedTextColor.YELLOW));
                        return Command.SINGLE_SUCCESS;
                    })
            )
            .build();

    private static int giveInvisItem(@NotNull CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        List<ItemStack> itemStacks = getInvisItemArg(context, amount);
        if (!(context.getSource().getExecutor() instanceof Player player)) throw NOT_PLAYER.create();
        player.give(itemStacks);
        player.sendMessage(getReceiveItemMessage(itemStacks));
        return Command.SINGLE_SUCCESS;
    }

    private static int giveInvisItemOthers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        List<ItemStack> itemStacks = getInvisItemArg(context, amount);
        List<Player> players = context.getArgument("players", PlayerSelectorArgumentResolver.class).resolve(context.getSource());
        if (players.isEmpty()){
            throw NO_PLAYERS_FOUND.create();
        }
        CommandSender sender = context.getSource().getSender();
        Component baseGiveMessage = Component.text(InvisItems.getInstance().getMessage("give-player-item")
                        .replace("%amount%", Integer.toString(amount)), NamedTextColor.GREEN)
                .replaceText(TextReplacementConfig.builder().matchLiteral("%item%").replacement(itemStacks.getFirst().displayName()).build());
        for (Player player : players){
            player.give(itemStacks);
            sender.sendMessage(baseGiveMessage.replaceText(TextReplacementConfig.builder()
                    .matchLiteral("%player%")
                    .replacement(player.displayName())
                    .build()));
        }
        return players.size();
    }

    private static @NotNull List<ItemStack> getInvisItemArg(@NotNull CommandContext<CommandSourceStack> context, int amount){
        ItemStack itemStack = context.getArgument("item", ItemStack.class);
        InvisItems.getInstance().applyInvisibility(itemStack);
        List<ItemStack> itemStacks = new ArrayList<>();
        for (int i = 0; i < amount; i++){
            itemStacks.add(itemStack);
        }

        return itemStacks;
    }

    private static @NotNull Component getReceiveItemMessage(@NotNull List<ItemStack> itemStacks){
        return Component.text(InvisItems.getInstance().getMessage("receive-item").replace("%amount%", Integer.toString(itemStacks.size())), NamedTextColor.GREEN)
                .replaceText(TextReplacementConfig.builder().matchLiteral("%item%").replacement(itemStacks.getFirst().displayName()).build());
    }

    private static void sendApplyMessage(@NotNull Player player, @NotNull ItemStack itemStack){
        Component message = Component.text(InvisItems.getInstance().getMessage("apply-to-item"), NamedTextColor.GREEN)
                .replaceText(TextReplacementConfig.builder().matchLiteral("%item%").replacement(itemStack.displayName()).build());
        player.sendMessage(message);
    }

    private static @NotNull ItemStack getItemInSlot(@NotNull Player player, EquipmentSlot slot) throws CommandSyntaxException {
        ItemStack itemStack;
        try {
            itemStack = player.getInventory().getItem(slot);
        } catch (IllegalArgumentException e) {
            throw SLOT_NOT_EXISTS.create(slot.name().toLowerCase());
        }
        if (itemStack.isEmpty()) throw NO_ITEM_IN_SLOT.create(slot.name().toLowerCase());
        return itemStack;
    }

    private static int removeInvisibility(@NotNull CommandContext<CommandSourceStack> context, EquipmentSlot slot) throws CommandSyntaxException {
        if (!(context.getSource().getExecutor() instanceof Player player)) throw NOT_PLAYER.create();
        ItemStack itemStack = getItemInSlot(player, slot);
        if (!InvisItems.getInstance().hasInvisibility(itemStack)) {
            throw new SimpleCommandExceptionType(MessageComponentSerializer.message()
                    .serialize(Component.empty().append(itemStack.displayName()).append(Component.text(" does not have invisibility", NamedTextColor.RED)))).create();
        }
        InvisItems.getInstance().removeInvisibility(itemStack);
        Component message = Component.text(InvisItems.getInstance().getMessage("remove-from-item"), NamedTextColor.YELLOW)
                .replaceText(TextReplacementConfig.builder().matchLiteral("%item%").replacement(itemStack.displayName()).build());
        player.sendMessage(message);
        return Command.SINGLE_SUCCESS;
    }
}
