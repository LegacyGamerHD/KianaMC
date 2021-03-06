package net.minecraft.server;

public enum EnumCreatureType {

    MONSTER(IMonster.class, 70, false, false), CREATURE(EntityAnimal.class, 10, true, true), AMBIENT(EntityAmbient.class, 15, true, false), WATER_CREATURE(EntityWaterAnimal.class, 15, true, false);

    private final Class<? extends IAnimal> e;
    private final int f;
    private final boolean g;
    private final boolean h;

    private EnumCreatureType(Class oclass, int i, boolean flag, boolean flag1) {
        this.e = oclass;
        this.f = i;
        this.g = flag;
        this.h = flag1;
    }

    public boolean matches(Entity entity) { return innerClass().isAssignableFrom(entity.getClass()); } // Paper
    public Class<? extends IAnimal> innerClass() { return this.a(); } // Paper - OBFHELPER
    public Class<? extends IAnimal> a() {
        return this.e;
    }

    public int spawnLimit() { return this.b(); } // Akarin
    public int b() {
        return this.f;
    }

    public boolean passive() { return c(); } // Akarin
    public boolean c() {
        return this.g;
    }

    public boolean rare() { return d(); } // Akarin
    public boolean d() {
        return this.h;
    }
}
