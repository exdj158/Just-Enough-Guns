package ttv.migami.jeg.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.client.util.PropertyHelper;
import ttv.migami.jeg.common.GripType;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.compat.PlayerReviveHelper;
import ttv.migami.jeg.compat.ShoulderSurfingHelper;
import ttv.migami.jeg.debug.Debug;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.C2SMessageAim;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Author: MrCrayfish
 */
public class AimingHandler
{
    private static AimingHandler instance;

    public static AimingHandler get()
    {
        if(instance == null)
        {
            instance = new AimingHandler();
        }
        return instance;
    }

    private static final double MAX_AIM_PROGRESS = 5;
    private final AimTracker localTracker = new AimTracker();
    private final Map<Player, AimTracker> aimingMap = new WeakHashMap<>();
    private double normalisedAdsProgress;
    private boolean aiming = false;
    private boolean doTempFirstPerson = false;
    private boolean skipThirdPersonSwitch = false;

    private AimingHandler() {}

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if(event.phase != TickEvent.Phase.START)
            return;

        Player player = event.player;
        AimTracker tracker = getAimTracker(player);
        if(tracker != null)
        {
            tracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
            if(!tracker.isAiming())
            {
                this.aimingMap.remove(player);
            }
        }
    }

    @Nullable
    private AimTracker getAimTracker(Player player)
    {
        if(ModSyncedDataKeys.AIMING.getValue(player) && !this.aimingMap.containsKey(player))
        {
            this.aimingMap.put(player, new AimTracker());
        }
        return this.aimingMap.get(player);
    }

    public float getAimProgress(Player player, float partialTicks)
    {
        if(player.isLocalPlayer())
        {
            return (float) this.localTracker.getNormalProgress(partialTicks);
        }

        AimTracker tracker = this.getAimTracker(player);
        if(tracker != null)
        {
            return (float) tracker.getNormalProgress(partialTicks);
        }
        return 0F;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase != TickEvent.Phase.START)
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if(player == null)
            return;

        ItemStack heldItem = mc.player.getMainHandItem();
        GunItem gunItem;
        Gun modifiedGun = null;
        if (mc.player.getMainHandItem().getItem() instanceof GunItem)
        {
            gunItem = (GunItem) mc.player.getMainHandItem().getItem();
            modifiedGun = gunItem.getModifiedGun(mc.player.getMainHandItem());
        }
        boolean resetPOV = false;

        if(this.isAiming())
        {
            if (!mc.options.keySprint.isDown())
                player.setSprinting(false);
            if(!this.aiming)
            {
                ModSyncedDataKeys.AIMING.setValue(player, true);
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(true));
                this.aiming = true;
            }

            if (getNormalisedAdsProgress() > 0.0 && getNormalisedAdsProgress() <= 0.2) {
                player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
            }

            if (Config.CLIENT.display.forceFirstPersonOnZoomedAim.get() && getNormalisedAdsProgress() >= 0.2 && getNormalisedAdsProgress() <= 0.95)
            {
                if (!this.doTempFirstPerson && modifiedGun!=null)
                {
                    if(modifiedGun.getModules().getZoom() != null && Gun.getFovModifier(heldItem, modifiedGun) <= Config.CLIENT.display.firstPersonAimZoomThreshold.get())
                    {
                        if (ShoulderSurfingHelper.isShoulderSurfing())
                        {
                            this.doTempFirstPerson = true;
                            ShoulderSurfingHelper.changePerspective("FIRST_PERSON");
                            this.skipThirdPersonSwitch = false;
                        }
                    }
                    else
                    if (this.doTempFirstPerson)
                        resetPOV = true;
                }
            }
        }
        else
        {
            if (getNormalisedAdsProgress() > 0.8 && getNormalisedAdsProgress() < 1) {
                player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 0.8F);
            }
            if (this.doTempFirstPerson && getNormalisedAdsProgress()<=0.3)
                resetPOV = true;
            if(this.aiming)
            {
                ModSyncedDataKeys.AIMING.setValue(player, false);
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
                this.aiming = false;
            }
        }

        if (this.doTempFirstPerson)
        {
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON)
                this.skipThirdPersonSwitch = true;
            if(modifiedGun == null || modifiedGun.getModules().getZoom() == null
                    || Gun.getFovModifier(heldItem, modifiedGun) > Config.CLIENT.display.firstPersonAimZoomThreshold.get())
                resetPOV = true;
        }

        if (resetPOV && Config.CLIENT.display.forceFirstPersonOnZoomedAim.get())
        {
            this.doTempFirstPerson = false;
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON && !skipThirdPersonSwitch)
                ShoulderSurfingHelper.changePerspective("SHOULDER_SURFING");
        }

        this.localTracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
    }

    @SubscribeEvent
    public void onFovUpdate(ViewportEvent.ComputeFov event)
    {
        if(!event.usedConfiguredFov())
            return;

        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null || mc.player.getMainHandItem().isEmpty() || mc.options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        ItemStack heldItem = mc.player.getMainHandItem();
        if(!(heldItem.getItem() instanceof GunItem gunItem))
            return;

        if(AimingHandler.get().getNormalisedAdsProgress() == 0)
            return;

        if(ModSyncedDataKeys.RELOADING.getValue(mc.player))
            return;

        Gun modifiedGun = gunItem.getModifiedGun(heldItem);
        if(modifiedGun.getModules().getZoom() == null)
            return;

        double time = PropertyHelper.getSightAnimations(heldItem, modifiedGun).getFovCurve().apply(this.normalisedAdsProgress);
        float modifier = Gun.getFovModifier(heldItem, modifiedGun);
        modifier = (1.0F - modifier) * (float) time;
        event.setFOV(event.getFOV() - event.getFOV() * modifier);
    }

    @SubscribeEvent
    public void onClientTick(ClientPlayerNetworkEvent.LoggingOut event)
    {
        this.aimingMap.clear();
    }

    /**
     * Prevents the crosshair from rendering when aiming down sight
     */
    @SubscribeEvent(receiveCanceled = true)
    public void onRenderOverlay(RenderGuiOverlayEvent event)
    {
        this.normalisedAdsProgress = this.localTracker.getNormalProgress(event.getPartialTick());
    }

    public boolean isZooming()
    {
        return this.aiming;
    }

    public boolean isAiming()
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null)
            return false;

        if(mc.player.isSpectator())
            return false;

        if(Debug.isForceAim())
            return true;

        if(mc.screen != null)
            return false;

        if(PlayerReviveHelper.isBleeding(mc.player))
            return false;

        ItemStack heldItem = mc.player.getMainHandItem();
        if(!(heldItem.getItem() instanceof GunItem))
            return false;

        Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);
        if(!gun.canAimDownSight())
            return false;

        if(mc.player.getMainHandItem().getTag() != null) {
            if (mc.player.getMainHandItem().getTag().getBoolean("IsDrawing")) {
                return false;
            }
        }

        if(mc.player.getOffhandItem().getItem() instanceof ShieldItem && gun.getGeneral().getGripType() == GripType.ONE_HANDED)
            return false;

        if(!this.localTracker.isAiming() && this.isLookingAtInteractableBlock())
            return false;

        if(ModSyncedDataKeys.RELOADING.getValue(mc.player))
            return false;

        boolean zooming = KeyBinds.getAimMapping().isDown();
        if(JustEnoughGuns.controllableLoaded)
        {
            zooming |= ControllerHandler.isAiming();
        }

        return zooming;
    }

    public boolean isLookingAtInteractableBlock()
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.hitResult != null && mc.level != null)
        {
            if(mc.hitResult instanceof BlockHitResult result)
            {
                BlockState state = mc.level.getBlockState(result.getBlockPos());
                Block block = state.getBlock();
                // Forge should add a tag for intractable blocks so modders can know which blocks can be interacted with :)
                if (state.is(Blocks.IRON_DOOR) || state.is(Blocks.IRON_TRAPDOOR)) {
                    return false;
                }
                return (block instanceof EntityBlock && !state.is(ModBlocks.DYNAMIC_LIGHT.get())) || block == Blocks.CRAFTING_TABLE || block == ModBlocks.SCRAP_WORKBENCH.get() || block == ModBlocks.GUNMETAL_WORKBENCH.get() || block == ModBlocks.GUNNITE_WORKBENCH.get() || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(Tags.Blocks.CHESTS) || state.is(Tags.Blocks.FENCE_GATES);
            }
            else if(mc.hitResult instanceof EntityHitResult result)
            {
                return result.getEntity() instanceof ItemFrame;
            }
        }
        return false;
    }

    public double getNormalisedAdsProgress()
    {
        return this.normalisedAdsProgress;
    }

    public class AimTracker
    {
        private double currentAim;
        private double previousAim;

        private void handleAiming(Player player, ItemStack heldItem)
        {
            this.previousAim = this.currentAim;
            if(ModSyncedDataKeys.AIMING.getValue(player) || (player.isLocalPlayer() && AimingHandler.this.isAiming()))
            {
                if(this.currentAim < MAX_AIM_PROGRESS)
                {
                    double speed = GunEnchantmentHelper.getAimDownSightSpeed(heldItem);
                    speed = GunModifierHelper.getModifiedAimDownSightSpeed(heldItem, speed);
                    this.currentAim += speed;
                    if(this.currentAim > MAX_AIM_PROGRESS)
                    {
                        this.currentAim = (int) MAX_AIM_PROGRESS;
                    }
                }
            }
            else
            {
                if(this.currentAim > 0)
                {
                    double speed = GunEnchantmentHelper.getAimDownSightSpeed(heldItem);
                    speed = GunModifierHelper.getModifiedAimDownSightSpeed(heldItem, speed);
                    this.currentAim -= speed;
                    if(this.currentAim < 0)
                    {
                        this.currentAim = 0;
                    }
                }
            }
        }

        public boolean isAiming()
        {
            return this.currentAim != 0 || this.previousAim != 0;
        }

        public double getNormalProgress(float partialTicks)
        {
            return Mth.clamp((this.previousAim + (this.currentAim - this.previousAim) * partialTicks) / MAX_AIM_PROGRESS, 0.0, 1.0);
        }
    }
}
