package net.minecraft.server;

import co.aikar.timings.Timings;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;

import io.akarin.server.core.AkarinGlobalConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// CraftBukkit start
import joptsimple.OptionSet;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.Main;
import org.bukkit.event.server.ServerLoadEvent;
// CraftBukkit end
import org.spigotmc.SlackActivityAccountant; // Spigot
import co.aikar.timings.MinecraftTimings; // Paper
import co.aikar.timings.ThreadAssertion;

public abstract class MinecraftServer implements IAsyncTaskHandler, IMojangStatistics, ICommandListener, Runnable {

    private static MinecraftServer SERVER; // Paper
    public static final Logger LOGGER = LogManager.getLogger();
    public static final File a = new File("usercache.json");
    public Convertable convertable;
    private final MojangStatisticsGenerator snooper = new MojangStatisticsGenerator("server", this, SystemUtils.getMonotonicMillis());
    public File universe;
    private final List<ITickable> k = Lists.newArrayList();
    public final MethodProfiler methodProfiler = new MethodProfiler();
    private ServerConnection serverConnection; // Spigot
    private final ServerPing m = new ServerPing();
    private final Random n = new Random();
    public final DataFixer dataConverterManager;
    private String serverIp;
    private int q = -1;
    public final Map<DimensionManager, WorldServer> worldServer = new com.destroystokyo.paper.PaperWorldMap(); // Paper;
    private PlayerList playerList;
    private boolean isRunning = true;
    private boolean isRestarting = false; // Paper - flag to signify we're attempting to restart
    private boolean isStopped;
    private int ticks; public int currentTick() { return this.ticks; } // Akarin - OBFHELPER
    protected final Proxy c;
    private IChatBaseComponent w;
    private int x;
    private boolean onlineMode;
    private boolean z;
    private boolean spawnAnimals;
    private boolean spawnNPCs;
    private boolean pvpMode;
    private boolean allowFlight;
    private String motd;
    private int F;
    private int G;
    public final long[] d = new long[100];
    protected final Map<DimensionManager, long[]> e = com.koloboke.collect.map.hash.HashObjObjMaps.getDefaultFactory().withNullKeyAllowed(true).withKeyEquivalence(com.koloboke.collect.Equivalence.identity()).newUpdatableMap(); // Akarin - koloboke
    private KeyPair H;
    private String I;
    private String J;
    private boolean demoMode;
    private boolean M;
    private String N = "";
    private String O = "";
    private boolean P;
    private long lastOverloadTime;
    private IChatBaseComponent R;
    private boolean S;
    private boolean T;
    private final YggdrasilAuthenticationService U;
    private final MinecraftSessionService V;
    private final GameProfileRepository W;
    private final UserCache X;
    private long Y;
    protected final Queue<FutureTask<?>> f = Queues.newConcurrentLinkedQueue();
    private Thread serverThread;
    private long nextTick = SystemUtils.getMonotonicMillis();
    private final IReloadableResourceManager ac;
    private final ResourcePackRepository<ResourcePackLoader> resourcePackRepository;
    private ResourcePackSourceFolder resourcePackFolder;
    public CommandDispatcher commandDispatcher;
    private final CraftingManager ag;
    private final TagRegistry ah;
    private final ScoreboardServer ai;
    private final BossBattleCustomData aj;
    private final LootTableRegistry ak;
    private final AdvancementDataWorld al;
    private final CustomFunctionData am;
    private boolean an;
    private boolean forceUpgrade;
    private float ap;

    // CraftBukkit start
    public org.bukkit.craftbukkit.CraftServer server;
    public OptionSet options;
    public org.bukkit.command.ConsoleCommandSender console;
    public org.bukkit.command.RemoteConsoleCommandSender remoteConsole;
    //public ConsoleReader reader; // Paper
    public static int currentTick = 0; // Paper - Further improve tick loop
    public boolean serverAutoSave = false; // Paper
    public final Thread primaryThread;
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
    public int autosavePeriod;
    public File bukkitDataPackFolder;
    public CommandDispatcher vanillaCommandDispatcher;
    public List<ExpiringMap> expiringMaps = java.util.Collections.synchronizedList(new java.util.ArrayList<>()); // Paper
    // CraftBukkit end
    // Spigot start
    public static final int TPS = 20;
    public static final int TICK_TIME = 1000000000 / TPS;
    private static final int SAMPLE_INTERVAL = 20; // Paper
    public final double[] recentTps = new double[ 3 ];
    public final SlackActivityAccountant slackActivityAccountant = new SlackActivityAccountant();
    // Spigot end

    public MinecraftServer(OptionSet options, Proxy proxy, DataFixer datafixer, CommandDispatcher commanddispatcher, YggdrasilAuthenticationService yggdrasilauthenticationservice, MinecraftSessionService minecraftsessionservice, GameProfileRepository gameprofilerepository, UserCache usercache) {
        SERVER = this; // Paper - better singleton
        this.commandDispatcher = commanddispatcher; // CraftBukkit
        this.ac = new ResourceManager(EnumResourcePackType.SERVER_DATA);
        this.resourcePackRepository = new ResourcePackRepository<>(ResourcePackLoader::new);
        this.ag = new CraftingManager();
        this.ah = new TagRegistry();
        this.ai = new ScoreboardServer(this);
        this.aj = new BossBattleCustomData(this);
        this.ak = new LootTableRegistry();
        this.al = new AdvancementDataWorld();
        this.am = new CustomFunctionData(this);
        this.c = proxy;
        this.commandDispatcher = this.vanillaCommandDispatcher = commanddispatcher; // CraftBukkit
        this.U = yggdrasilauthenticationservice;
        this.V = minecraftsessionservice;
        this.W = gameprofilerepository;
        this.X = usercache;
        this.userCache = new AkarinUserCache(gameprofilerepository, usercache.file(), usercache.gson()); // Akarin
        // this.universe = file; // CraftBukkit
        // this.serverConnection = new ServerConnection(this); // CraftBukkit // Spigot
        // this.convertable = file == null ? null : new WorldLoaderServer(file.toPath(), file.toPath().resolve("../backups"), datafixer); // CraftBukkit - moved to DedicatedServer.init
        this.dataConverterManager = datafixer;
        this.ac.a((IResourcePackListener) this.ah);
        this.ac.a((IResourcePackListener) this.ag);
        this.ac.a((IResourcePackListener) this.ak);
        this.ac.a((IResourcePackListener) this.am);
        this.ac.a((IResourcePackListener) this.al);
        // CraftBukkit start
        this.options = options;
        // Paper start - Handled by TerminalConsoleAppender
        // Try to see if we're actually running in a terminal, disable jline if not
        /*
        if (System.console() == null && System.getProperty("jline.terminal") == null) {
            System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
            Main.useJline = false;
        }

        try {
            reader = new ConsoleReader(System.in, System.out);
            reader.setExpandEvents(false); // Avoid parsing exceptions for uncommonly used event designators
        } catch (Throwable e) {
            try {
                // Try again with jline disabled for Windows users without C++ 2008 Redistributable
                System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                System.setProperty("user.language", "en");
                Main.useJline = false;
                reader = new ConsoleReader(System.in, System.out);
                reader.setExpandEvents(false);
            } catch (IOException ex) {
                LOGGER.warn((String) null, ex);
            }
        }
        */
        // Paper end
        Runtime.getRuntime().addShutdownHook(new org.bukkit.craftbukkit.util.ServerShutdownThread(this));

        this.serverThread = primaryThread = new Thread(this, "Server thread"); // Moved from main
    }

    public abstract PropertyManager getPropertyManager();
    // CraftBukkit end

    public abstract boolean init() throws IOException;

