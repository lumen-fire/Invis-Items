package me.lumen.invisItems.commands.bukkit;

import me.lumen.invisItems.InvisItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("deprecation")
public class BukkitInvisItemCommand implements CommandExecutor, TabCompleter {
    public static BukkitInvisItemCommand COMMAND = new BukkitInvisItemCommand();
    private BukkitInvisItemCommand(){}

    public void register(@NotNull JavaPlugin plugin){
        PluginCommand command = plugin.getCommand("invisitems");
        assert command != null;
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0){
            sendUsageMessage(sender);
            return false;
        }
        String firstArg = args[0];
        switch (firstArg){
            case "apply" -> {
                if (args.length > 3 || !sender.hasPermission("invisitems.apply")){
                    sendUsageMessage(sender);
                    return false;
                }
                EquipmentSlot slot = getSlot(args, sender);
                if (slot == null) return false;
                if (args.length == 3){
                    List<Entity> entities = Bukkit.selectEntities(sender, args[2]);
                    int amount = InvisItems.getInstance().applyInvisibility(entities, slot);
                    if (amount == 0){
                        sender.sendMessage(ChatColor.RED + "No entities were found that could have invisibility applied to slot " + slot.name().toLowerCase());
                        return false;
                    }
                    String message = ChatColor.GREEN + InvisItems.getInstance().getMessage("apply-to-entities")
                            .replace("%amount%", Integer.toString(amount))
                            .replace("%slot%", slot.name().toLowerCase());
                    sender.sendMessage(message);

                } else {
                    ItemStack itemStack = getItem(sender, slot, it -> !InvisItems.getInstance().hasInvisibility(it), s -> s + " already has invisibility");
                    if (itemStack == null) return false;

                    InvisItems.getInstance().applyInvisibility(itemStack);
                    String message = ChatColor.GREEN + InvisItems.getInstance().getMessage("apply-to-item")
                            .replace("%item%", itemStack.getType().getKey().toString());
                    sender.sendMessage(message);
                }
                return true;
            }
            case "remove" -> {
                if (args.length > 3 || !sender.hasPermission("invisitems.remove")){
                    sendUsageMessage(sender);
                    return false;
                }
                EquipmentSlot slot = getSlot(args, sender);
                if (slot == null) return false;
                if (args.length == 3){
                    List<Entity> entities = Bukkit.selectEntities(sender, args[2]);
                    int amount = InvisItems.getInstance().removeInvisibility(entities, slot);
                    if (amount == 0){
                        sender.sendMessage(ChatColor.RED + "No entities were found that could have invisibility removed from slot " + slot.name().toLowerCase());
                        return false;
                    }
                    String message = ChatColor.YELLOW + InvisItems.getInstance().getMessage("remove-from-entities")
                            .replace("%amount%", Integer.toString(amount))
                            .replace("%slot%", slot.name().toLowerCase());
                    sender.sendMessage(message);
                } else {
                    ItemStack itemStack = getItem(sender, slot, it -> InvisItems.getInstance().hasInvisibility(it), s -> s + " does not have invisibility");
                    if (itemStack == null) return false;

                    InvisItems.getInstance().removeInvisibility(itemStack);
                    String message = ChatColor.YELLOW + InvisItems.getInstance().getMessage("remove-from-item")
                            .replace("%item%", itemStack.getType().getKey().toString());
                    sender.sendMessage(message);
                }
                return true;
            }
            case "give" -> {
                if (args.length == 1 || args.length > 4 || !sender.hasPermission("invisitems.give")){
                    sendUsageMessage(sender);
                    return false;
                }
                String itemArg = args[1];
                ItemStack itemStack = getItem(itemArg, sender);
                if (itemStack == null) return false;
                InvisItems.getInstance().applyInvisibility(itemStack);

                int amount = 1;
                if (args.length >= 3){
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sendUsageMessage(sender);
                        return false;
                    }
                    if (amount > 1000 || amount < 1){
                        sendUsageMessage(sender);
                        return false;
                    }
                }

                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < amount; i++){
                    items.add(itemStack);
                }

