package me.lumen.invisItems;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PacketEventListener implements PacketListener {
    public static PacketEventListener LISTENER = new PacketEventListener();
    private PacketEventListener(){}
    @Override
    public void onPacketSend(@NotNull PacketSendEvent event){
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT){
            WrapperPlayServerEntityEquipment sendEquipment = new WrapperPlayServerEntityEquipment(event);

            boolean changed = false;
            int i = 0;
            for (Equipment equipment : sendEquipment.getEquipment()){
                i++;
                ItemStack itemStack = SpigotConversionUtil.toBukkitItemStack(equipment.getItem());
                if (itemStack == null) continue;
                if (InvisItems.getInstance().hasInvisibility(itemStack)){
                    changed = true;
                    Equipment air = new Equipment(equipment.getSlot(), com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY);
                    sendEquipment.getEquipment().set(i - 1, air);
                }
            }
            if (changed) {
                event.markForReEncode(true);
            }
        }
    }
}