    public void convertWorld(String s) {
        if (this.getConvertable().isConvertable(s)) {
            MinecraftServer.LOGGER.info("Converting map!");
            this.b((IChatBaseComponent) (new ChatMessage("menu.convertingLevel", new Object[0])));
            this.getConvertable().convert(s, new IProgressUpdate() {
                private long b = SystemUtils.getMonotonicMillis();

                public void a(IChatBaseComponent ichatbasecomponent) {}

                public void a(int i) {
                    if (SystemUtils.getMonotonicMillis() - this.b >= 1000L) {
                        this.b = SystemUtils.getMonotonicMillis();
                        MinecraftServer.LOGGER.info("Converting... {}%", i);
                    }

                }

                public void c(IChatBaseComponent ichatbasecomponent) {}
            });
        }

        if (this.forceUpgrade) {
            MinecraftServer.LOGGER.info("Forcing world upgrade! {}", s); // CraftBukkit
            WorldData worlddata = this.getConvertable().c(s); // CraftBukkit

            if (worlddata != null) {
                WorldUpgrader worldupgrader = new WorldUpgrader(s, this.getConvertable(), worlddata); // CraftBukkit
                IChatBaseComponent ichatbasecomponent = null;

                while (!worldupgrader.b()) {
                    IChatBaseComponent ichatbasecomponent1 = worldupgrader.g();

                    if (ichatbasecomponent != ichatbasecomponent1) {
                        ichatbasecomponent = ichatbasecomponent1;
                        MinecraftServer.LOGGER.info(worldupgrader.g().getString());
                    }

                    int i = worldupgrader.d();

                    if (i > 0) {
                        int j = worldupgrader.e() + worldupgrader.f();

                        MinecraftServer.LOGGER.info("{}% completed ({} / {} chunks)...", MathHelper.d((float) j / (float) i * 100.0F), j, i);
                    }

                    if (this.isStopped()) {
                        worldupgrader.a();
                    } else {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException interruptedexception) {
                            ;
                        }
                    }
                }
            }
        }

    }

    protected synchronized void b(IChatBaseComponent ichatbasecomponent) {
        this.R = ichatbasecomponent;
    }

    public void a(String s, String s1, long i, WorldType worldtype, JsonElement jsonelement) {
        // this.convertWorld(s); // CraftBukkit - moved down
        this.b((IChatBaseComponent) (new ChatMessage("menu.loadingLevel", new Object[0])));
        /* CraftBukkit start - Remove ticktime arrays and worldsettings
        IDataManager idatamanager = this.getConvertable().a(s, this);

        this.a(this.getWorld(), idatamanager);
        WorldData worlddata = idatamanager.getWorldData();
        WorldSettings worldsettings;

        if (worlddata == null) {
            if (this.L()) {
                worldsettings = DemoWorldServer.a;
            } else {
                worldsettings = new WorldSettings(i, this.getGamemode(), this.getGenerateStructures(), this.isHardcore(), worldtype);
                worldsettings.setGeneratorSettings(jsonelement);
                if (this.M) {
                    worldsettings.a();
                }
            }

            worlddata = new WorldData(worldsettings, s1);
        } else {
            worlddata.a(s1);
            worldsettings = new WorldSettings(worlddata);
        }

        this.a(idatamanager.getDirectory(), worlddata);
        */
        int worldCount = 3;

        for (int j = 0; j < worldCount; ++j) {
            WorldServer world;
            WorldData worlddata;
            byte dimension = 0;

            if (j == 1) {
                if (getAllowNether()) {
                    dimension = -1;
                } else {
                    continue;
                }
            }

            if (j == 2) {
                if (server.getAllowEnd()) {
                    dimension = 1;
                } else {
                    continue;
                }
            }

            String worldType = org.bukkit.World.Environment.getEnvironment(dimension).toString().toLowerCase();
            String name = (dimension == 0) ? s : s + "_" + worldType;
            this.convertWorld(name); // Run conversion now

            org.bukkit.generator.ChunkGenerator gen = this.server.getGenerator(name);
            WorldSettings worldsettings = new WorldSettings(com.destroystokyo.paper.PaperConfig.seedOverride.getOrDefault(name, i), this.getGamemode(), this.getGenerateStructures(), this.isHardcore(), worldtype); // Paper
            worldsettings.setGeneratorSettings(jsonelement);

            if (j == 0) {
                IDataManager idatamanager = new ServerNBTManager(server.getWorldContainer(), s1, this, this.dataConverterManager);
                worlddata = idatamanager.getWorldData();
                if (worlddata == null) {
                    worlddata = new WorldData(worldsettings, s1);
                }
                worlddata.checkName(s1); // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
                this.a(idatamanager.getDirectory(), worlddata);
                PersistentCollection persistentcollection = new PersistentCollection(idatamanager);

                if (this.L()) {
                    world = (WorldServer) (new DemoWorldServer(this, idatamanager, persistentcollection, worlddata, DimensionManager.OVERWORLD, this.methodProfiler)).i_();
                } else {
                    world = (WorldServer) (new WorldServer(this, idatamanager, persistentcollection, worlddata, DimensionManager.OVERWORLD, this.methodProfiler, org.bukkit.World.Environment.getEnvironment(dimension), gen)).i_();
                }

                world.a(worldsettings);
                this.server.scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(this, world.getScoreboard());
            } else {
                String dim = "DIM" + dimension;

                File newWorld = new File(new File(name), dim);
                File oldWorld = new File(new File(s), dim);
                File oldLevelDat = new File(new File(s), "level.dat"); // The data folders exist on first run as they are created in the PersistentCollection constructor above, but the level.dat won't

                if (!newWorld.isDirectory() && oldWorld.isDirectory() && oldLevelDat.isFile()) {
                    MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder required ----");
                    MinecraftServer.LOGGER.info("Unfortunately due to the way that Minecraft implemented multiworld support in 1.6, Bukkit requires that you move your " + worldType + " folder to a new location in order to operate correctly.");
                    MinecraftServer.LOGGER.info("We will move this folder for you, but it will mean that you need to move it back should you wish to stop using Bukkit in the future.");
                    MinecraftServer.LOGGER.info("Attempting to move " + oldWorld + " to " + newWorld + "...");

                    if (newWorld.exists()) {
                        MinecraftServer.LOGGER.warn("A file or folder already exists at " + newWorld + "!");
                        MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                    } else if (newWorld.getParentFile().mkdirs()) {
                        if (oldWorld.renameTo(newWorld)) {
                            MinecraftServer.LOGGER.info("Success! To restore " + worldType + " in the future, simply move " + newWorld + " to " + oldWorld);
                            // Migrate world data too.
                            try {
                                com.google.common.io.Files.copy(oldLevelDat, new File(new File(name), "level.dat"));
                                org.apache.commons.io.FileUtils.copyDirectory(new File(new File(s), "data"), new File(new File(name), "data"));
                            } catch (IOException exception) {
                                MinecraftServer.LOGGER.warn("Unable to migrate world data.");
                            }
                            MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder complete ----");
                        } else {
                            MinecraftServer.LOGGER.warn("Could not move folder " + oldWorld + " to " + newWorld + "!");
                            MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                        }
                    } else {
                        MinecraftServer.LOGGER.warn("Could not create path for " + newWorld + "!");
                        MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                    }
                }

                IDataManager idatamanager = new ServerNBTManager(server.getWorldContainer(), name, this, this.dataConverterManager);
                // world =, b0 to dimension, s1 to name, added Environment and gen
                worlddata = idatamanager.getWorldData();
                if (worlddata == null) {
                    worlddata = new WorldData(worldsettings, name);
                }
                worlddata.checkName(name); // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
                world = (WorldServer) new SecondaryWorldServer(this, idatamanager, DimensionManager.a(dimension), this.getWorldServer(DimensionManager.OVERWORLD), this.methodProfiler, worlddata, org.bukkit.World.Environment.getEnvironment(dimension), gen).i_();
            }

            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(world.getWorld()));

            world.addIWorldAccess(new WorldManager(this, world));
            if (!this.H()) {
                world.getWorldData().setGameType(this.getGamemode());
            }

            this.worldServer.put(world.dimension, world);
            this.getPlayerList().setPlayerFileData(world);

            if (worlddata.P() != null) {
                this.getBossBattleCustomData().a(worlddata.P());
            }
        }
        this.a(this.getDifficulty());
        this.a(this.getWorldServer(DimensionManager.OVERWORLD).worldMaps);
        // CraftBukkit end

        // Paper start - Handle collideRule team for player collision toggle
        final Scoreboard scoreboard = this.getScoreboard();
        final java.util.Collection<String> toRemove = scoreboard.getTeams().stream().filter(team -> team.getName().startsWith("collideRule_")).map(ScoreboardTeam::getName).collect(java.util.stream.Collectors.toList());
        for (String teamName : toRemove) {
            scoreboard.removeTeam(scoreboard.getTeam(teamName)); // Clean up after ourselves
        }

        if (!com.destroystokyo.paper.PaperConfig.enablePlayerCollisions) {
            this.getPlayerList().collideRuleTeamName = org.apache.commons.lang3.StringUtils.left("collideRule_" + java.util.concurrent.ThreadLocalRandom.current().nextInt(), 16);
            ScoreboardTeam collideTeam = scoreboard.createTeam(this.getPlayerList().collideRuleTeamName);
            collideTeam.setCanSeeFriendlyInvisibles(false); // Because we want to mimic them not being on a team at all
        }
        // Paper end
    }

    protected void a(File file, WorldData worlddata) {
        this.resourcePackRepository.a((ResourcePackSource) (new ResourcePackSourceVanilla()));
        this.resourcePackFolder = new ResourcePackSourceFolder(new File(file, "datapacks"));
        // CraftBukkit start
        bukkitDataPackFolder = new File(new File(file, "datapacks"), "bukkit");
        if (!bukkitDataPackFolder.exists()) {
            bukkitDataPackFolder.mkdirs();
        }
        File mcMeta = new File(bukkitDataPackFolder, "pack.mcmeta");
        if (!mcMeta.exists()) {
            try {
                com.google.common.io.Files.write("{\n"
                        + "    \"pack\": {\n"
                        + "        \"description\": \"Data pack for resources provided by Bukkit plugins\",\n"
                        + "        \"pack_format\": 1\n"
                        + "    }\n"
                        + "}", mcMeta, com.google.common.base.Charsets.UTF_8);
            } catch (IOException ex) {
                throw new RuntimeException("Could not initialize Bukkit datapack", ex);
            }
        }
        // CraftBukkit end
        this.resourcePackRepository.a((ResourcePackSource) this.resourcePackFolder);
        this.resourcePackRepository.a();
        List<ResourcePackLoader> list = Lists.newArrayList();
        Iterator iterator = worlddata.O().iterator();

        while (iterator.hasNext()) {
            String s = (String) iterator.next();
            ResourcePackLoader resourcepackloader = this.resourcePackRepository.a(s);

            if (resourcepackloader != null) {
                list.add(resourcepackloader);
            } else {
                MinecraftServer.LOGGER.warn("Missing data pack {}", s);
            }
        }

        this.resourcePackRepository.a((Collection) list);
        this.a(worlddata);
    }

    protected void a(PersistentCollection persistentcollection) {
        boolean flag = true;
        boolean flag1 = true;
        boolean flag2 = true;
        boolean flag3 = true;
        boolean flag4 = true;

        this.b((IChatBaseComponent) (new ChatMessage("menu.generatingTerrain", new Object[0])));

        // CraftBukkit start - fire WorldLoadEvent and handle whether or not to keep the spawn in memory
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean waitForChunks = Boolean.getBoolean("paper.waitforchunks"); // Paper
        for (WorldServer worldserver : this.getWorlds()) {
            MinecraftServer.LOGGER.info("Preparing start region for level " + worldserver.dimension + " (Seed: " + worldserver.getSeed() + ")");
            if (!worldserver.getWorld().getKeepSpawnInMemory()) {
                continue;
            }

            BlockPosition blockposition = worldserver.getSpawn();
            List<ChunkCoordIntPair> list = worldserver.getChunkProvider().getSpiralOutChunks(blockposition,  worldserver.paperConfig.keepLoadedRange >> 4); // Paper
            Set<ChunkCoordIntPair> set = Sets.newConcurrentHashSet();

            // Paper - remove arraylist creation, call spiral above
            if (this.isRunning()) { // Paper
                int expected = list.size(); // Paper

                CompletableFuture completablefuture = worldserver.getChunkProvider().loadAllChunks(list, (chunk) -> { // Paper
                    set.add(chunk.getPos());
                    if (waitForChunks && (set.size() == expected || (set.size() < expected && set.size() % (set.size() / 10) == 0))) {
                        this.a(new ChatMessage("menu.preparingSpawn", new Object[0]), set.size() * 100 / expected); // Paper
                    }
                });

                while (waitForChunks && !completablefuture.isDone() && isRunning()) { // Paper
                    try {
                        PaperAsyncChunkProvider.processMainThreadQueue(this); // Paper
                        completablefuture.get(50L, TimeUnit.MILLISECONDS); // Paper
                    } catch (InterruptedException interruptedexception) {
                        throw new RuntimeException(interruptedexception);
                    } catch (ExecutionException executionexception) {
                        if (executionexception.getCause() instanceof RuntimeException) {
                            throw (RuntimeException) executionexception.getCause();
                        }

                        throw new RuntimeException(executionexception.getCause());
                    } catch (TimeoutException timeoutexception) {
                        //this.a(new ChatMessage("menu.preparingSpawn", new Object[0]), set.size() * 100 / expected); // Paper
                    }
                }

                if (waitForChunks) this.a(new ChatMessage("menu.preparingSpawn", new Object[0]), set.size() * 100 / expected); // Paper
            }
        }

        for (WorldServer world : com.google.common.collect.Lists.newArrayList(this.getWorlds())) { // Paper - avoid como if 1 world triggers another world
            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(world.getWorld()));
        }
        // CraftBukkit end
        MinecraftServer.LOGGER.info("Time elapsed: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        Iterator iterator = DimensionManager.b().iterator();

        while (iterator.hasNext()) {
            DimensionManager dimensionmanager = (DimensionManager) iterator.next();
            ForcedChunk forcedchunk = (ForcedChunk) persistentcollection.get(dimensionmanager, ForcedChunk::new, "chunks");

            if (forcedchunk != null) {
                WorldServer worldserver1 = this.getWorldServer(dimensionmanager);
                LongIterator longiterator = forcedchunk.a().iterator();

                while (longiterator.hasNext()) {
                    this.a(new ChatMessage("menu.loadingForcedChunks", new Object[] { dimensionmanager}), forcedchunk.a().size() * 100 / 625);
                    long k = longiterator.nextLong();
                    ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(k);

                    worldserver1.getChunkProvider().getChunkAt(chunkcoordintpair.x, chunkcoordintpair.z, true, true);
                }
            }
        }

        this.l();
    }

    protected void a(String s, IDataManager idatamanager) {
        File file = new File(idatamanager.getDirectory(), "resources.zip");

        if (file.isFile()) {
            try {
                this.setResourcePack("level://" + URLEncoder.encode(s, StandardCharsets.UTF_8.toString()) + "/" + "resources.zip", "");
            } catch (UnsupportedEncodingException unsupportedencodingexception) {
                MinecraftServer.LOGGER.warn("Something went wrong url encoding {}", s);
            }
        }

    }

    public abstract boolean getGenerateStructures();

    public abstract EnumGamemode getGamemode();

    public abstract EnumDifficulty getDifficulty();

    public abstract boolean isHardcore();

    public abstract int j();

    public abstract boolean k();

    protected void a(IChatBaseComponent ichatbasecomponent, int i) {
        this.w = ichatbasecomponent;
        this.x = i;
        MinecraftServer.LOGGER.info("{}: {}%", ichatbasecomponent.getString(), i);
    }

    protected void l() {
        this.w = null;
        this.x = 0;
        // CraftBukkit Start
        this.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD);
        LoginListener.allowLogins(); // Paper - Allow logins once postworld
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
        // CraftBukkit end
    }

    protected void saveChunks(boolean flag) {
        Iterator iterator = this.getWorlds().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (worldserver != null) {
                if (!flag) {
                    MinecraftServer.LOGGER.info("Saving chunks for level '{}'/{}", worldserver.getWorldData().getName(), DimensionManager.a(worldserver.worldProvider.getDimensionManager()));
                }

                try {
                    worldserver.save(true, (IProgressUpdate) null);
                } catch (ExceptionWorldConflict exceptionworldconflict) {
                    MinecraftServer.LOGGER.warn(exceptionworldconflict.getMessage());
                }
            }
        }

    }

    // CraftBukkit start
    private boolean hasStopped = false;
    private final Object stopLock = new Object();
    // CraftBukkit end

    public void stop() throws ExceptionWorldConflict { // CraftBukkit - added throws
        // CraftBukkit start - prevent double stopping on multiple threads
        synchronized(stopLock) {
            if (hasStopped) return;
            hasStopped = true;
        }
        PaperAsyncChunkProvider.stop(this); // Paper
        // CraftBukkit end
        MinecraftServer.LOGGER.info("Stopping server");
        MinecraftTimings.stopServer(); // Paper
        // CraftBukkit start
        if (this.server != null) {
            this.server.disablePlugins();
        }
        // CraftBukkit end
        if (this.getServerConnection() != null) {
            this.getServerConnection().b();
        }

        if (this.playerList != null) {
            MinecraftServer.LOGGER.info("Saving players");
            this.playerList.savePlayers();
            this.playerList.u(isRestarting); // Paper
            try { Thread.sleep(100); } catch (InterruptedException ex) {} // CraftBukkit - SPIGOT-625 - give server at least a chance to send packets
        }

        MinecraftServer.LOGGER.info("Saving worlds");
        Iterator iterator = this.getWorlds().iterator();

        WorldServer worldserver;

        while (iterator.hasNext()) {
            worldserver = (WorldServer) iterator.next();
            if (worldserver != null) {
                worldserver.savingDisabled = false;
            }
        }

        this.saveChunks(false);
        iterator = this.getWorlds().iterator();

        while (iterator.hasNext()) {
            worldserver = (WorldServer) iterator.next();
            if (worldserver != null) {
                worldserver.close();
            }
        }

        if (this.snooper.d()) {
            this.snooper.e();
        }

        // Spigot start
        if (org.spigotmc.SpigotConfig.saveUserCacheOnStopOnly) {
            LOGGER.info("Saving usercache.json");
            this.getModernUserCache().save(); // Paper // Akarin - force to single thread to ensure safety
        }
        // Spigot end
    }

    public String getServerIp() {
        return this.serverIp;
    }

    public void b(String s) {
        this.serverIp = s;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    // Paper start - allow passing of the intent to restart
    public void safeShutdown() {
        safeShutdown(false);
    }

    public void safeShutdown(boolean isRestarting) {
        this.isRunning = false;
        this.isRestarting = isRestarting;
    }
    // Paper end

    private boolean canSleepForTick() {
        return System.nanoTime() - lastTick + catchupTime < TICK_TIME; // Paper - improved "are we lagging" check to match our own
    }

    // Spigot Start
    private static double calcTps(double avg, double exp, double tps)
    {
        return ( avg * exp ) + ( tps * ( 1 - exp ) );
    }

    // Paper start - Further improve server tick loop
    private static final long SEC_IN_NANO = 1000000000;
    private static final long MAX_CATCHUP_BUFFER = TICK_TIME * TPS * 60L;
    private long lastTick = 0;
    private long catchupTime = 0;
    public final RollingAverage tps1 = new RollingAverage(60);
    public final RollingAverage tps5 = new RollingAverage(60 * 5);
    public final RollingAverage tps15 = new RollingAverage(60 * 15);

    public static class RollingAverage {
        private final int size;
        private long time;
        private java.math.BigDecimal total;
        private int index = 0;
        private final java.math.BigDecimal[] samples;
        private final long[] times;

        RollingAverage(int size) {
            this.size = size;
            this.time = size * SEC_IN_NANO;
            this.total = dec(TPS).multiply(dec(SEC_IN_NANO)).multiply(dec(size));
            this.samples = new java.math.BigDecimal[size];
            this.times = new long[size];
            for (int i = 0; i < size; i++) {
                this.samples[i] = dec(TPS);
                this.times[i] = SEC_IN_NANO;
            }
        }

        private static java.math.BigDecimal dec(long t) {
            return new java.math.BigDecimal(t);
        }
        public void add(java.math.BigDecimal x, long t) {
            time -= times[index];
            total = total.subtract(samples[index].multiply(dec(times[index])));
            samples[index] = x;
            times[index] = t;
            time += t;
            total = total.add(x.multiply(dec(t)));
            if (++index == size) {
                index = 0;
            }
        }

        public double getAverage() {
            return total.divide(dec(time), 30, java.math.RoundingMode.HALF_UP).doubleValue();
        }
    }
    private static final java.math.BigDecimal TPS_BASE = new java.math.BigDecimal(1E9).multiply(new java.math.BigDecimal(SAMPLE_INTERVAL));
    // Paper End
    // Spigot End

    public void run() {
        try {
            if (this.init()) {
                this.nextTick = SystemUtils.getMonotonicMillis();
                this.m.setMOTD(new ChatComponentText(this.motd));
                this.m.setServerInfo(new ServerPing.ServerData("1.13.2", 404));
                this.a(this.m);

                // Spigot start
                org.spigotmc.WatchdogThread.hasStarted = true; // Paper
                io.akarin.server.core.AkarinAsyncScheduler.initalise(); // Akarin
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY); // Akarin
                Arrays.fill( recentTps, 20 );
                long start = System.nanoTime(), curTime, wait, tickSection = start; // Paper - Further improve server tick loop
                lastTick = start - TICK_TIME; // Paper
                while (this.isRunning) {
                    curTime = System.nanoTime();
                    // Paper start - Further improve server tick loop
                    wait = TICK_TIME - (curTime - lastTick);
                    if (wait > 0) {
                        if (catchupTime < 2E6) {
                            wait += Math.abs(catchupTime);
                        } else if (wait < catchupTime) {
                            catchupTime -= wait;
                            wait = 0;
                        } else {
                            wait -= catchupTime;
                            catchupTime = 0;
                        }
                    }
                    if (wait > 0) {
                    	// Akarin start
                    	if (AkarinGlobalConfig.spinningAwaitTicking) {
                        	long park = System.nanoTime();
                        	while ((System.nanoTime() - park) < wait);
                    	} else {
                    		Thread.sleep(wait / 1000000);
                    	}
                    	// Akarin end
                        curTime = System.nanoTime();
                        wait = TICK_TIME - (curTime - lastTick);
                    }

                    catchupTime = Math.min(MAX_CATCHUP_BUFFER, catchupTime - wait);
                    if ( ++MinecraftServer.currentTick % SAMPLE_INTERVAL == 0 )
                    {
                        final long diff = curTime - tickSection;
                        java.math.BigDecimal currentTps = TPS_BASE.divide(new java.math.BigDecimal(diff), 30, java.math.RoundingMode.HALF_UP);
                        tps1.add(currentTps, diff);
                        tps5.add(currentTps, diff);
                        tps15.add(currentTps, diff);
                        // Backwards compat with bad plugins
                        recentTps[0] = tps1.getAverage();
                        recentTps[1] = tps5.getAverage();
                        recentTps[2] = tps15.getAverage();
                        // Paper end
                        tickSection = curTime;
                    }
                    lastTick = curTime;

                    //MinecraftServer.currentTick = (int) (System.currentTimeMillis() / 50); // CraftBukkit // Paper - don't overwrite current tick time
                    this.a(this::canSleepForTick);
                    this.nextTick += 50L;
                    // Spigot end
                    this.P = true;
                }
            } else {
                this.a((CrashReport) null);
            }
        } catch (Throwable throwable) {
            MinecraftServer.LOGGER.error("Encountered an unexpected exception", throwable);
            // Spigot Start
            if ( throwable.getCause() != null )
            {
                MinecraftServer.LOGGER.error( "\tCause of unexpected exception was", throwable.getCause() );
            }
            // Spigot End
            CrashReport crashreport;

            if (throwable instanceof ReportedException) {
                crashreport = this.b(((ReportedException) throwable).a());
            } else {
                crashreport = this.b(new CrashReport("Exception in server tick loop", throwable));
            }

            File file = new File(new File(this.s(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.a(file)) {
                MinecraftServer.LOGGER.error("This crash report has been saved to: {}", file.getAbsolutePath());
            } else {
                MinecraftServer.LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.a(crashreport);
        } finally {
            try {
                org.spigotmc.WatchdogThread.doStop();
                this.isStopped = true;
                this.stop();
            } catch (Throwable throwable1) {
                MinecraftServer.LOGGER.error("Exception stopping the server", throwable1);
            } finally {
                // CraftBukkit start - Restore terminal to original settings
                try {
                    net.minecrell.terminalconsole.TerminalConsoleAppender.close(); // Paper - Use TerminalConsoleAppender
                } catch (Exception ignored) {
                }
                // CraftBukkit end
                this.t();
            }

        }

    }

    public void a(ServerPing serverping) {
        File file = this.c("server-icon.png");

        if (!file.exists()) {
            file = this.getConvertable().b(this.getWorld(), "icon.png");
        }

        if (file.isFile()) {
            ByteBuf bytebuf = Unpooled.buffer();

            try {
                BufferedImage bufferedimage = ImageIO.read(file);

                Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
                Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
                ImageIO.write(bufferedimage, "PNG", new ByteBufOutputStream(bytebuf));
                ByteBuffer bytebuffer = Base64.getEncoder().encode(bytebuf.nioBuffer());

                serverping.setFavicon("data:image/png;base64," + StandardCharsets.UTF_8.decode(bytebuffer));
            } catch (Exception exception) {
                MinecraftServer.LOGGER.error("Couldn't load server icon", exception);
            } finally {
                bytebuf.release();
            }
        }

    }

    public File s() {
        return new File(".");
    }

    protected void a(CrashReport crashreport) {}

    public void t() {}

    protected void a(BooleanSupplier booleansupplier) {
        co.aikar.timings.TimingsManager.FULL_SERVER_TICK.startTiming(); // Paper // Akarin
        this.slackActivityAccountant.tickStarted(); // Spigot
        long i = SystemUtils.getMonotonicNanos(); long startTime = i; // Paper
        new com.destroystokyo.paper.event.server.ServerTickStartEvent(this.ticks+1).callEvent(); // Paper

        ++this.ticks;
        if (this.S) {
            this.S = false;
            //this.methodProfiler.a(this.ticks); // Akarin - remove caller
        }

        //this.methodProfiler.enter(* // Akarin - remove caller
        this.b(booleansupplier);
        if (i - this.Y >= 5000000000L) {
            this.Y = i;
            this.m.setPlayerSample(new ServerPing.ServerPingPlayerSample(this.getMaxPlayers(), this.getPlayerCount()));
            GameProfile[] agameprofile = new GameProfile[Math.min(this.getPlayerCount(), org.spigotmc.SpigotConfig.playerSample)]; // Paper
            int j = MathHelper.nextInt(this.n, 0, this.getPlayerCount() - agameprofile.length);

            for (int k = 0; k < agameprofile.length; ++k) {
                agameprofile[k] = ((EntityPlayer) this.playerList.v().get(j + k)).getProfile();
            }

            Collections.shuffle(Arrays.asList(agameprofile));
            this.m.b().a(agameprofile);
        }

            //this.methodProfiler.enter(* // Akarin - remove caller

        serverAutoSave = (autosavePeriod > 0 && this.ticks % autosavePeriod == 0); // Paper
        int playerSaveInterval = com.destroystokyo.paper.PaperConfig.playerAutoSaveRate;
        if (playerSaveInterval < 0) {
            playerSaveInterval = autosavePeriod;
        }
        if (playerSaveInterval > 0) { // CraftBukkit // Paper
            this.playerList.savePlayers(playerSaveInterval);
            // Spigot Start
        } // Paper - Incremental Auto Saving

            // We replace this with saving each individual world as this.saveChunks(...) is broken,
            // and causes the main thread to sleep for random amounts of time depending on chunk activity
            // Also pass flag to only save modified chunks
            server.playerCommandState = true;
            for (World world : getWorlds()) {
                if (world.paperConfig.autoSavePeriod > 0) world.getWorld().save(false); // Paper - Incremental / Configurable Auto Saving
            }
            server.playerCommandState = false;
            // this.saveChunks(true);
            // Spigot End
            ////this.methodProfiler.exit(); // Akarin // Akarin - remove caller
        //} // Paper - Incremental Auto Saving

        //this.methodProfiler.enter(* // Akarin - remove caller
        if (getSnooperEnabled() && !this.snooper.d() && this.ticks > 100) { // Spigot
            this.snooper.a();
        }

        if (getSnooperEnabled() && this.ticks % 6000 == 0) { // Spigot
            this.snooper.b();
        }

        //this.methodProfiler.exit(); // Akarin
        //this.methodProfiler.enter(* // Akarin - remove caller
        long l = this.d[this.ticks % 100] = SystemUtils.getMonotonicNanos() - i;

        this.ap = this.ap * 0.8F + (float) l / 1000000.0F * 0.19999999F;
        //this.methodProfiler.exit(); // Akarin
        //this.methodProfiler.exit(); // Akarin
        org.spigotmc.WatchdogThread.tick(); // Spigot
        PaperLightingQueue.processQueue(startTime); // Paper
        expiringMaps.removeIf(ExpiringMap::clean); // Paper
        this.slackActivityAccountant.tickEnded(l); // Spigot
        // Paper start
        long endTime = System.nanoTime();
        long remaining = (TICK_TIME - (endTime - lastTick)) - catchupTime;
        new com.destroystokyo.paper.event.server.ServerTickEndEvent(this.ticks, ((double)(endTime - lastTick) / 1000000D), remaining).callEvent();
        // Paper end
        co.aikar.timings.TimingsManager.FULL_SERVER_TICK.stopTiming(); // Paper
    }

    public void b(BooleanSupplier booleansupplier) {
        MinecraftTimings.bukkitSchedulerTimer.startTimingUnsafe(); // Paper // Akarin
        this.server.getScheduler().mainThreadHeartbeat(this.ticks); // CraftBukkit
        MinecraftTimings.bukkitSchedulerTimer.stopTimingUnsafe(); // Paper // Akarin
        MinecraftTimings.minecraftSchedulerTimer.startTimingUnsafe(); // Paper // Akarin
        //this.methodProfiler.enter(* // Akarin - remove caller

        FutureTask futuretask;

        while ((futuretask = (FutureTask) this.f.poll()) != null) {
            SystemUtils.a(futuretask, MinecraftServer.LOGGER);
        }
        PaperAsyncChunkProvider.processMainThreadQueue(this); // Paper
        MinecraftTimings.minecraftSchedulerTimer.stopTimingUnsafe(); // Paper // Akarin

        //this.methodProfiler.exitEnter("commandFunctions"); // Akarin
        MinecraftTimings.commandFunctionsTimer.startTimingUnsafe(); // Spigot // Akarin
        this.getFunctionData().tick();
        MinecraftTimings.commandFunctionsTimer.stopTimingUnsafe(); // Spigot // Akarin
        //this.methodProfiler.exitEnter("levels"); // Akarin

        // CraftBukkit start
        // Run tasks that are waiting on processing
        MinecraftTimings.processQueueTimer.startTimingUnsafe(); // Spigot // Akarin
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }
        MinecraftTimings.processQueueTimer.stopTimingUnsafe(); // Spigot // Akarin

        MinecraftTimings.chunkIOTickTimer.startTimingUnsafe(); // Spigot // Akarin
        org.bukkit.craftbukkit.chunkio.ChunkIOExecutor.tick();
        MinecraftTimings.chunkIOTickTimer.stopTimingUnsafe(); // Spigot // Akarin

        // Akarin start
        /*
        MinecraftTimings.timeUpdateTimer.startTiming(); // Spigot
        // Send time updates to everyone, it will get the right time from the world the player is in.
        // Paper start - optimize time updates
        for (final WorldServer world : this.getWorlds()) {
            final boolean doDaylight = world.getGameRules().getBoolean("doDaylightCycle");
            final long dayTime = world.getDayTime();
            long worldTime = world.getTime();
            final PacketPlayOutUpdateTime worldPacket = new PacketPlayOutUpdateTime(worldTime, dayTime, doDaylight);
            for (EntityHuman entityhuman : world.players) {
                if (!(entityhuman instanceof EntityPlayer) || (ticks + entityhuman.getId()) % 20 != 0) {
                    continue;
                }
                EntityPlayer entityplayer = (EntityPlayer) entityhuman;
                long playerTime = entityplayer.getPlayerTime();
                PacketPlayOutUpdateTime packet = (playerTime == dayTime) ? worldPacket :
                    new PacketPlayOutUpdateTime(worldTime, playerTime, doDaylight);
                entityplayer.playerConnection.sendPacket(packet); // Add support for per player time
            }
        }
        // Paper end
        MinecraftTimings.timeUpdateTimer.stopTiming(); // Spigot
        */
        // Akarin end

        // WorldServer worldserver; // CraftBukkit - dropped down
        long i;

        // CraftBukkit - dropTickTime
        for (Iterator iterator = this.getWorlds().iterator(); iterator.hasNext();) {
             WorldServer worldserver = (WorldServer) iterator.next();
            PaperAsyncChunkProvider.processMainThreadQueue(worldserver); // Paper
            worldserver.hasPhysicsEvent =  org.bukkit.event.block.BlockPhysicsEvent.getHandlerList().getRegisteredListeners().length > 0; // Paper
            TileEntityHopper.skipHopperEvents = worldserver.paperConfig.disableHopperMoveEvents || org.bukkit.event.inventory.InventoryMoveItemEvent.getHandlerList().getRegisteredListeners().length == 0; // Paper
            i = SystemUtils.getMonotonicNanos();
            if (true || worldserver.worldProvider.getDimensionManager() == DimensionManager.OVERWORLD || this.getAllowNether()) { // CraftBukkit
                // Akarin start
                /*
                this.methodProfiler.a(() -> {
                    return "dim-" + worldserver.worldProvider.getDimensionManager().getDimensionID();
                });
                */
                // Akarin end
                /* Drop global time updates
                if (this.ticks % 20 == 0) {
                    //this.methodProfiler.enter(* // Akarin - remove caller
                    this.playerList.a((Packet) (new PacketPlayOutUpdateTime(worldserver.getTime(), worldserver.getDayTime(), worldserver.getGameRules().getBoolean("doDaylightCycle"))), worldserver.worldProvider.getDimensionManager());
                    //this.methodProfiler.exit(); // Akarin
                }
                // CraftBukkit end */

                //this.methodProfiler.enter(* // Akarin - remove caller

                CrashReport crashreport;

                try {
                    worldserver.timings.doTick.startTimingUnsafe(); // Spigot // Akarin
                    worldserver.doTick(booleansupplier);
                    worldserver.timings.doTick.stopTimingUnsafe(); // Spigot // Akarin
                } catch (Throwable throwable) {
                    // Spigot Start
                    try {
                    crashreport = CrashReport.a(throwable, "Exception ticking world");
                    } catch (Throwable t){
                        throw new RuntimeException("Error generating crash report", t);
                    }
                    // Spigot End
                    worldserver.a(crashreport);
                    throw new ReportedException(crashreport);
                }

                try {
                    worldserver.timings.tickEntities.startTimingUnsafe(); // Spigot // Akarin
                    worldserver.tickEntities();
                    worldserver.timings.tickEntities.stopTimingUnsafe(); // Spigot // Akarin
                } catch (Throwable throwable1) {
                    // Spigot Start
                    try {
                    crashreport = CrashReport.a(throwable1, "Exception ticking world entities");
                    } catch (Throwable t){
                        throw new RuntimeException("Error generating crash report", t);
                    }
                    // Spigot End
                    worldserver.a(crashreport);
                    throw new ReportedException(crashreport);
                }

                //this.methodProfiler.exit(); // Akarin
                //this.methodProfiler.enter(* // Akarin - remove caller
                if (playerList.players.size() > 0) worldserver.getTracker().updatePlayers(); // Paper - No players, why spend time tracking them? (See patch)
                //this.methodProfiler.exit(); // Akarin
                //this.methodProfiler.exit(); // Akarin
                worldserver.explosionDensityCache.clear(); // Paper - Optimize explosions
            }
        }

        //this.methodProfiler.exitEnter("connection"); // Akarin
        MinecraftTimings.connectionTimer.startTimingUnsafe(); // Spigot // Akarin
        this.getServerConnection().c();
        MinecraftTimings.connectionTimer.stopTimingUnsafe(); // Spigot // Akarin
        //this.methodProfiler.exitEnter("players"); // Akarin
        // Akarin start
        /*
        MinecraftTimings.playerListTimer.startTiming(); // Spigot
        this.playerList.tick();
        MinecraftTimings.playerListTimer.stopTiming(); // Spigot
        */
        // Akarin end
        //this.methodProfiler.exitEnter("tickables"); // Akarin

        MinecraftTimings.tickablesTimer.startTimingUnsafe(); // Spigot // Akarin
        for (int j = 0; j < this.k.size(); ++j) {
            ((ITickable) this.k.get(j)).tick();
        }
        MinecraftTimings.tickablesTimer.stopTimingUnsafe(); // Spigot // Akarin

        //this.methodProfiler.exit(); // Akarin
    }

    public boolean getAllowNether() {
        return true;
    }

    public void a(ITickable itickable) {
        this.k.add(itickable);
    }

    public static void main(final OptionSet options) { // CraftBukkit - replaces main(String[] astring)
        DispenserRegistry.c();

        try {
            /* CraftBukkit start - Replace everything
            boolean flag = true;
            String s = null;
            String s1 = ".";
            String s2 = null;
            boolean flag1 = false;
            boolean flag2 = false;
            boolean flag3 = false;
            int i = -1;

            for (int j = 0; j < astring.length; ++j) {
                String s3 = astring[j];
                String s4 = j == astring.length - 1 ? null : astring[j + 1];
                boolean flag4 = false;

                if (!"nogui".equals(s3) && !"--nogui".equals(s3)) {
                    if ("--port".equals(s3) && s4 != null) {
                        flag4 = true;

                        try {
                            i = Integer.parseInt(s4);
                        } catch (NumberFormatException numberformatexception) {
                            ;
                        }
                    } else if ("--singleplayer".equals(s3) && s4 != null) {
                        flag4 = true;
                        s = s4;
                    } else if ("--universe".equals(s3) && s4 != null) {
                        flag4 = true;
                        s1 = s4;
                    } else if ("--world".equals(s3) && s4 != null) {
                        flag4 = true;
                        s2 = s4;
                    } else if ("--demo".equals(s3)) {
                        flag1 = true;
                    } else if ("--bonusChest".equals(s3)) {
                        flag2 = true;
                    } else if ("--forceUpgrade".equals(s3)) {
                        flag3 = true;
                    }
                } else {
                    flag = false;
                }

                if (flag4) {
                    ++j;
                }
            }
            */ // CraftBukkit end

            String s1 = "."; // PAIL?
            YggdrasilAuthenticationService yggdrasilauthenticationservice = new com.destroystokyo.paper.profile.PaperAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString()); // Paper
            MinecraftSessionService minecraftsessionservice = yggdrasilauthenticationservice.createMinecraftSessionService();
            GameProfileRepository gameprofilerepository = yggdrasilauthenticationservice.createProfileRepository();
            UserCache usercache = new UserCache(gameprofilerepository, new File(s1, MinecraftServer.a.getName()));
            final DedicatedServer dedicatedserver = new DedicatedServer(options, DataConverterRegistry.a(), yggdrasilauthenticationservice, minecraftsessionservice, gameprofilerepository, usercache);

            /* CraftBukkit start
            if (s != null) {
                dedicatedserver.h(s);
            }

            if (s2 != null) {
                dedicatedserver.setWorld(s2);
            }

            if (i >= 0) {
                dedicatedserver.setPort(i);
            }

            if (flag1) {
                dedicatedserver.c(true);
            }

            if (flag2) {
                dedicatedserver.d(true);
            }

            if (flag && !GraphicsEnvironment.isHeadless()) {
                dedicatedserver.aW();
            }

            if (flag3) {
                dedicatedserver.setForceUpgrade(true);
            }

            dedicatedserver.v();
            Thread thread = new Thread("Server Shutdown Thread") {
                public void run() {
                    dedicatedserver.stop();
                }
            };

            thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(MinecraftServer.LOGGER));
            Runtime.getRuntime().addShutdownHook(thread);
            */

            if (options.has("port")) {
                int port = (Integer) options.valueOf("port");
                if (port > 0) {
                    dedicatedserver.setPort(port);
                }
            }

            if (options.has("universe")) {
                dedicatedserver.universe = (File) options.valueOf("universe");
            }

            if (options.has("world")) {
                dedicatedserver.setWorld((String) options.valueOf("world"));
            }

            if (options.has("forceUpgrade")) {
                dedicatedserver.setForceUpgrade(true);
            }

            dedicatedserver.primaryThread.start();
            // CraftBukkit end
        } catch (Exception exception) {
            MinecraftServer.LOGGER.fatal("Failed to start the minecraft server", exception);
        }

    }

    protected void setForceUpgrade(boolean flag) {
        this.forceUpgrade = flag;
    }

    public void v() {
        /* CraftBukkit start - prevent abuse
        this.serverThread = new Thread(this, "Server thread");
        this.serverThread.setUncaughtExceptionHandler((thread, throwable) -> {
            MinecraftServer.LOGGER.error(throwable);
        });
        this.serverThread.start();
        // CraftBukkit end */
    }

    public File c(String s) {
        return new File(this.s(), s);
    }

    public void info(String s) {
        MinecraftServer.LOGGER.info(s);
    }

    public void warning(String s) {
        MinecraftServer.LOGGER.warn(s);
    }

    public WorldServer getWorldServer(DimensionManager dimensionmanager) {
        return (WorldServer) this.worldServer.get(dimensionmanager);
    }

    public Iterable<WorldServer> getWorlds() {
        return this.worldServer.values();
    }

    public String getVersion() {
        return "1.13.2";
    }

    public int getPlayerCount() {
        return this.playerList.getPlayerCount();
    }

    public int getMaxPlayers() {
        return this.playerList.getMaxPlayers();
    }

    public String[] getPlayers() {
        return this.playerList.f();
    }

    public boolean isDebugging() {
        return this.getPropertyManager().getBoolean("debug", false); // CraftBukkit - don't hardcode
    }

    public void f(String s) {
        MinecraftServer.LOGGER.error(s);
    }

    public void g(String s) {
        if (this.isDebugging()) {
            MinecraftServer.LOGGER.info(s);
        }

    }

    public String getServerModName() {
        return "Paper"; //Paper - Paper > // Spigot - Spigot > // CraftBukkit - cb > vanilla!
    }

    public CrashReport b(CrashReport crashreport) {
        crashreport.g().a("Profiler Position", () -> {
            return this.methodProfiler.a() ? this.methodProfiler.f() : "N/A (disabled)";
        });
        if (this.playerList != null) {
            crashreport.g().a("Player Count", () -> {
                return this.playerList.getPlayerCount() + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.v();
            });
        }

        crashreport.g().a("Data Packs", () -> {
            StringBuilder stringbuilder = new StringBuilder();
            Iterator iterator = this.resourcePackRepository.d().iterator();

            while (iterator.hasNext()) {
                ResourcePackLoader resourcepackloader = (ResourcePackLoader) iterator.next();

                if (stringbuilder.length() > 0) {
                    stringbuilder.append(", ");
                }

                stringbuilder.append(resourcepackloader.e());
                if (!resourcepackloader.c().a()) {
                    stringbuilder.append(" (incompatible)");
                }
            }

            return stringbuilder.toString();
        });
        return crashreport;
    }

    public boolean D() {
        return true; // CraftBukkit
    }

    public void sendMessage(IChatBaseComponent ichatbasecomponent) {
        MinecraftServer.LOGGER.info(org.bukkit.craftbukkit.util.CraftChatMessage.fromComponent(ichatbasecomponent, net.minecraft.server.EnumChatFormat.WHITE));// Paper - Log message with colors
    }

    public KeyPair E() {
        return this.H;
    }

    public int getPort() {
        return this.q;
    }

    public void setPort(int i) {
        this.q = i;
    }

    public String G() {
        return this.I;
    }

    public void h(String s) {
        this.I = s;
    }

    public boolean H() {
        return this.I != null;
    }

    public String getWorld() {
        return this.J;
    }

    public void setWorld(String s) {
        this.J = s;
    }

    public void a(KeyPair keypair) {
        this.H = keypair;
    }

    public void a(EnumDifficulty enumdifficulty) {
        Iterator iterator = this.getWorlds().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (worldserver.getWorldData().isHardcore()) {
                worldserver.getWorldData().setDifficulty(EnumDifficulty.HARD);
                worldserver.setSpawnFlags(true, true);
            } else if (this.H()) {
                worldserver.getWorldData().setDifficulty(enumdifficulty);
                worldserver.setSpawnFlags(worldserver.getDifficulty() != EnumDifficulty.PEACEFUL, true);
            } else {
                worldserver.getWorldData().setDifficulty(enumdifficulty);
                worldserver.setSpawnFlags(this.getSpawnMonsters(), this.spawnAnimals);
            }
        }

    }

    public boolean getSpawnMonsters() {
        return true;
    }

    public boolean L() {
        return this.demoMode;
    }

    public void c(boolean flag) {
        this.demoMode = flag;
    }

    public void d(boolean flag) {
        this.M = flag;
    }

    public Convertable getConvertable() {
        return this.convertable;
    }

    public String getResourcePack() {
        return this.N;
    }

    public String getResourcePackHash() {
        return this.O;
    }

    public void setResourcePack(String s, String s1) {
        this.N = s;
        this.O = s1;
    }

    public void a(MojangStatisticsGenerator mojangstatisticsgenerator) {
        mojangstatisticsgenerator.a("whitelist_enabled", false);
        mojangstatisticsgenerator.a("whitelist_count", 0);
        if (this.playerList != null) {
            mojangstatisticsgenerator.a("players_current", this.getPlayerCount());
            mojangstatisticsgenerator.a("players_max", this.getMaxPlayers());
            mojangstatisticsgenerator.a("players_seen", this.playerList.getSeenPlayers().length);
        }

        mojangstatisticsgenerator.a("uses_auth", this.onlineMode);
        mojangstatisticsgenerator.a("gui_state", this.ag() ? "enabled" : "disabled");
        mojangstatisticsgenerator.a("run_time", (SystemUtils.getMonotonicMillis() - mojangstatisticsgenerator.g()) / 60L * 1000L);
        mojangstatisticsgenerator.a("avg_tick_ms", (int) (MathHelper.a(this.d) * 1.0E-6D));
        int i = 0;
        Iterator iterator = this.getWorlds().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (worldserver != null) {
                WorldData worlddata = worldserver.getWorldData();

                mojangstatisticsgenerator.a("world[" + i + "][dimension]", worldserver.worldProvider.getDimensionManager());
                mojangstatisticsgenerator.a("world[" + i + "][mode]", worlddata.getGameType());
                mojangstatisticsgenerator.a("world[" + i + "][difficulty]", worldserver.getDifficulty());
                mojangstatisticsgenerator.a("world[" + i + "][hardcore]", worlddata.isHardcore());
                mojangstatisticsgenerator.a("world[" + i + "][generator_name]", worlddata.getType().name());
                mojangstatisticsgenerator.a("world[" + i + "][generator_version]", worlddata.getType().getVersion());
                mojangstatisticsgenerator.a("world[" + i + "][height]", this.F);
                mojangstatisticsgenerator.a("world[" + i + "][chunks_loaded]", worldserver.getChunkProvider().g());
                ++i;
            }
        }

        mojangstatisticsgenerator.a("worlds", i);
    }

    public boolean getSnooperEnabled() {
        return true;
    }

    public abstract boolean Q();

    public boolean getOnlineMode() {
        return server.getOnlineMode(); // CraftBukkit
    }

    public void setOnlineMode(boolean flag) {
        this.onlineMode = flag;
    }

    public boolean S() {
        return this.z;
    }

    public void f(boolean flag) {
        this.z = flag;
    }

    public boolean getSpawnAnimals() {
        return this.spawnAnimals;
    }

    public void setSpawnAnimals(boolean flag) {
        this.spawnAnimals = flag;
    }

    public boolean getSpawnNPCs() {
        return this.spawnNPCs;
    }

    public abstract boolean V();

    public void setSpawnNPCs(boolean flag) {
        this.spawnNPCs = flag;
    }

    public boolean getPVP() {
        return this.pvpMode;
    }

    public void setPVP(boolean flag) {
        this.pvpMode = flag;
    }

    public boolean getAllowFlight() {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean flag) {
        this.allowFlight = flag;
    }

    public abstract boolean getEnableCommandBlock();

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String s) {
        this.motd = s;
    }

    public int getMaxBuildHeight() {
        return this.F;
    }

    public void b(int i) {
        this.F = i;
    }

    public boolean isStopped() {
        return this.isStopped;
    }

    public PlayerList getPlayerList() {
        return this.playerList;
    }

    public void a(PlayerList playerlist) {
        this.playerList = playerlist;
    }

    public abstract boolean ad();

    public void setGamemode(EnumGamemode enumgamemode) {
        Iterator iterator = this.getWorlds().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            worldserver.getWorldData().setGameType(enumgamemode);
        }

    }

    public ServerConnection getServerConnection() {
        return this.serverConnection == null ? this.serverConnection = new ServerConnection(this) : this.serverConnection; // Spigot
    }

    public boolean ag() {
        return false;
    }

    public abstract boolean a(EnumGamemode enumgamemode, boolean flag, int i);

    public int ah() {
        return this.ticks;
    }

    public void ai() {
        this.S = true;
    }

    public int getSpawnProtection() {
        return 16;
    }

    public boolean a(World world, BlockPosition blockposition, EntityHuman entityhuman) {
        return false;
    }

    public void setForceGamemode(boolean flag) {
        this.T = flag;
    }

    public boolean getForceGamemode() {
        return this.T;
    }

    public int getIdleTimeout() {
        return this.G;
    }

    public void setIdleTimeout(int i) {
        this.G = i;
    }

    public MinecraftSessionService getSessionService() { return ap(); } // Paper - OBFHELPER
    public MinecraftSessionService ap() {
        return this.V;
    }

    public GameProfileRepository getGameProfileRepository() {
        return this.W;
    }

    public UserCache getUserCache() {
        return this.X;
    }
    // Akarin start
    private final AkarinUserCache userCache;
    
    public AkarinUserCache getModernUserCache() {
        return userCache;
    }
    // Akarin end

    public ServerPing getServerPing() {
        return this.m;
    }

    public void at() {
        this.Y = 0L;
    }

    public int au() {
        return 29999984;
    }

    public <V> ListenableFuture<V> a(Callable<V> callable) {
        Validate.notNull(callable);
        if (!this.isMainThread()) { // CraftBukkit && !this.isStopped()) {
            ListenableFutureTask<V> listenablefuturetask = ListenableFutureTask.create(callable);

            this.f.offer(listenablefuturetask); // Akarin - add -> offer
            return listenablefuturetask;
        } else {
            try {
                return Futures.immediateFuture(callable.call());
            } catch (Exception exception) {
                return Futures.immediateFailedCheckedFuture(exception);
            }
        }
    }
    // Akarin start
    @Override
    public void ensuresMainThread(Runnable runnable) {
        this.f.offer(ListenableFutureTask.create(runnable, null));
    }
    // Akarin end

    public ListenableFuture<Object> postToMainThread(Runnable runnable) {
        Validate.notNull(runnable);
        return this.a(Executors.callable(runnable));
    }

    public boolean isMainThread() {
        return ThreadAssertion.is() || Thread.currentThread() == this.serverThread; // Akarin
    }

    public int aw() {
        return 256;
    }

    public long ax() {
        return this.nextTick;
    }

    public Thread ay() {
        return this.serverThread;
    }

    public DataFixer az() {
        return this.dataConverterManager;
    }

    public int a(@Nullable WorldServer worldserver) {
        return worldserver != null ? worldserver.getGameRules().c("spawnRadius") : 10;
    }

    public AdvancementDataWorld getAdvancementData() {
        return this.al;
    }

    public CustomFunctionData getFunctionData() {
        return this.am;
    }

    public void reload() {
        if (!this.isMainThread()) {
            this.postToMainThread(this::reload);
        } else {
            this.getPlayerList().savePlayers();
            this.resourcePackRepository.a();
            this.a(this.getWorldServer(DimensionManager.OVERWORLD).getWorldData());
            this.getPlayerList().reload();
        }
    }

    private void a(WorldData worlddata) {
        List<ResourcePackLoader> list = Lists.newArrayList(this.resourcePackRepository.d());
        Iterator iterator = this.resourcePackRepository.b().iterator();

        while (iterator.hasNext()) {
            ResourcePackLoader resourcepackloader = (ResourcePackLoader) iterator.next();

            if (!worlddata.N().contains(resourcepackloader.e()) && !list.contains(resourcepackloader)) {
                MinecraftServer.LOGGER.info("Found new data pack {}, loading it automatically", resourcepackloader.e());
                resourcepackloader.h().a(list, resourcepackloader, (resourcepackloader1) -> {
                    return resourcepackloader1;
                }, false);
            }
        }

        this.resourcePackRepository.a((Collection) list);
        List<IResourcePack> list1 = Lists.newArrayList();

        this.resourcePackRepository.d().forEach((resourcepackloader1) -> {
            list1.add(resourcepackloader1.d());
        });
        this.ac.a((List) list1);
        worlddata.O().clear();
        worlddata.N().clear();
        this.resourcePackRepository.d().forEach((resourcepackloader1) -> {
            worlddata.O().add(resourcepackloader1.e());
        });
        this.resourcePackRepository.b().forEach((resourcepackloader1) -> {
            if (!this.resourcePackRepository.d().contains(resourcepackloader1)) {
                worlddata.N().add(resourcepackloader1.e());
            }

        });
    }

    public void a(CommandListenerWrapper commandlistenerwrapper) {
        if (this.aQ()) {
            PlayerList playerlist = commandlistenerwrapper.getServer().getPlayerList();
            WhiteList whitelist = playerlist.getWhitelist();

            if (whitelist.isEnabled()) {
                List<EntityPlayer> list = Lists.newArrayList(playerlist.v());
                Iterator iterator = list.iterator();

                while (iterator.hasNext()) {
                    EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                    if (!whitelist.isWhitelisted(entityplayer.getProfile())) {
                        entityplayer.playerConnection.disconnect(new ChatMessage("multiplayer.disconnect.not_whitelisted", new Object[0]));
                    }
                }

            }
        }
    }

    public IReloadableResourceManager getResourceManager() {
        return this.ac;
    }

    public ResourcePackRepository<ResourcePackLoader> getResourcePackRepository() {
        return this.resourcePackRepository;
    }

    public CommandDispatcher getCommandDispatcher() {
        return this.commandDispatcher;
    }

    public CommandListenerWrapper getServerCommandListener() {
        return new CommandListenerWrapper(this, this.getWorldServer(DimensionManager.OVERWORLD) == null ? Vec3D.a : new Vec3D(this.getWorldServer(DimensionManager.OVERWORLD).getSpawn()), Vec2F.a, this.getWorldServer(DimensionManager.OVERWORLD), 4, "Server", new ChatComponentText("Server"), this, (Entity) null);
    }

    public boolean a() {
        return true;
    }

    public boolean b() {
        return true;
    }

    public CraftingManager getCraftingManager() {
        return this.ag;
    }

    public TagRegistry getTagRegistry() {
        return this.ah;
    }

    public ScoreboardServer getScoreboard() {
        return this.ai;
    }

    public LootTableRegistry getLootTableRegistry() {
        return this.ak;
    }

    public GameRules getGameRules() {
        return this.getWorldServer(DimensionManager.OVERWORLD).getGameRules();
    }

    public BossBattleCustomData getBossBattleCustomData() {
        return this.aj;
    }

    public boolean aQ() {
        return this.an;
    }

    public void l(boolean flag) {
        this.an = flag;
    }

    public int a(GameProfile gameprofile) {
        if (this.getPlayerList().isOp(gameprofile)) {
            OpListEntry oplistentry = (OpListEntry) this.getPlayerList().getOPs().get(gameprofile);

            return oplistentry != null ? oplistentry.a() : (this.H() ? (this.G().equals(gameprofile.getName()) ? 4 : (this.getPlayerList().x() ? 4 : 0)) : this.j());
        } else {
            return 0;
        }
    }

    // CraftBukkit start
    //@Deprecated // Akarin - remove deprecated
    public static MinecraftServer getServer() {
        return SERVER;
    }
    // CraftBukkit end
}