                if (args.length == 4){
                    try {
                        List<Entity> entities = Bukkit.selectEntities(sender, args[3]);
                        //give an error if any are not players
                        if (entities.stream().anyMatch(entity -> !(entity instanceof Player))) {
                            sendUsageMessage(sender);
                            return false;
                        }
                        List<Player> players = entities.stream().map(entity -> (Player) entity).toList();
                        String message = ChatColor.GREEN + InvisItems.getInstance().getMessage("give-player-item")
                                .replace("%amount%", Integer.toString(amount)).replace("%item%", itemStack.getType().getKey().toString());
                        for (Player player : players){
                            giveItems(player, items);
                            player.sendMessage(message.replace("%player%", player.getName()));
                        }
                    } catch (IllegalArgumentException e){
                        sendUsageMessage(sender);
                        return false;
                    }
                } else {
                    if (!(sender instanceof Player player)){
                        sendPlayerActionMessage(sender);
                        return false;
                    }
                    giveItems(player, items);
                    String message = ChatColor.GREEN + InvisItems.getInstance().getMessage("receive-item")
                            .replace("%amount%", Integer.toString(amount)).replace("%item%", itemStack.getType().getKey().toString());
                    player.sendMessage(message);
                }
                return true;
            }
            case "replace" -> {
                if (args.length < 3 || args.length > 4 || !sender.hasPermission("invisitems.replace")){
                    sendUsageMessage(sender);
                    return false;
                }
                EquipmentSlot slot = getSlot(args, sender);
                if (slot == null) return false;

                String itemArg = args[2];
                ItemStack itemStack = getItem(itemArg, sender);
                if (itemStack == null) return false;
                InvisItems.getInstance().applyInvisibility(itemStack);

                if (args.length == 4){
                    List<Entity> entities = Bukkit.selectEntities(sender, args[3]);
                    int i = InvisItems.setItem(slot, itemStack, entities);
                    if (i == 0){
                        sender.sendMessage(ChatColor.RED + "No entities were found that could have slot " + slot.name().toLowerCase() + " replaced with invisible " + itemStack.getType().getKey());
                        return false;
                    }
                    String message = ChatColor.GREEN + InvisItems.getInstance().getMessage("set-entity-slot-to-item")
                            .replace("%item%", itemStack.getType().getKey().toString())
                            .replace("%amount%", Integer.toString(i))
                            .replace("%slot%", slot.name().toLowerCase());
                    sender.sendMessage(message);
                } else {
                    if (!(sender instanceof Player player)) {
                        sendPlayerActionMessage(sender);
                        return false;
                    }
                    try {
                        player.getInventory().setItem(slot, itemStack);
                    } catch (IllegalArgumentException e){
                        player.sendMessage(ChatColor.RED + "You do not have slot " + slot.name().toLowerCase());
                        return false;
                    }
                    String message = ChatColor.GREEN + InvisItems.getInstance().getMessage("set-slot-to-item")
                            .replace("%slot%", slot.name().toLowerCase())
                            .replace("%item%", itemStack.getType().getKey().toString());
                    player.sendMessage(message);
                }
                return true;
            }
            case "version" -> {
                if (args.length != 1 || !sender.hasPermission("invisitems.version")){
                    sendUsageMessage(sender);
                    return false;
                }
                String version = InvisItems.getInstance().getDescription().getVersion();
                String message = ChatColor.GREEN + InvisItems.getInstance().getMessage("version").replace("%version%", version);
                sender.sendMessage(message);
                return true;
            }
            case "reload" -> {
                if (args.length != 1 || !sender.hasPermission("invisitems.reload")){
                    sendUsageMessage(sender);
                    return false;
                }
                InvisItems.getInstance().reloadConfig();
                sender.sendMessage(ChatColor.YELLOW + InvisItems.getInstance().getMessage("reload"));
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1){
            return getSubCommands(sender);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "give" -> {
                switch (args.length) {
                    //item
                    case 2 -> {
                        return suggestItems(args[1]);
                    }
                    //amount
                    case 3 -> {
                        return List.of("1", "16", "64");
                    }
                    //players
                    case 4 -> {
                        String remaining = args[3];
                        List<String> suggestions = new ArrayList<>(List.of("@a", "@p", "@r"));
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (!player.getName().toLowerCase().startsWith(remaining.toLowerCase())) continue;
                            suggestions.add(player.getName());
                        }
                        return suggestions;
                    }
                }
            }
            case "apply", "remove" -> {
                switch (args.length) {
                    //slot
                    case 2 -> {
                        return suggestSlots(args[1]);
                    }
                    //entities
                    case 3 -> {
                        return suggestEntities(args[2]);
                    }
                }
            }
            case "replace" -> {
                switch (args.length) {
                    //slot
                    case 2 -> {
                        return suggestSlots(args[1]);
                    }
                    //item
                    case 3 -> {
                        return suggestItems(args[2]);
                    }
                    //entities
                    case 4 -> {
                        return suggestEntities(args[3]);
                    }
                }
            }
        }
        return List.of();
    }

    private static void giveItems(Player player, @NotNull List<ItemStack> items){
        for (ItemStack item : items){
            HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(item);
            if (!remainder.isEmpty()){
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }
    }

    private static @Nullable ItemStack getItem(CommandSender sender, EquipmentSlot slot, Predicate<ItemStack> condition, Function<String, String> errMessage){
        if (!(sender instanceof Player player)) {
            sendPlayerActionMessage(sender);
            return null;
        }

        ItemStack itemStack;
        try {
            itemStack = player.getInventory().getItem(slot);
        } catch (IllegalArgumentException e){
            player.sendMessage(ChatColor.RED + "You do not have slot " + slot.name().toLowerCase());
            return null;
        }

        if (InvisItems.isEmpty(itemStack)){
            player.sendMessage(ChatColor.RED + "You have no item in slot " + slot.name().toLowerCase());
            return null;
        }

        if (!condition.test(itemStack)){
            player.sendMessage(ChatColor.RED + errMessage.apply(itemStack.getType().getKey().toString()));
            return null;
        }

        return itemStack;
    }

    private static @Nullable ItemStack getItem(String itemArg, CommandSender sender){
        try {
            ItemStack itemStack = Bukkit.getItemFactory().createItemStack(itemArg);
            if (itemStack.getType() == Material.AIR) {
                sender.sendMessage(ChatColor.RED + "Item cannot be air!");
                return null;
            }
            return itemStack;
        } catch (IllegalArgumentException e){
            sendUsageMessage(sender);
            return null;
        }
    }

    private static @Nullable EquipmentSlot getSlot(String @NotNull [] args, CommandSender sender){
        EquipmentSlot slot = EquipmentSlot.HAND;
        if (args.length >= 2){
            String slotName = args[1];
            try {
                slot = EquipmentSlot.valueOf(slotName.toUpperCase());
            } catch (IllegalArgumentException e) {
                sendUsageMessage(sender);
                return null;
            }
        }
        return slot;
    }

    private static @NotNull List<String> suggestSlots(String arg){
        List<String> suggestions = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()){
            if (!slot.name().startsWith(arg.toUpperCase())) continue;
            suggestions.add(slot.name().toLowerCase());
        }
        return suggestions;
    }

    private static @NotNull List<String> suggestItems(String arg){
        return Registry.ITEM.stream()
                .filter(itemType -> itemType.getKey().toString().startsWith(arg.toLowerCase()) || itemType.getKey().getKey().startsWith(arg.toLowerCase()))
                .map(itemType -> itemType.getKey().toString())
                .toList();
    }

    private static @NotNull List<String> suggestEntities(String arg){
        List<String> suggestions = new ArrayList<>(List.of("@a", "@p", "@r", "@n", "@e", "@s"));
        for (Player player : Bukkit.getOnlinePlayers()){
            if (!player.getName().toLowerCase().startsWith(arg.toLowerCase())) continue;
            suggestions.add(player.getName());
        }
        return suggestions;
    }

    private static void sendUsageMessage(CommandSender sender){
        String message = ChatColor.RED + "Usage: /invisitems (";
        message += String.join("|", getSubCommands(sender));
        message += ") [args...]";
        sender.sendMessage(message);
    }

    private static void sendPlayerActionMessage(@NotNull CommandSender sender){
        sender.sendMessage(ChatColor.RED + "This is a player only action!");
    }

    private static @NotNull List<String> getSubCommands(@NotNull CommandSender sender){
        List<String> subCommands = new ArrayList<>();
        if (sender.hasPermission("invisitems.give")){
            subCommands.add("give");
        }
        if (sender.hasPermission("invisitems.apply")){
            subCommands.add("apply");
        }
        if (sender.hasPermission("invisitems.remove")){
            subCommands.add("remove");
        }
        if (sender.hasPermission("invisitems.replace")){
            subCommands.add("replace");
        }
        if (sender.hasPermission("invisitems.reload")){
            subCommands.add("reload");
        }
        if (sender.hasPermission("invisitems.version")){
            subCommands.add("version");
        }
        return subCommands;
    }
}
