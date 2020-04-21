package red.mohist.inventory;

import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.inventory.ItemStack;
import red.mohist.api.ItemAPI;

/**
 * @author Mgazul
 * @date 2020/4/21 14:15
 */
public class ItemCap implements Cloneable{

    private NBTTagCompound capNBT;

    public ItemCap(NBTTagCompound capNBT) {
        this.capNBT = capNBT;
    }

    public NBTTagCompound getCapNBT() {
        return capNBT.copy();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ItemCap)) {
            return false;
        }

        return this.capNBT.equals(((ItemCap) obj).capNBT);
    }

    @Override
    public int hashCode() {
        return this.capNBT.hashCode();
    }

    @Override
    public ItemCap clone() {
        try {
            return (ItemCap) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public static void put(net.minecraft.item.ItemStack nmsItemStack, ItemStack itemStack) {
        if (nmsItemStack != null && nmsItemStack.capabilities != null) {
            NBTTagCompound capNBT = nmsItemStack.capabilities.serializeNBT();
            if (capNBT != null && !capNBT.hasNoTags()) {
                itemStack.setItemCapNBT(new ItemCap(capNBT));
            }
        }
    }

    public String serialize() {
        return ItemAPI.serializeNBT(this.capNBT);
    }

    public static ItemCap deserialize(String serializeNBT) {
        return new ItemCap(ItemAPI.deserializeNBT(serializeNBT));
    }
}
