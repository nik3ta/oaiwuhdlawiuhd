package nuclear.module.api;

import nuclear.module.impl.combat.*;
import nuclear.module.impl.movement.*;
import nuclear.module.impl.other.*;
import nuclear.module.impl.player.*;
import nuclear.module.impl.render.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Manager {

    private final List<Module> modules = new CopyOnWriteArrayList<>();
    public final Sprint sprint;
    public final Flight flightFunction;
    public final Strafe strafe;
    public final AntiTarget antiTarget;
    public final SyncTps syncTps;
    public final SantaHat santaHat;
    public final Timer timer;
    public final AutoPotion autoPotionFunction;
    public final AutoRespawn autoRespawnFunction;
    public final Velocity velocityFunction;
    public final ClickPearl middleClickPearlFunction;
    public final AutoTotem autoTotemFunction;
    public final GuiMove guiMove;
    public final NoPush noPushFunction;
    public final HitBox hitBoxFunction;
    public final NoSlow noSlow;
    public final SeeInvisibles seeInvisibles;
    public final AttackAura auraFunction;
    public final ElytraPredict elytraPredict;
    public final CustomSwing swingAnimationFunction;
    public final NoRender noRenderFunction;
    public final TargetEsp targetEsp;
    public final ChinaHat chinaHat;
    public final ItemsCooldown gappleCooldownFunction;
    public final Optimization optimization;
    public final SkeletonEsp skeletonEsp;
    public final ServerJoiner serverJoiner;
    public final AutoSwap autoSwapFunction;
    public final Aspect aspect;
    public final ItemScroller itemScroller;
    public final Esp espFunction;
    public final NameTags nameTags;
    public final PacketCriticals packetCriticals;
    public final ElytraBooster elytraBooster;
    public final NoInteract noInteractFunction;
    public final Ambience ambience;
    public final AutoMessage autoMessage;
    public final CustomDance customDance;
    public final Assistent assistent;
    public final ElytraHelper elytraHelper;
    public final ClientSounds clientSounds;
    public final Crosshair crosshair;
    public final ViewMode viewMode;
    public final AutoExplosion autoExplosionFunction;
    public final HitColor hitColor;
    public final FreeCam freeCam;
    public final ProjectileHelper projectileHelper;
    public final BetterChat betterChat;
    public final ClickGui clickGui;
    public final AirStuck airStuck;
    public final ScoreboardHealth scoreboardHealth;
    public Interface hud2;
    public ElytraMotion elytraMotion;
    public Hands hands;
    public WaterSpeed waterSpeed;
    public final ItemPhysics itemPhysics;
    public final AutoFish autoFish;
    public final MoveHelper moveHelper;
    public final ElytraResolver elytraResolver;
    public NoCommands noCommands;
    public StreamerMode streamerMode;
    public NameProtect nameProtect;
    public final ItemFix itemFix;

    public Manager() {
        this.modules.addAll(Arrays.asList(
                this.moveHelper = new MoveHelper(),
                this.clickGui = new ClickGui(),
                this.waterSpeed = new WaterSpeed(),
                this.autoFish = new AutoFish(),
                this.elytraPredict = new ElytraPredict(),
                this.chinaHat = new ChinaHat(),
                this.autoMessage = new AutoMessage(),
                this.elytraBooster = new ElytraBooster(),
                this.scoreboardHealth = new ScoreboardHealth(),
                this.hands = new Hands(),
                this.skeletonEsp = new SkeletonEsp(),
                this.nameProtect = new NameProtect(),
                this.crosshair = new Crosshair(),
                this.noCommands = new NoCommands(),
                this.targetEsp = new TargetEsp(),
                this.airStuck = new AirStuck(),
                this.serverJoiner = new ServerJoiner(),
                this.assistent = new Assistent(),
                this.noRenderFunction = new NoRender(),
                this.sprint = new Sprint(),
                this.customDance = new CustomDance(),
                this.projectileHelper = new ProjectileHelper(),
                this.packetCriticals = new PacketCriticals(),
                this.elytraMotion = new ElytraMotion(),
                this.betterChat = new BetterChat(),
                this.seeInvisibles = new SeeInvisibles(),
                this.elytraHelper = new ElytraHelper(),
                this.flightFunction = new Flight(),
                this.strafe = new Strafe(),
                this.santaHat = new SantaHat(),
                this.timer = new Timer(),
                this.velocityFunction = new Velocity(),
                this.middleClickPearlFunction = new ClickPearl(),
                this.autoTotemFunction = new AutoTotem(),
                this.guiMove = new GuiMove(),
                this.autoRespawnFunction = new AutoRespawn(),
                this.autoSwapFunction = new AutoSwap(),
                this.syncTps = new SyncTps(),
                this.noPushFunction = new NoPush(),
                this.hitBoxFunction = new HitBox(),
                this.noSlow = new NoSlow(),
                this.antiTarget = new AntiTarget(),
                this.autoPotionFunction = new AutoPotion(),
                this.swingAnimationFunction = new CustomSwing(),
                this.gappleCooldownFunction = new ItemsCooldown(),
                this.optimization = new Optimization(),
                this.itemScroller = new ItemScroller(),
                this.aspect = new Aspect(),
                this.espFunction = new Esp(),
                this.noInteractFunction = new NoInteract(),
                this.ambience = new Ambience(),
                this.clientSounds = new ClientSounds(),
                this.itemPhysics = new ItemPhysics(),
                this.viewMode = new ViewMode(),
                this.hitColor = new HitColor(),
                this.auraFunction = new AttackAura(),
                this.elytraResolver = new ElytraResolver(),
                new UseTracker(),
                new BlockOverlay(),
                new AutoTool(),
                new ChestStealer(),
                new Tracers(),
                new NoFriendDamage(),
                new Predictions(),
                new AutoTpaccept(),
                new ClickFriend(),
                new JumpCircle(),
                this.autoExplosionFunction = new AutoExplosion(),
                new Trails(),
                new NoWeb(),
                new Speed(),
                new NoEntityTrace(),
                new AntiAfk(),
                new DeathCoords(),
                new ObsidianFarm(),
                new Spider(),
                freeCam = new FreeCam(),
                new NoClip(),
                new BlockEsp(),
                new TotemAngle(),
                this.nameTags = new NameTags(),
                new TriggerBot(),
                new AntiBot(),
                new AutoLeave(),
                new Chams(),
                new NoDelay(),
                new Particles(),
                new CatFly(),
                new ItemRelease(),
                new AutoEat(),
                new AimingBalls(),
                new BackTrack(),
                new Trajectories(),
                new LootTracker(),
                new ItemHelper(),
                this.itemFix = new ItemFix(),
                new FastBreak(),
                new AutoFarm(),
                new EcOpen(),
                new TapeMouse(),
                new TargetPearl(),
                new VoiceModeSound(),
                new NoFall(),
                new AutoDuel(),
                new Brightness(),
                new Arrows(),
                new AutoReple(),
                new CrystalOptimizer(),
                new ElytraJump(),
                new DragonFly(),
                new ItemSwapFix(),
                new Jesus(),
                streamerMode = new StreamerMode(),
                hud2 = new Interface()
        ));
    }

    public List<Module> getFunctions() {
        return modules;
    }

    public Module get(String name) {
        for (Module module : modules) {
            if (module != null && module.name.equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }
}
