package org.bukkit.craftbukkit.entity;

import net.minecraft.server.EntitySlime;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;

public class CraftSlime extends CraftMob implements Slime {

    public CraftSlime(CraftServer server, EntitySlime entity) {
        super(server, entity);
    }

    public int getSize() {
        return getHandle().getSize();
    }

    public void setSize(int size) {
        getHandle().setSize(size, true);
    }

    @Override
    public EntitySlime getHandle() {
        return (EntitySlime) entity;
    }

    @Override
    public String toString() {
        return "CraftSlime";
    }

    public EntityType getType() {
        return EntityType.SLIME;
    }

    // Paper start
    public boolean canWander() {
        return getHandle().canWander();
    }

    public void setWander(boolean canWander) {
        getHandle().setWander(canWander);
    }
    // Paper end
}
