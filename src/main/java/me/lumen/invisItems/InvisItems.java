package me.lumen.invisItems;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.lumen.invisItems.commands.brigadier.BrigadierInvisItemCommand;
import me.lumen.invisItems.commands.bukkit.BukkitInvisItemCommand;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class InvisItems extends JavaPlugin {
    private static InvisItems instance;
    private final NamespacedKey INVIS_ITEM_KEY = new NamespacedKey(this, "invisible_item");
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        PacketEvents.getAPI().getEventManager().registerListener(PacketEventListener.LISTENER, PacketListenerPriority.NORMAL);
        if (hasPaperCommands()){
            BrigadierInvisItemCommand.register(this);
        } else {
            BukkitInvisItemCommand.COMMAND.register(this);
        }
    }

    public String getMessage(String key){
        return getConfig().getString("messages." + key);
    }

    public void applyInvisibility(@NotNull ItemStack itemStack){
        ItemMeta meta = itemStack.getItemMeta();
        //simply skip if null - should only be null in edge cases such as air & stuff
        if (meta == null) return;
        meta.getPersistentDataContainer().set(INVIS_ITEM_KEY, PersistentDataType.BOOLEAN, true);
        itemStack.setItemMeta(meta);
    }

    public void removeInvisibility(@NotNull ItemStack itemStack){
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().remove(INVIS_ITEM_KEY);
        itemStack.setItemMeta(meta);
    }

    public boolean hasInvisibility(@NotNull ItemStack itemStack){
        if (!itemStack.hasItemMeta()) return false;
        return itemStack.getItemMeta().getPersistentDataContainer().has(INVIS_ITEM_KEY);
    }

    /**
     *
     * @param entities the entities to apply invis item to - only ones that are a {@link LivingEntity} will actually have it applied
     * @param slot the EquipmentSlot to apply to, e.g. boots, main hand, saddle
     * @return the number of the entities it was applied to
     */
    public int applyInvisibility(@NotNull List<Entity> entities, EquipmentSlot slot){
        return invisOperation(entities, slot, itemStack -> !hasInvisibility(itemStack), this::applyInvisibility);
    }

    /**
     *
     * @param entities the entities to remove invisible items from - only ones that are a {@link LivingEntity} will actually have it removed
     * @param slot the EquipmentSlot to removed from, e.g. boots, main hand, saddle
     * @return the number of the entities invis was removed from
     */
    public int removeInvisibility(@NotNull List<Entity> entities, EquipmentSlot slot){
        return invisOperation(entities, slot, this::hasInvisibility, this::removeInvisibility);
    }

    /**
     *
     * @param entities the entities to operate on - only living entities will work
     * @param slot the slot of the entities to use
     * @param applyPredicate a predicate that must succeed for the operation to happen -  if it fails the entity will be skipped
     * @param operation the thing to do to the item
     * @return the amount of entities the operation was applied to successfully
     */
    private int invisOperation(@NotNull List<Entity> entities, EquipmentSlot slot, Predicate<ItemStack> applyPredicate, Consumer<ItemStack> operation){
        List<LivingEntity> livingEntities = toLiving(entities);
        int amountChanged = 0;

        for (LivingEntity entity : livingEntities){
            EntityEquipment equipment = entity.getEquipment();
            if (equipment == null) continue;
            ItemStack item;
            try {
                item = entity.getEquipment().getItem(slot);
            } catch (IllegalArgumentException e){ continue; }
            if (isEmpty(item)) continue;
            //skip if predicate fails
            if (!applyPredicate.test(item)) continue;
            //run operation
            operation.accept(item);
            entity.getEquipment().setItem(slot, item);
            amountChanged++;
        }

        return amountChanged;
    }

    public static int setItem(EquipmentSlot slot, ItemStack itemStack, List<Entity> entities){
        List<LivingEntity> livingEntities = InvisItems.toLiving(entities);
        int i = 0;
        for (LivingEntity entity : livingEntities){
            EntityEquipment equipment = entity.getEquipment();
            if (equipment == null) continue;
            try {
                entity.getEquipment().setItem(slot, itemStack);
                i++;
            } catch (IllegalArgumentException ignored) {}
        }
        return i;
    }

    public static List<LivingEntity> toLiving(@NotNull List<Entity> entities){
        return entities.stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .toList();
    }

    /**
     * Check if an item is empty, as ItemStack#isEmpty does not work on spigot
     * @param itemStack the item
     * @return true if it is null, air or has stack size 0
     */
    public static boolean isEmpty(ItemStack itemStack){
        return itemStack == null || itemStack.getAmount() == 0 || itemStack.getType() == Material.AIR;
    }

    public static InvisItems getInstance(){
        return instance;
    }

    private static boolean hasPaperCommands(){
        try {
            Class.forName("io.papermc.paper.command.brigadier.Commands");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
