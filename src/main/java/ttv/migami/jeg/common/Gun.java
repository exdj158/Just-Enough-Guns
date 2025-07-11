package ttv.migami.jeg.common;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.annotation.Ignored;
import ttv.migami.jeg.annotation.Optional;
import ttv.migami.jeg.client.ClientHandler;
import ttv.migami.jeg.debug.Debug;
import ttv.migami.jeg.debug.IDebugWidget;
import ttv.migami.jeg.debug.IEditorMenu;
import ttv.migami.jeg.debug.client.screen.widget.DebugButton;
import ttv.migami.jeg.debug.client.screen.widget.DebugSlider;
import ttv.migami.jeg.debug.client.screen.widget.DebugToggle;
import ttv.migami.jeg.init.ModEnchantments;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.attachment.item.ScopeItem;
import ttv.migami.jeg.item.attachment.IAttachment;
import ttv.migami.jeg.item.attachment.impl.Scope;
import ttv.migami.jeg.util.GunJsonUtil;
import ttv.migami.jeg.util.SuperBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class Gun implements INBTSerializable<CompoundTag>, IEditorMenu
{
    protected General general = new General();
    protected Reloads reloads = new Reloads();
    protected Projectile projectile = new Projectile();
    protected PotionEffect potionEffect = new PotionEffect();
    protected Sounds sounds = new Sounds();
    protected Display display = new Display();
    protected Modules modules = new Modules();

    public General getGeneral() { return this.general; }

    public Reloads getReloads() { return this.reloads; }

    public Projectile getProjectile()
    {
        return this.projectile;
    }

    public PotionEffect getPotionEffect()
    {
        return this.potionEffect;
    }

    public Sounds getSounds()
    {
        return this.sounds;
    }

    public Display getDisplay()
    {
        return this.display;
    }

    public Modules getModules()
    {
        return this.modules;
    }

    @Override
    public Component getEditorLabel()
    {
        return Component.translatable("Gun");
    }

    @Override
    public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ItemStack heldItem = Objects.requireNonNull(Minecraft.getInstance().player).getMainHandItem();
            ItemStack scope = Gun.getScopeStack(heldItem);
            if(scope.getItem() instanceof ScopeItem scopeItem)
            {
                widgets.add(Pair.of(scope.getItem().getName(scope), () -> new DebugButton(Component.translatable("Edit"), btn -> {
                    Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(Debug.getScope(scopeItem)));
                })));
            }

            widgets.add(Pair.of(this.modules.getEditorLabel(), () -> new DebugButton(Component.translatable(">"), btn -> {
                Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(this.modules));
            })));
        });
    }

    public static class General implements INBTSerializable<CompoundTag>
    {
        @Ignored
        private FireMode fireMode = FireMode.SEMI_AUTO;
        @Optional
        private int burstAmount;
        @Optional
        private int burstDelay;
        private int rate;
        @Optional
        private int fireTimer;
        @Optional
        private int maxHoldFire;
        @Optional
        private int overheatTimer;
        private int drawTimer = 20;
        private boolean silenced;
        private boolean customFiring;
        @Ignored
        private GripType gripType = GripType.ONE_HANDED;
        @Optional
        private float shooterPushback;
        //@Optional
        private float recoilAngle;
        @Optional
        private float recoilKick;
        @Optional
        private float recoilDurationOffset;
        @Optional
        private float recoilAdsReduction = 0.2F;
        @Optional
        private int projectileAmount = 1;
        @Optional
        private boolean alwaysSpread;
        @Optional
        private float spread;
        @Optional
        private boolean canBeBlueprinted = true;
        private boolean infinityDisabled;
        private boolean witheredDisabled;
        private boolean canFireUnderwater = false;

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString("FireMode", this.fireMode.getId().toString());
            tag.putInt("BurstAmount", this.burstAmount);
            tag.putInt("BurstDelay", this.burstDelay);
            tag.putInt("Rate", this.rate);
            tag.putInt("FireTimer", this.fireTimer);
            tag.putInt("MaxHoldFire", this.maxHoldFire);
            tag.putInt("OverheatTimer", this.overheatTimer);
            tag.putInt("DrawTimer", this.drawTimer);
            tag.putBoolean("Silenced", this.silenced);
            tag.putBoolean("CustomFiring", this.customFiring);
            tag.putString("GripType", this.gripType.getId().toString());
            tag.putFloat("ShooterPushback", this.shooterPushback);
            tag.putFloat("RecoilAngle", this.recoilAngle);
            tag.putFloat("RecoilKick", this.recoilKick);
            tag.putFloat("RecoilDurationOffset", this.recoilDurationOffset);
            tag.putFloat("RecoilAdsReduction", this.recoilAdsReduction);
            tag.putInt("ProjectileAmount", this.projectileAmount);
            tag.putBoolean("AlwaysSpread", this.alwaysSpread);
            tag.putFloat("Spread", this.spread);
            tag.putBoolean("CanBeBlueprinted", this.canBeBlueprinted);
            tag.putBoolean("InfinityDisabled", this.infinityDisabled);
            tag.putBoolean("WitheredDisabled", this.witheredDisabled);
            tag.putBoolean("CanFireUnderwater", this.canFireUnderwater);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            if(tag.contains("FireMode", Tag.TAG_STRING))
            {
                this.fireMode = FireMode.getType(ResourceLocation.tryParse(tag.getString("FireMode")));
            }
            if(tag.contains("BurstAmount", Tag.TAG_ANY_NUMERIC))
            {
                this.burstAmount = tag.getInt("BurstAmount");
            }
            if(tag.contains("BurstDelay", Tag.TAG_ANY_NUMERIC))
            {
                this.burstDelay = tag.getInt("BurstDelay");
            }
            if(tag.contains("Rate", Tag.TAG_ANY_NUMERIC))
            {
                this.rate = tag.getInt("Rate");
            }
            if(tag.contains("FireTimer", Tag.TAG_ANY_NUMERIC))
            {
                this.fireTimer = tag.getInt("FireTimer");
            }
            if(tag.contains("MaxHoldFire", Tag.TAG_ANY_NUMERIC))
            {
                this.maxHoldFire = tag.getInt("MaxHoldFire");
            }
            if(tag.contains("OverheatTimer", Tag.TAG_ANY_NUMERIC))
            {
                this.overheatTimer = tag.getInt("OverheatTimer");
            }
            if(tag.contains("DrawTimer", Tag.TAG_ANY_NUMERIC))
            {
                this.drawTimer = tag.getInt("DrawTimer");
            }
            if(tag.contains("Silenced", Tag.TAG_ANY_NUMERIC))
            {
                this.silenced = tag.getBoolean("Silenced");
            }
            if(tag.contains("CustomFiring", Tag.TAG_ANY_NUMERIC))
            {
                this.alwaysSpread = tag.getBoolean("CustomFiring");
            }
            if(tag.contains("GripType", Tag.TAG_STRING))
            {
                this.gripType = GripType.getType(ResourceLocation.tryParse(tag.getString("GripType")));
            }
            if(tag.contains("ShooterPushback", Tag.TAG_ANY_NUMERIC))
            {
                this.shooterPushback = tag.getFloat("ShooterPushback");
            }
            if(tag.contains("RecoilAngle", Tag.TAG_ANY_NUMERIC))
            {
                this.recoilAngle = tag.getFloat("RecoilAngle");
            }
            if(tag.contains("RecoilKick", Tag.TAG_ANY_NUMERIC))
            {
                this.recoilKick = tag.getFloat("RecoilKick");
            }
            if(tag.contains("RecoilDurationOffset", Tag.TAG_ANY_NUMERIC))
            {
                this.recoilDurationOffset = tag.getFloat("RecoilDurationOffset");
            }
            if(tag.contains("RecoilAdsReduction", Tag.TAG_ANY_NUMERIC))
            {
                this.recoilAdsReduction = tag.getFloat("RecoilAdsReduction");
            }
            if(tag.contains("ProjectileAmount", Tag.TAG_ANY_NUMERIC))
            {
                this.projectileAmount = tag.getInt("ProjectileAmount");
            }
            if(tag.contains("AlwaysSpread", Tag.TAG_ANY_NUMERIC))
            {
                this.alwaysSpread = tag.getBoolean("AlwaysSpread");
            }
            if(tag.contains("Spread", Tag.TAG_ANY_NUMERIC))
            {
                this.spread = tag.getFloat("Spread");
            }
            if(tag.contains("CanBeBlueprinted", Tag.TAG_ANY_NUMERIC))
            {
                this.canBeBlueprinted = tag.getBoolean("CanBeBlueprinted");
            }
            if(tag.contains("InfinityDisabled", Tag.TAG_ANY_NUMERIC))
            {
                this.infinityDisabled = tag.getBoolean("InfinityDisabled");
            }
            if(tag.contains("WitheredDisabled", Tag.TAG_ANY_NUMERIC))
            {
                this.witheredDisabled = tag.getBoolean("WitheredDisabled");
            }
            if(tag.contains("CanFireUnderwater", Tag.TAG_ANY_NUMERIC))
            {
                this.canFireUnderwater = tag.getBoolean("CanFireUnderwater");
            }
        }

        public JsonObject toJsonObject()
        {
            Preconditions.checkArgument(this.rate >= 0, "Rate must be more than zero");
            Preconditions.checkArgument(this.recoilAngle >= 0.0F, "Recoil angle must be more than or equal to zero");
            Preconditions.checkArgument(this.recoilKick >= 0.0F, "Recoil kick must be more than or equal to zero");
            Preconditions.checkArgument(this.recoilDurationOffset >= 0.0F && this.recoilDurationOffset <= 1.0F, "Recoil duration offset must be between 0.0 and 1.0");
            Preconditions.checkArgument(this.recoilAdsReduction >= 0.0F && this.recoilAdsReduction <= 1.0F, "Recoil ads reduction must be between 0.0 and 1.0");
            Preconditions.checkArgument(this.projectileAmount >= 1, "Projectile amount must be more than or equal to one");
            Preconditions.checkArgument(this.spread >= 0.0F, "Spread must be more than or equal to zero");
            JsonObject object = new JsonObject();
            object.addProperty("fireMode", this.fireMode.getId().toString());
            if(this.burstAmount != 0) object.addProperty("burstAmount", this.burstAmount);
            if(this.burstAmount != 0 && this.burstDelay != 0) object.addProperty("burstDelay", this.burstDelay);
            object.addProperty("rate", this.rate);
            if(this.fireTimer != 0) object.addProperty("fireTimer", this.fireTimer);
            if(this.maxHoldFire != 0) object.addProperty("maxHoldFire", this.maxHoldFire);
            if(this.overheatTimer != 0) object.addProperty("overheatTimer", this.overheatTimer);
            if(this.drawTimer != 0) object.addProperty("drawTimer", this.drawTimer);
            if(this.silenced) object.addProperty("silenced", true);
            if(this.customFiring) object.addProperty("customFiring", true);
            object.addProperty("gripType", this.gripType.getId().toString());
            if(this.recoilAngle != 0.0F) object.addProperty("recoilAngle", this.recoilAngle);
            if(this.recoilKick != 0.0F) object.addProperty("recoilKick", this.recoilKick);
            if(this.recoilDurationOffset != 0.0F) object.addProperty("recoilDurationOffset", this.recoilDurationOffset);
            if(this.recoilAdsReduction != 0.2F) object.addProperty("recoilAdsReduction", this.recoilAdsReduction);
            if(this.projectileAmount != 1) object.addProperty("projectileAmount", this.projectileAmount);
            if(this.alwaysSpread) object.addProperty("alwaysSpread", true);
            if(this.shooterPushback != 0.0F) object.addProperty("shooterPushback", this.shooterPushback);
            if(this.spread != 0.0F) object.addProperty("spread", this.spread);
            if(!this.canBeBlueprinted) object.addProperty("canBeBlueprinted", this.canBeBlueprinted);
            if(this.infinityDisabled) object.addProperty("infinityDisabled", this.infinityDisabled);
            if(this.witheredDisabled) object.addProperty("witheredDisabled", this.witheredDisabled);
            if(this.canFireUnderwater) object.addProperty("canFireUnderwater", this.canFireUnderwater);
            return object;
        }

        /**
         * @return A copy of the general get
         */
        public General copy()
        {
            General general = new General();
            general.fireMode = this.fireMode;
            general.burstAmount = this.burstAmount;
            general.burstDelay = this.burstDelay;
            general.rate = this.rate;
            general.fireTimer = this.fireTimer;
            general.maxHoldFire = this.maxHoldFire;
            general.overheatTimer = this.overheatTimer;
            general.drawTimer = this.drawTimer;
            general.silenced = this.silenced;
            general.customFiring = this.customFiring;
            general.gripType = this.gripType;
            general.recoilAngle = this.recoilAngle;
            general.recoilKick = this.recoilKick;
            general.recoilDurationOffset = this.recoilDurationOffset;
            general.recoilAdsReduction = this.recoilAdsReduction;
            general.projectileAmount = this.projectileAmount;
            general.alwaysSpread = this.alwaysSpread;
            general.shooterPushback = this.shooterPushback;
            general.spread = this.spread;
            general.canBeBlueprinted = this.canBeBlueprinted;
            general.infinityDisabled = this.infinityDisabled;
            general.witheredDisabled = this.witheredDisabled;
            general.canFireUnderwater = this.canFireUnderwater;
            return general;
        }

        /**
         * @return The type of grip this weapon uses
         */
        public FireMode getFireMode()
        {
            return this.fireMode;
        }

        /**
         * @return The projectile amount in a burst
         */
        public int getBurstAmount()
        {
            return this.burstAmount;
        }

        /**
         * @return The burst delay
         */
        public int getBurstDelay()
        {
            return this.burstDelay;
        }

        /**
         * @return The fire rate of this weapon in ticks
         */
        public int getRate()
        {
            return this.rate;
        }

        /**
         * @return The timer before firing
         */
        public int getFireTimer()
        {
            return this.fireTimer;
        }

        /**
         * @return The max timer until the gun reaches its full capacity
         */
        public int getMaxHoldFire()
        {
            return this.maxHoldFire;
        }

        /**
         * @return The max timer until the gun overheats
         */
        public int getOverheatTimer()
        {
            return this.overheatTimer;
        }

        /**
         * @return The delay in ticks for the gun to be drawn
         */
        public int getDrawTimer()
        {
            return this.drawTimer;
        }

        /**
         * @return Is the gun natively-silenced
         */
        public boolean isSilenced()
        {
            return this.silenced;
        }

        /**
         * @return Has the gun a custom firing event? (Typhoonee and Atlantean Spear)
         */
        public boolean isCustomFiring()
        {
            return this.customFiring;
        }

        /**
         * @return The type of grip this weapon uses
         */
        public GripType getGripType()
        {
            return this.gripType;
        }

        /**
         * @return The amount of recoil this gun produces upon firing in degrees
         */
        public float getRecoilAngle()
        {
            return this.recoilAngle;
        }

        /**
         * @return The amount of kick this gun produces upon firing
         */
        public float getRecoilKick()
        {
            return this.recoilKick;
        }

        /**
         * @return The duration offset for recoil. This reduces the duration of recoil animation
         */
        public float getRecoilDurationOffset()
        {
            return this.recoilDurationOffset;
        }

        /**
         * @return The amount of reduction applied when aiming down this weapon's sight
         */
        public float getRecoilAdsReduction()
        {
            return this.recoilAdsReduction;
        }

        /**
         * @return The amount of projectiles this weapon fires
         */
        public int getProjectileAmount()
        {
            return this.projectileAmount;
        }

        /**
         * @return If this weapon should always spread it's projectiles according to {@link #getSpread()}
         */
        public boolean isAlwaysSpread()
        {
            return this.alwaysSpread;
        }


        /**
         * @return The amount of pushback applied to the shooter!
         */
        public float getShooterPushback()
        {
            return this.shooterPushback;
        }

        /**
         * @return The maximum amount of degrees applied to the initial pitch and yaw direction of
         * the fired projectile.
         */
        public float getSpread()
        {
            return this.spread;
        }

        /**
         * @return If false, the gun will not be able to be copied into a blueprint.
         */
        public boolean canBeBlueprinted()
        {
            return this.canBeBlueprinted;
        }

        /**
         * @return If true, the gun will not be able to obtain Infinity by killing the Dragon.
         */
        public boolean isInfinityDisabled()
        {
            return this.infinityDisabled;
        }

        /**
         * @return If true, the gun will not be able to obtain Withered by killing the Wither.
         */
        public boolean isWitheredDisabled()
        {
            return this.witheredDisabled;
        }

        /**
         * @return If true, the gun will be able to fire underwater.
         */
        public boolean canFireUnderwater()
        {
            return this.canFireUnderwater;
        }
    }

    public static class Reloads implements INBTSerializable<CompoundTag>
    {
        @Optional
        @Ignored
        private ResourceLocation reloadItem = new ResourceLocation(Reference.MOD_ID, "scrap");
        private int maxAmmo = 30;
        @Ignored
        private ReloadType reloadType = ReloadType.MANUAL;
        private int reloadTimer = 20;
        private int additionalReloadTimer = 5;
        private int reloadAmount = 1;

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString("ReloadItem", this.reloadItem.toString());
            tag.putInt("MaxAmmo", this.maxAmmo);
            tag.putString("ReloadType", this.reloadType.getId().toString());
            tag.putInt("ReloadTimer", this.reloadTimer);
            tag.putInt("AdditionalReloadTimer", this.additionalReloadTimer);
            tag.putInt("ReloadAmount", this.reloadAmount);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            if(tag.contains("ReloadItem", Tag.TAG_STRING))
            {
                this.reloadItem = new ResourceLocation(tag.getString("ReloadItem"));
            }
            if(tag.contains("MaxAmmo", Tag.TAG_ANY_NUMERIC))
            {
                this.maxAmmo = tag.getInt("MaxAmmo");
            }
            if(tag.contains("ReloadType", Tag.TAG_STRING))
            {
                this.reloadType = ReloadType.getType(ResourceLocation.tryParse(tag.getString("ReloadType")));
            }
            if(tag.contains("ReloadTimer", Tag.TAG_ANY_NUMERIC))
            {
                this.reloadTimer = tag.getInt("ReloadTimer");
            }
            if(tag.contains("AdditionalReloadTimer", Tag.TAG_ANY_NUMERIC))
            {
                this.additionalReloadTimer = tag.getInt("AdditionalReloadTimer");
            }
            if(tag.contains("ReloadAmount", Tag.TAG_ANY_NUMERIC))
            {
                this.reloadAmount = tag.getInt("ReloadAmount");
            }
        }

        public JsonObject toJsonObject()
        {
            Preconditions.checkArgument(this.maxAmmo > 0, "Max ammo must be more than zero");
            Preconditions.checkArgument(this.reloadTimer >= 0, "Reload timer must be more than or equal to zero");
            Preconditions.checkArgument(this.additionalReloadTimer >= 0, "Empty mag additional reload timer must be more than or equal to zero");
            Preconditions.checkArgument(this.reloadAmount >= 1, "Reloading amount must be more than or equal to zero");
            JsonObject object = new JsonObject();
            if(!this.reloadItem.toString().matches(ModItems.SCRAP.getId().toString())) object.addProperty("reloadItem", this.reloadItem.toString());
            object.addProperty("maxAmmo", this.maxAmmo);
            object.addProperty("reloadType", this.reloadType.getId().toString());
            object.addProperty("reloadTimer", this.reloadTimer);
            object.addProperty("additionalReloadTimer", this.additionalReloadTimer);
            if(this.reloadAmount != 1) object.addProperty("reloadAmount", this.reloadAmount);
            return object;
        }

        public Reloads copy()
        {
            Reloads reloads = new Reloads();
            reloads.reloadItem = this.reloadItem;
            reloads.maxAmmo = this.maxAmmo;
            reloads.reloadType = this.reloadType;
            reloads.reloadTimer = this.reloadTimer;
            reloads.additionalReloadTimer = this.additionalReloadTimer;
            reloads.reloadAmount = this.reloadAmount;
            return reloads;
        }

        /**
         * @return The registry id of the reload item
         */
        public ResourceLocation getReloadItem()
        {
            return this.reloadItem;
        }

        public int getMaxAmmo() { return this.maxAmmo; }

        public ReloadType getReloadType() { return this.reloadType; }

        public int getReloadTimer() { return this.reloadTimer; }

        public int getAdditionalReloadTimer() { return this.additionalReloadTimer; }

        public int getReloadAmount() { return this.reloadAmount; }
    }

    public static class Projectile implements INBTSerializable<CompoundTag>
    {
        private ResourceLocation item = new ResourceLocation(Reference.MOD_ID, "pistol_ammo");
        @Optional
        private boolean ejectsCasing;
        @Optional
        private boolean visible;
        private boolean ignoresBlocks;
        private boolean collateral;
        private float damage;
        private float headshotMultiplier = 1.5F;
        @Optional
        private ResourceLocation advantage = new ResourceLocation(Reference.MOD_ID, "none");
        private float size;
        private double speed;
        private int life;
        @Optional
        private boolean gravity;
        @Optional
        private boolean damageReduceOverLife;
        @Optional
        private int trailColor = 0xFFD289;
        @Optional
        private double trailLengthMultiplier = 1.0;
        private boolean hideTrail;
        private boolean noProjectile;
        private boolean hitsRubberFruit;

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString("Item", this.item.toString());
            tag.putBoolean("EjectsCasing", this.ejectsCasing);
            tag.putBoolean("Visible", this.visible);
            tag.putBoolean("IgnoresBlocks", this.ignoresBlocks);
            tag.putBoolean("Collateral", this.collateral);
            tag.putFloat("Damage", this.damage);
            tag.putFloat("HeadshotMultiplier", this.headshotMultiplier);
            tag.putString("Advantage", this.advantage.toString());
            tag.putFloat("Size", this.size);
            tag.putDouble("Speed", this.speed);
            tag.putInt("Life", this.life);
            tag.putBoolean("Gravity", this.gravity);
            tag.putBoolean("DamageReduceOverLife", this.damageReduceOverLife);
            tag.putInt("TrailColor", this.trailColor);
            tag.putDouble("TrailLengthMultiplier", this.trailLengthMultiplier);
            tag.putBoolean("HideTrail", this.hideTrail);
            tag.putBoolean("NoProjectile", this.noProjectile);
            tag.putBoolean("HitsRubberFruit", this.hitsRubberFruit);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            if(tag.contains("Item", Tag.TAG_STRING))
            {
                this.item = new ResourceLocation(tag.getString("Item"));
            }
            if(tag.contains("EjectsCasing", Tag.TAG_ANY_NUMERIC))
            {
                this.ejectsCasing = tag.getBoolean("EjectsCasing");
            }
            if(tag.contains("Visible", Tag.TAG_ANY_NUMERIC))
            {
                this.visible = tag.getBoolean("Visible");
            }
            if(tag.contains("IgnoresBlocks", Tag.TAG_ANY_NUMERIC))
            {
                this.ignoresBlocks = tag.getBoolean("IgnoresBlocks");
            }
            if(tag.contains("Collateral", Tag.TAG_ANY_NUMERIC))
            {
                this.collateral = tag.getBoolean("IgnoresBlocksCollateral");
            }
            if(tag.contains("Damage", Tag.TAG_ANY_NUMERIC))
            {
                this.damage = tag.getFloat("Damage");
            }
            if(tag.contains("HeadshotMultiplier", Tag.TAG_ANY_NUMERIC))
            {
                this.headshotMultiplier = tag.getFloat("HeadshotMultiplier");
            }
            if(tag.contains("Advantage", Tag.TAG_STRING))
            {
                this.advantage = new ResourceLocation(tag.getString("Advantage"));
            }
            if(tag.contains("Size", Tag.TAG_ANY_NUMERIC))
            {
                this.size = tag.getFloat("Size");
            }
            if(tag.contains("Speed", Tag.TAG_ANY_NUMERIC))
            {
                this.speed = tag.getDouble("Speed");
            }
            if(tag.contains("Life", Tag.TAG_ANY_NUMERIC))
            {
                this.life = tag.getInt("Life");
            }
            if(tag.contains("Gravity", Tag.TAG_ANY_NUMERIC))
            {
                this.gravity = tag.getBoolean("Gravity");
            }
            if(tag.contains("DamageReduceOverLife", Tag.TAG_ANY_NUMERIC))
            {
                this.damageReduceOverLife = tag.getBoolean("DamageReduceOverLife");
            }
            if(tag.contains("TrailColor", Tag.TAG_ANY_NUMERIC))
            {
                this.trailColor = tag.getInt("TrailColor");
            }
            if(tag.contains("TrailLengthMultiplier", Tag.TAG_ANY_NUMERIC))
            {
                this.trailLengthMultiplier = tag.getDouble("TrailLengthMultiplier");
            }
            if(tag.contains("HideTrail", Tag.TAG_ANY_NUMERIC))
            {
                this.hideTrail = tag.getBoolean("HideTrail");
            }
            if(tag.contains("NoProjectile", Tag.TAG_ANY_NUMERIC))
            {
                this.noProjectile = tag.getBoolean("NoProjectile");
            }
            if(tag.contains("HitsRubberFruit", Tag.TAG_ANY_NUMERIC))
            {
                this.hitsRubberFruit = tag.getBoolean("HitsRubberFruit");
            }
        }

        public JsonObject toJsonObject()
        {
            Preconditions.checkArgument(this.damage >= 0.0F, "Damage must be more than or equal to zero");
            Preconditions.checkArgument(this.size >= 0.0F, "Projectile size must be more than or equal to zero");
            Preconditions.checkArgument(this.speed >= 0.0, "Projectile speed must be more than or equal to zero");
            Preconditions.checkArgument(this.life > 0, "Projectile life must be more than zero");
            Preconditions.checkArgument(this.trailLengthMultiplier >= 0.0, "Projectile trail length multiplier must be more than or equal to zero");
            JsonObject object = new JsonObject();
            object.addProperty("item", this.item.toString());
            object.addProperty("headshotMultiplier", this.headshotMultiplier);
            if(this.ejectsCasing) object.addProperty("ejectsCasing", true);
            if(this.visible) object.addProperty("visible", true);
            if(this.ignoresBlocks) object.addProperty("ignoresBlocks", true);
            if(this.collateral) object.addProperty("collateral", true);
            object.addProperty("damage", this.damage);
            if(this.advantage != null) object.addProperty("advantage", this.advantage.toString());
            object.addProperty("size", this.size);
            object.addProperty("speed", this.speed);
            object.addProperty("life", this.life);
            if(this.gravity) object.addProperty("gravity", true);
            if(this.damageReduceOverLife) object.addProperty("damageReduceOverLife", this.damageReduceOverLife);
            if(this.trailColor != 0xFFFF00) object.addProperty("trailColor", this.trailColor);
            if(this.trailLengthMultiplier != 1.0) object.addProperty("trailLengthMultiplier", this.trailLengthMultiplier);
            if(this.hideTrail) object.addProperty("hideTrail", true);
            if(this.noProjectile) object.addProperty("noProjectile", true);
            if(this.hitsRubberFruit) object.addProperty("hitsRubberFruit", true);
            return object;
        }

        public Projectile copy()
        {
            Projectile projectile = new Projectile();
            projectile.item = this.item;
            projectile.ejectsCasing = this.ejectsCasing;
            projectile.visible = this.visible;
            projectile.ignoresBlocks = this.ignoresBlocks;
            projectile.collateral = this.collateral;
            projectile.damage = this.damage;
            projectile.headshotMultiplier = this.headshotMultiplier;
            projectile.advantage = this.advantage;
            projectile.size = this.size;
            projectile.speed = this.speed;
            projectile.life = this.life;
            projectile.gravity = this.gravity;
            projectile.damageReduceOverLife = this.damageReduceOverLife;
            projectile.trailColor = this.trailColor;
            projectile.trailLengthMultiplier = this.trailLengthMultiplier;
            projectile.hideTrail = this.hideTrail;
            projectile.noProjectile = this.noProjectile;
            projectile.hitsRubberFruit = this.hitsRubberFruit;
            return projectile;
        }

        /**
         * @return The registry id of the ammo item
         */
        public ResourceLocation getItem()
        {
            return this.item;
        }

        /**
         * @return If this projectile ejects a casing/shell when fired
         */
        public boolean ejectsCasing()
        {
            return this.ejectsCasing;
        }

        /**
         * @return If this projectile should be visible when rendering
         */
        public boolean isVisible()
        {
            return this.visible;
        }

        /**
         * @return If this projectile ignores blocks
         */
        public boolean ignoresBlocks()
        {
            return this.ignoresBlocks;
        }

        /**
         * @return If this projectile ignores entity collisions
         */
        public boolean isCollateral()
        {
            return this.collateral;
        }

        /**
         * @return The damage caused by this projectile
         */
        public float getDamage()
        {
            return this.damage;
        }

        /**
         * @return The headshot multiplier of each gun
         */
        public float getHeadshotMultiplier()
        {
            return this.headshotMultiplier;
        }

        /**
         * @return The damage caused by this projectile
         */
        public ResourceLocation getAdvantage()
        {
            return this.advantage;
        }

        /**
         * @return The size of the projectile entity bounding box
         */
        public float getSize()
        {
            return this.size;
        }

        /**
         * @return The speed the projectile moves every tick
         */
        public double getSpeed()
        {
            return this.speed;
        }

        /**
         * @return The amount of ticks before this projectile is removed
         */
        public int getLife()
        {
            return this.life;
        }

        /**
         * @return If gravity should be applied to the projectile
         */
        public boolean isGravity()
        {
            return this.gravity;
        }

        /**
         * @return If the damage should reduce the further the projectile travels
         */
        public boolean isDamageReduceOverLife()
        {
            return this.damageReduceOverLife;
        }

        /**
         * @return The color of the projectile trail in rgba integer format
         */
        public int getTrailColor()
        {
            return this.trailColor;
        }

        /**
         * @return The multiplier to change the length of the projectile trail
         */
        public double getTrailLengthMultiplier()
        {
            return this.trailLengthMultiplier;
        }

        /**
         * @return If the projectile's trail should be hidden
         */
        public boolean hideTrail()
        {
            return this.hideTrail;
        }

        /**
         * @return If the gun has a "bullet" or not
         */
        public boolean hasProjectile()
        {
            return !this.noProjectile;
        }

        /**
         * @return If the projectile hits a Rubber Fruit user from MigaMi's Devil Fruits
         */
        public boolean hitsRubberFruit()
        {
            return this.hitsRubberFruit;
        }
    }

    public static class PotionEffect implements INBTSerializable<CompoundTag>
    {
        @Optional
        private boolean selfPotionEffect;
        @Optional
        @Nullable
        private ResourceLocation potionEffect;
        @Optional
        private int potionEffectStrength;
        @Optional
        private int potionEffectDuration;

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            if (this.potionEffect != null) {
                tag.putBoolean("SelfPotionEffect", this.selfPotionEffect);
                tag.putString("PotionEffect", this.potionEffect.toString());
                tag.putInt("PotionEffectStrength", this.potionEffectStrength);
                tag.putInt("PotionEffectDuration", this.potionEffectDuration);
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            if(tag.contains("SelfPotionEffect", Tag.TAG_ANY_NUMERIC))
            {
                this.selfPotionEffect = tag.getBoolean("SelfPotionEffect");
            }
            if(tag.contains("PotionEffect", Tag.TAG_STRING))
            {
                this.potionEffect = new ResourceLocation(tag.getString("PotionEffect"));
            }
            if(tag.contains("PotionEffectStrength", Tag.TAG_ANY_NUMERIC))
            {
                this.potionEffectStrength = tag.getInt("PotionEffectStrength");
            }
            if(tag.contains("PotionEffectDuration", Tag.TAG_ANY_NUMERIC))
            {
                this.potionEffectDuration = tag.getInt("PotionEffectDuration");
            }
        }

        public JsonObject toJsonObject()
        {
            JsonObject object = new JsonObject();
            if(this.potionEffect != null) {
                if(this.selfPotionEffect) object.addProperty("selfPotionEffect", this.selfPotionEffect);
                object.addProperty("potionEffect", this.potionEffect.toString());
                if(this.potionEffectStrength > 0) {
                    object.addProperty("potionEffectStrength", this.potionEffectStrength);
                } else {
                    object.addProperty("potionEffectStrength", 0);
                }
                if(this.potionEffectDuration > 0) {
                    object.addProperty("potionEffectDuration", this.potionEffectDuration);
                } else {
                    object.addProperty("potionEffectDuration", 20);
                }
            }
            return object;
        }

        public PotionEffect copy()
        {
            PotionEffect potionEffect = new PotionEffect();
            potionEffect.selfPotionEffect = this.selfPotionEffect;
            potionEffect.potionEffect = this.potionEffect;
            potionEffect.potionEffectStrength = this.potionEffectStrength;
            potionEffect.potionEffectDuration = this.potionEffectDuration;
            return potionEffect;
        }

        /**
         * @return Is the effect added to the shooter?
         */
        public boolean isSelfApplied()
        {
            return this.selfPotionEffect;
        }

        /**
         * @return The effect added by this projectile
         */
        public ResourceLocation getPotionEffect()
        {
            return this.potionEffect;
        }

        /**
         * @return The effect's strength
         */
        public int getPotionEffectStrength()
        {
            return this.potionEffectStrength;
        }

        /**
         * @return The effects duration
         */
        public int getPotionEffectDuration()
        {
            return this.potionEffectDuration;
        }
    }

    public static class Sounds implements INBTSerializable<CompoundTag>
    {
        @Optional
        @Nullable
        private ResourceLocation fire;
        @Optional
        @Nullable
        private ResourceLocation reloadStart;
        @Optional
        @Nullable
        private ResourceLocation reloadLoad;
        @Optional
        @Nullable
        private ResourceLocation reloadEnd;
        @Optional
        @Nullable
        private ResourceLocation ejectorPull;
        @Optional
        @Nullable
        private ResourceLocation ejectorRelease;
        @Optional
        @Nullable
        private ResourceLocation silencedFire;
        @Optional
        @Nullable
        private ResourceLocation enchantedFire;
        @Optional
        @Nullable
        private ResourceLocation preFire;

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            if(this.fire != null)
            {
                tag.putString("Fire", this.fire.toString());
            }
            if(this.reloadStart != null)
            {
                tag.putString("ReloadStart", this.reloadStart.toString());
            }
            if(this.reloadLoad != null)
            {
                tag.putString("ReloadLoad", this.reloadLoad.toString());
            }
            if(this.reloadEnd != null)
            {
                tag.putString("ReloadEnd", this.reloadEnd.toString());
            }
            if(this.ejectorPull != null)
            {
                tag.putString("EjectorPull", this.ejectorPull.toString());
            }
            if(this.ejectorRelease != null)
            {
                tag.putString("EjectorRelease", this.ejectorRelease.toString());
            }
            if(this.silencedFire != null)
            {
                tag.putString("SilencedFire", this.silencedFire.toString());
            }
            if(this.enchantedFire != null)
            {
                tag.putString("EnchantedFire", this.enchantedFire.toString());
            }
            if(this.preFire != null)
            {
                tag.putString("PreFire", this.preFire.toString());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            if(tag.contains("Fire", Tag.TAG_STRING))
            {
                this.fire = this.createSound(tag, "Fire");
            }
            if(tag.contains("ReloadStart", Tag.TAG_STRING))
            {
                this.reloadStart = this.createSound(tag, "ReloadStart");
            }
            if(tag.contains("ReloadLoad", Tag.TAG_STRING))
            {
                this.reloadLoad = this.createSound(tag, "ReloadLoad");
            }
            if(tag.contains("ReloadEnd", Tag.TAG_STRING))
            {
                this.reloadEnd = this.createSound(tag, "ReloadEnd");
            }
            if(tag.contains("EjectorPull", Tag.TAG_STRING))
            {
                this.ejectorPull = this.createSound(tag, "EjectorPull");
            }
            if(tag.contains("EjectorRelease", Tag.TAG_STRING))
            {
                this.ejectorRelease = this.createSound(tag, "EjectorRelease");
            }
            if(tag.contains("SilencedFire", Tag.TAG_STRING))
            {
                this.silencedFire = this.createSound(tag, "SilencedFire");
            }
            if(tag.contains("EnchantedFire", Tag.TAG_STRING))
            {
                this.enchantedFire = this.createSound(tag, "EnchantedFire");
            }
            if(tag.contains("PreFire", Tag.TAG_STRING))
            {
                this.preFire = this.createSound(tag, "PreFire");
            }
        }

        public JsonObject toJsonObject()
        {
            JsonObject object = new JsonObject();
            if(this.fire != null)
            {
                object.addProperty("fire", this.fire.toString());
            }
            if(this.reloadStart != null)
            {
                object.addProperty("reloadStart", this.reloadStart.toString());
            }
            if(this.reloadLoad != null)
            {
                object.addProperty("reloadLoad", this.reloadLoad.toString());
            }
            if(this.reloadEnd != null)
            {
                object.addProperty("reloadEnd", this.reloadEnd.toString());
            }
            if(this.ejectorPull != null)
            {
                object.addProperty("ejectorPull", this.ejectorPull.toString());
            }
            if(this.ejectorRelease != null)
            {
                object.addProperty("ejectorRelease", this.ejectorRelease.toString());
            }
            if(this.silencedFire != null)
            {
                object.addProperty("silencedFire", this.silencedFire.toString());
            }
            if(this.enchantedFire != null)
            {
                object.addProperty("enchantedFire", this.enchantedFire.toString());
            }
            if(this.preFire != null)
            {
                object.addProperty("preFire", this.preFire.toString());
            }
            return object;
        }

        public Sounds copy()
        {
            Sounds sounds = new Sounds();
            sounds.fire = this.fire;
            sounds.reloadStart = this.reloadStart;
            sounds.reloadLoad = this.reloadLoad;
            sounds.reloadEnd = this.reloadEnd;
            sounds.ejectorPull = this.ejectorPull;
            sounds.ejectorRelease = this.ejectorRelease;
            sounds.silencedFire = this.silencedFire;
            sounds.enchantedFire = this.enchantedFire;
            sounds.preFire = this.preFire;
            return sounds;
        }

        @Nullable
        private ResourceLocation createSound(CompoundTag tag, String key)
        {
            String sound = tag.getString(key);
            return sound.isEmpty() ? null : new ResourceLocation(sound);
        }

        /**
         * @return The registry id of the sound event when firing this weapon
         */
        @Nullable
        public ResourceLocation getFire()
        {
            return this.fire;
        }

        /**
         * @return The registry iid of the sound event when reloading this weapon
         */
        @Nullable
        public ResourceLocation getReloadStart()
        {
            return this.reloadStart;
        }

        /**
         * @return The registry iid of the sound event when loading this weapon
         */
        @Nullable
        public ResourceLocation getReloadLoad()
        {
            return this.reloadLoad;
        }

        /**
         * @return The registry iid of the sound event when ending the reload
         */
        @Nullable
        public ResourceLocation getReloadEnd()
        {
            return this.reloadEnd;
        }

        /**
         * @return The registry iid of the sound event when cocking this weapon
         */
        @Nullable
        public ResourceLocation getEjectorPull()
        {
            return this.ejectorPull;
        }

        /**
         * @return The registry iid of the sound event when releasing the ejector
         */
        @Nullable
        public ResourceLocation getEjectorRelease()
        {
            return this.ejectorRelease;
        }

        /**
         * @return The registry iid of the sound event when silenced firing this weapon
         */
        @Nullable
        public ResourceLocation getSilencedFire()
        {
            return this.silencedFire;
        }

        /**
         * @return The registry iid of the sound event when silenced firing this weapon
         */
        @Nullable
        public ResourceLocation getEnchantedFire()
        {
            return this.enchantedFire;
        }

        /**
         * @return The registry iid of the sound event when preparing to fire this weapon
         */
        @Nullable
        public ResourceLocation getPreFire()
        {
            return this.preFire;
        }
    }

    public static class Display implements INBTSerializable<CompoundTag>
    {
        @Optional
        @Nullable
        protected Flash flash;

        @Nullable
        public Flash getFlash()
        {
            return this.flash;
        }

        public static class Flash extends Positioned
        {
            private double size = 0.5;

            @Override
            public CompoundTag serializeNBT()
            {
                CompoundTag tag = super.serializeNBT();
                tag.putDouble("Size", this.size);
                return tag;
            }

            @Override
            public void deserializeNBT(CompoundTag tag)
            {
                super.deserializeNBT(tag);
                if(tag.contains("Size", Tag.TAG_ANY_NUMERIC))
                {
                    this.size = tag.getDouble("Size");
                }
            }

            @Override
            public JsonObject toJsonObject()
            {
                Preconditions.checkArgument(this.size >= 0, "Muzzle flash size must be more than or equal to zero");
                JsonObject object = super.toJsonObject();
                if(this.size != 0.5)
                {
                    object.addProperty("size", this.size);
                }
                return object;
            }

            public Flash copy()
            {
                Flash flash = new Flash();
                flash.size = this.size;
                flash.xOffset = this.xOffset;
                flash.yOffset = this.yOffset;
                flash.zOffset = this.zOffset;
                return flash;
            }

            /**
             * @return The size/scale of the muzzle flash render
             */
            public double getSize()
            {
                return this.size;
            }
        }

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            if(this.flash != null)
            {
                tag.put("Flash", this.flash.serializeNBT());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            if(tag.contains("Flash", Tag.TAG_COMPOUND))
            {
                CompoundTag flashTag = tag.getCompound("Flash");
                if(!flashTag.isEmpty())
                {
                    Flash flash = new Flash();
                    flash.deserializeNBT(tag.getCompound("Flash"));
                    this.flash = flash;
                }
                else
                {
                    this.flash = null;
                }
            }
        }

        public JsonObject toJsonObject()
        {
            JsonObject object = new JsonObject();
            if(this.flash != null)
            {
                GunJsonUtil.addObjectIfNotEmpty(object, "flash", this.flash.toJsonObject());
            }
            return object;
        }

        public Display copy()
        {
            Display display = new Display();
            if(this.flash != null)
            {
                display.flash = this.flash.copy();
            }
            return display;
        }
    }

    public static class Modules implements INBTSerializable<CompoundTag>, IEditorMenu
    {
        private transient Zoom cachedZoom;

        @Optional
        @Nullable
        private Zoom zoom;
        private Attachments attachments = new Attachments();

        @Nullable
        public Zoom getZoom()
        {
            return this.zoom;
        }

        public Attachments getAttachments()
        {
            return this.attachments;
        }

        @Override
        public Component getEditorLabel()
        {
            return Component.translatable("Modules");
        }

        @Override
        public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets)
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                widgets.add(Pair.of(Component.translatable("Enabled Iron Sights"), () -> new DebugToggle(this.zoom != null, val -> {
                    if(val) {
                        if(this.cachedZoom != null) {
                            this.zoom = this.cachedZoom;
                        } else {
                            this.zoom = new Zoom();
                            this.cachedZoom = this.zoom;
                        }
                    } else {
                        this.cachedZoom = this.zoom;
                        this.zoom = null;
                    }
                })));

                widgets.add(Pair.of(Component.translatable("Adjust Iron Sights"), () -> new DebugButton(Component.translatable(">"), btn -> {
                    if(btn.active && this.zoom != null) {
                        Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(this.zoom));
                    }
                }, () -> this.zoom != null)));
            });
        }

        public static class Zoom extends Positioned implements IEditorMenu
        {
            @Optional
            private float fovModifier;

            @Override
            public CompoundTag serializeNBT()
            {
                CompoundTag tag = super.serializeNBT();
                tag.putFloat("FovModifier", this.fovModifier);
                return tag;
            }

            @Override
            public void deserializeNBT(CompoundTag tag)
            {
                super.deserializeNBT(tag);
                if(tag.contains("FovModifier", Tag.TAG_ANY_NUMERIC))
                {
                    this.fovModifier = tag.getFloat("FovModifier");
                }
            }

            public JsonObject toJsonObject()
            {
                JsonObject object = super.toJsonObject();
                object.addProperty("fovModifier", this.fovModifier);
                return object;
            }

            public Zoom copy()
            {
                Zoom zoom = new Zoom();
                zoom.fovModifier = this.fovModifier;
                zoom.xOffset = this.xOffset;
                zoom.yOffset = this.yOffset;
                zoom.zOffset = this.zOffset;
                return zoom;
            }

            @Override
            public Component getEditorLabel()
            {
                return Component.translatable("Zoom");
            }

            @Override
            public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets)
            {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    widgets.add(Pair.of(Component.translatable("FOV Modifier"), () -> new DebugSlider(0.0, 1.0, this.fovModifier, 0.01, 3, val -> {
                        this.fovModifier = val.floatValue();
                    })));
                });
            }

            public float getFovModifier()
            {
                return this.fovModifier;
            }

            public static Builder builder()
            {
                return new Builder();
            }

            public static class Builder extends AbstractBuilder<Builder> {}

            protected static abstract class AbstractBuilder<T extends AbstractBuilder<T>> extends Positioned.AbstractBuilder<T>
            {
                protected final Zoom zoom;

                protected AbstractBuilder()
                {
                    this(new Zoom());
                }

                protected AbstractBuilder(Zoom zoom)
                {
                    super(zoom);
                    this.zoom = zoom;
                }

                public T setFovModifier(float fovModifier)
                {
                    this.zoom.fovModifier = fovModifier;
                    return this.self();
                }

                @Override
                public Zoom build()
                {
                    return this.zoom.copy();
                }
            }
        }

        public static class Attachments implements INBTSerializable<CompoundTag>
        {
            @Optional
            @Nullable
            private ScaledPositioned scope;
            @Optional
            @Nullable
            private ScaledPositioned barrel;
            @Optional
            @Nullable
            private ScaledPositioned stock;
            @Optional
            @Nullable
            private ScaledPositioned underBarrel;
            @Optional
            @Nullable
            private ScaledPositioned magazine;
            @Optional
            @Nullable
            private ScaledPositioned special;

            @Nullable
            public ScaledPositioned getScope()
            {
                return this.scope;
            }

            @Nullable
            public ScaledPositioned getBarrel()
            {
                return this.barrel;
            }

            @Nullable
            public ScaledPositioned getStock()
            {
                return this.stock;
            }

            @Nullable
            public ScaledPositioned getUnderBarrel()
            {
                return this.underBarrel;
            }

            @Nullable
            public ScaledPositioned getMagazine()
            {
                return this.magazine;
            }

            @Nullable
            public ScaledPositioned getSpecial()
            {
                return this.special;
            }

            @Override
            public CompoundTag serializeNBT()
            {
                CompoundTag tag = new CompoundTag();
                if(this.scope != null)
                {
                    tag.put("Scope", this.scope.serializeNBT());
                }
                if(this.barrel != null)
                {
                    tag.put("Barrel", this.barrel.serializeNBT());
                }
                if(this.stock != null)
                {
                    tag.put("Stock", this.stock.serializeNBT());
                }
                if(this.underBarrel != null)
                {
                    tag.put("UnderBarrel", this.underBarrel.serializeNBT());
                }
                if(this.magazine != null)
                {
                    tag.put("Magazine", this.magazine.serializeNBT());
                }
                if(this.special != null)
                {
                    tag.put("Special", this.special.serializeNBT());
                }
                return tag;
            }

            @Override
            public void deserializeNBT(CompoundTag tag)
            {
                if(tag.contains("Scope", Tag.TAG_COMPOUND))
                {
                    this.scope = this.createScaledPositioned(tag, "Scope");
                }
                if(tag.contains("Barrel", Tag.TAG_COMPOUND))
                {
                    this.barrel = this.createScaledPositioned(tag, "Barrel");
                }
                if(tag.contains("Stock", Tag.TAG_COMPOUND))
                {
                    this.stock = this.createScaledPositioned(tag, "Stock");
                }
                if(tag.contains("UnderBarrel", Tag.TAG_COMPOUND))
                {
                    this.underBarrel = this.createScaledPositioned(tag, "UnderBarrel");
                }
                if(tag.contains("Magazine", Tag.TAG_COMPOUND))
                {
                    this.magazine = this.createScaledPositioned(tag, "Magazine");
                }
                if(tag.contains("Special", Tag.TAG_COMPOUND))
                {
                    this.special = this.createScaledPositioned(tag, "Special");
                }
            }

            public JsonObject toJsonObject()
            {
                JsonObject object = new JsonObject();
                if(this.scope != null)
                {
                    object.add("scope", this.scope.toJsonObject());
                }
                if(this.barrel != null)
                {
                    object.add("barrel", this.barrel.toJsonObject());
                }
                if(this.stock != null)
                {
                    object.add("stock", this.stock.toJsonObject());
                }
                if(this.underBarrel != null)
                {
                    object.add("underBarrel", this.underBarrel.toJsonObject());
                }
                if(this.magazine != null)
                {
                    object.add("magazine", this.magazine.toJsonObject());
                }
                if(this.special != null)
                {
                    object.add("special", this.special.toJsonObject());
                }
                return object;
            }

            public Attachments copy()
            {
                Attachments attachments = new Attachments();
                if(this.scope != null)
                {
                    attachments.scope = this.scope.copy();
                }
                if(this.barrel != null)
                {
                    attachments.barrel = this.barrel.copy();
                }
                if(this.stock != null)
                {
                    attachments.stock = this.stock.copy();
                }
                if(this.underBarrel != null)
                {
                    attachments.underBarrel = this.underBarrel.copy();
                }
                if(this.magazine != null)
                {
                    attachments.magazine = this.magazine.copy();
                }
                if(this.special != null)
                {
                    attachments.special = this.special.copy();
                }
                return attachments;
            }

            @Nullable
            private ScaledPositioned createScaledPositioned(CompoundTag tag, String key)
            {
                CompoundTag attachment = tag.getCompound(key);
                return attachment.isEmpty() ? null : new ScaledPositioned(attachment);
            }
        }

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            if(this.zoom != null)
            {
                tag.put("Zoom", this.zoom.serializeNBT());
            }
            tag.put("Attachments", this.attachments.serializeNBT());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            if(tag.contains("Zoom", Tag.TAG_COMPOUND))
            {
                Zoom zoom = new Zoom();
                zoom.deserializeNBT(tag.getCompound("Zoom"));
                this.zoom = zoom;
            }
            if(tag.contains("Attachments", Tag.TAG_COMPOUND))
            {
                this.attachments.deserializeNBT(tag.getCompound("Attachments"));
            }
        }

        public JsonObject toJsonObject()
        {
            JsonObject object = new JsonObject();
            if(this.zoom != null)
            {
                object.add("zoom", this.zoom.toJsonObject());
            }
            GunJsonUtil.addObjectIfNotEmpty(object, "attachments", this.attachments.toJsonObject());
            return object;
        }

        public Modules copy()
        {
            Modules modules = new Modules();
            if(this.zoom != null)
            {
                modules.zoom = this.zoom.copy();
            }
            modules.attachments = this.attachments.copy();
            return modules;
        }
    }

    public static class Positioned implements INBTSerializable<CompoundTag>
    {
        @Optional
        protected double xOffset;
        @Optional
        protected double yOffset;
        @Optional
        protected double zOffset;

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("XOffset", this.xOffset);
            tag.putDouble("YOffset", this.yOffset);
            tag.putDouble("ZOffset", this.zOffset);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            if(tag.contains("XOffset", Tag.TAG_ANY_NUMERIC))
            {
                this.xOffset = tag.getDouble("XOffset");
            }
            if(tag.contains("YOffset", Tag.TAG_ANY_NUMERIC))
            {
                this.yOffset = tag.getDouble("YOffset");
            }
            if(tag.contains("ZOffset", Tag.TAG_ANY_NUMERIC))
            {
                this.zOffset = tag.getDouble("ZOffset");
            }
        }

        public JsonObject toJsonObject()
        {
            JsonObject object = new JsonObject();
            if(this.xOffset != 0)
            {
                object.addProperty("xOffset", this.xOffset);
            }
            if(this.yOffset != 0)
            {
                object.addProperty("yOffset", this.yOffset);
            }
            if(this.zOffset != 0)
            {
                object.addProperty("zOffset", this.zOffset);
            }
            return object;
        }

        public double getXOffset()
        {
            return this.xOffset;
        }

        public double getYOffset()
        {
            return this.yOffset;
        }

        public double getZOffset()
        {
            return this.zOffset;
        }

        public Positioned copy()
        {
            Positioned positioned = new Positioned();
            positioned.xOffset = this.xOffset;
            positioned.yOffset = this.yOffset;
            positioned.zOffset = this.zOffset;
            return positioned;
        }

        public static class Builder extends AbstractBuilder<Builder> {}

        protected static abstract class AbstractBuilder<T extends AbstractBuilder<T>> extends SuperBuilder<Positioned, T>
        {
            private final Positioned positioned;

            private AbstractBuilder()
            {
                this(new Positioned());
            }

            protected AbstractBuilder(Positioned positioned)
            {
                this.positioned = positioned;
            }

            public T setOffset(double xOffset, double yOffset, double zOffset)
            {
                this.positioned.xOffset = xOffset;
                this.positioned.yOffset = yOffset;
                this.positioned.zOffset = zOffset;
                return this.self();
            }

            public T setXOffset(double xOffset)
            {
                this.positioned.xOffset = xOffset;
                return this.self();
            }

            public T setYOffset(double yOffset)
            {
                this.positioned.yOffset = yOffset;
                return this.self();
            }

            public T setZOffset(double zOffset)
            {
                this.positioned.zOffset = zOffset;
                return this.self();
            }

            @Override
            public Positioned build()
            {
                return this.positioned.copy();
            }
        }
    }

    public static class ScaledPositioned extends Positioned
    {
        @Optional
        protected double scale = 1.0;

        public ScaledPositioned() {}

        public ScaledPositioned(CompoundTag tag)
        {
            this.deserializeNBT(tag);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = super.serializeNBT();
            tag.putDouble("Scale", this.scale);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            super.deserializeNBT(tag);
            if(tag.contains("Scale", Tag.TAG_ANY_NUMERIC))
            {
                this.scale = tag.getDouble("Scale");
            }
        }

        @Override
        public JsonObject toJsonObject()
        {
            JsonObject object = super.toJsonObject();
            if(this.scale != 1.0)
            {
                object.addProperty("scale", this.scale);
            }
            return object;
        }

        public double getScale()
        {
            return this.scale;
        }

        @Override
        public ScaledPositioned copy()
        {
            ScaledPositioned positioned = new ScaledPositioned();
            positioned.xOffset = this.xOffset;
            positioned.yOffset = this.yOffset;
            positioned.zOffset = this.zOffset;
            positioned.scale = this.scale;
            return positioned;
        }
    }

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.put("General", this.general.serializeNBT());
        tag.put("Reloads", this.reloads.serializeNBT());
        tag.put("Projectile", this.projectile.serializeNBT());
        tag.put("PotionEffect", this.potionEffect.serializeNBT());
        tag.put("Sounds", this.sounds.serializeNBT());
        tag.put("Display", this.display.serializeNBT());
        tag.put("Modules", this.modules.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if(tag.contains("General", Tag.TAG_COMPOUND))
        {
            this.general.deserializeNBT(tag.getCompound("General"));
        }
        if(tag.contains("Reloads", Tag.TAG_COMPOUND))
        {
            this.reloads.deserializeNBT(tag.getCompound("Reloads"));
        }
        if(tag.contains("Projectile", Tag.TAG_COMPOUND))
        {
            this.projectile.deserializeNBT(tag.getCompound("Projectile"));
        }
        if(tag.contains("PotionEffect", Tag.TAG_COMPOUND))
        {
            this.potionEffect.deserializeNBT(tag.getCompound("PotionEffect"));
        }
        if(tag.contains("Sounds", Tag.TAG_COMPOUND))
        {
            this.sounds.deserializeNBT(tag.getCompound("Sounds"));
        }
        if(tag.contains("Display", Tag.TAG_COMPOUND))
        {
            this.display.deserializeNBT(tag.getCompound("Display"));
        }
        if(tag.contains("Modules", Tag.TAG_COMPOUND))
        {
            this.modules.deserializeNBT(tag.getCompound("Modules"));
        }
    }

    public JsonObject toJsonObject()
    {
        JsonObject object = new JsonObject();
        object.add("general", this.general.toJsonObject());
        object.add("reloads", this.reloads.toJsonObject());
        object.add("projectile", this.projectile.toJsonObject());
        GunJsonUtil.addObjectIfNotEmpty(object, "potionEffect", this.potionEffect.toJsonObject());
        GunJsonUtil.addObjectIfNotEmpty(object, "sounds", this.sounds.toJsonObject());
        GunJsonUtil.addObjectIfNotEmpty(object, "display", this.display.toJsonObject());
        GunJsonUtil.addObjectIfNotEmpty(object, "modules", this.modules.toJsonObject());
        return object;
    }

    public static Gun create(CompoundTag tag)
    {
        Gun gun = new Gun();
        gun.deserializeNBT(tag);
        return gun;
    }

    public Gun copy()
    {
        Gun gun = new Gun();
        gun.general = this.general.copy();
        gun.reloads = this.reloads.copy();
        gun.projectile = this.projectile.copy();
        gun.potionEffect = this.potionEffect.copy();
        gun.sounds = this.sounds.copy();
        gun.display = this.display.copy();
        gun.modules = this.modules.copy();
        return gun;
    }

    public boolean canAttachType(@Nullable IAttachment.Type type)
    {
        if(this.modules.attachments != null && type != null)
        {
            switch(type)
            {
                case SCOPE:
                    return this.modules.attachments.scope != null;
                case BARREL:
                    return this.modules.attachments.barrel != null;
                case STOCK:
                    return this.modules.attachments.stock != null;
                case UNDER_BARREL:
                    return this.modules.attachments.underBarrel != null;
                case MAGAZINE:
                    return this.modules.attachments.magazine != null;
                case SPECIAL:
                    return this.modules.attachments.special != null;
            }
        }
        return false;
    }

    @Nullable
    public ScaledPositioned getAttachmentPosition(IAttachment.Type type)
    {
        if(this.modules.attachments != null)
        {
            switch(type)
            {
                case SCOPE:
                    return this.modules.attachments.scope;
                case BARREL:
                    return this.modules.attachments.barrel;
                case STOCK:
                    return this.modules.attachments.stock;
                case UNDER_BARREL:
                    return this.modules.attachments.underBarrel;
                case MAGAZINE:
                    return this.modules.attachments.magazine;
                case SPECIAL:
                    return this.modules.attachments.special;
            }
        }
        return null;
    }

    public boolean canAimDownSight()
    {
        return this.canAttachType(IAttachment.Type.SCOPE) || this.modules.zoom != null;
    }

    public static ItemStack getScopeStack(ItemStack gun)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains("Scope", Tag.TAG_COMPOUND))
            {
                return ItemStack.of(attachment.getCompound("Scope"));
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasAttachmentEquipped(ItemStack stack, Gun gun, IAttachment.Type type)
    {
        if(!gun.canAttachType(type))
            return false;

        CompoundTag compound = stack.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            return attachment.contains(type.getTagKey(), Tag.TAG_COMPOUND);
        }
        return false;
    }

    //This one is used for SpecialModels' attachment rendering!
    public static boolean hasAttachmentEquipped(ItemStack stack, IAttachment.Type type)
    {
        CompoundTag compound = stack.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            return attachment.contains(type.getTagKey(), Tag.TAG_COMPOUND);
        }
        return false;
    }

    public static boolean hasCosmeticEquipped(ItemStack stack, IAttachment.Type type)
    {
        CompoundTag compound = stack.getTag();
        if(compound != null && compound.contains("Cosmetics", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Cosmetics");
            return attachment.contains(type.getTagKey(), Tag.TAG_COMPOUND);
        }
        return false;
    }

    @Nullable
    public static Scope getScope(ItemStack gun)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains("Scope", Tag.TAG_COMPOUND))
            {
                ItemStack scopeStack = ItemStack.of(attachment.getCompound("Scope"));
                Scope scope = null;
                if(scopeStack.getItem() instanceof ScopeItem scopeItem)
                {
                    if(JustEnoughGuns.isDebugging())
                    {
                        return Debug.getScope(scopeItem);
                    }
                    scope = scopeItem.getProperties();
                } else if (scopeStack.getItem() instanceof SpyglassItem) {
                    scope = Scope.builder()
                            .aimFovModifier(0.2F)
                            .modifiers(GunModifiers.SLOWEST_ADS)
                            .build();
                }
                return scope;
            }
        }
        return null;
    }

    public static ItemStack getAttachment(IAttachment.Type type, ItemStack gun)
    {
        if (gun == null) {
            return ItemStack.EMPTY;
        }

        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains(type.getTagKey(), Tag.TAG_COMPOUND))
            {
                return ItemStack.of(attachment.getCompound(type.getTagKey()));
            }
        }
        if(compound != null && compound.contains("Cosmetics", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Cosmetics");
            if(attachment.contains(type.getTagKey(), Tag.TAG_COMPOUND))
            {
                return ItemStack.of(attachment.getCompound(type.getTagKey()));
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack removeAttachment(ItemStack gun, String attachmentStack)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains(attachmentStack, Tag.TAG_COMPOUND))
            {
                attachment.remove(attachmentStack);
            }
        }
        if(compound != null && compound.contains("Cosmetics", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Cosmetics");
            if(attachment.contains(attachmentStack, Tag.TAG_COMPOUND))
            {
                attachment.remove(attachmentStack);
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack removeScopeStack(ItemStack gun)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains("Scope", Tag.TAG_COMPOUND))
            {
                attachment.remove("Scope");
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack removeBarrelStack(ItemStack gun)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains("Barrel", Tag.TAG_COMPOUND))
            {
                attachment.remove("Barrel");
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack removeStockStack(ItemStack gun)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains("Stock", Tag.TAG_COMPOUND))
            {
                attachment.remove("Stock");
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack removeUnderBarrelStack(ItemStack gun)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains("Under_Barrel", Tag.TAG_COMPOUND))
            {
                attachment.remove("Under_Barrel");
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack removeMagazineStack(ItemStack gun)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains("Magazine", Tag.TAG_COMPOUND))
            {
                attachment.remove("Magazine");
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack removeSpecialStack(ItemStack gun)
    {
        CompoundTag compound = gun.getTag();
        if(compound != null && compound.contains("Attachments", Tag.TAG_COMPOUND))
        {
            CompoundTag attachment = compound.getCompound("Attachments");
            if(attachment.contains("Special", Tag.TAG_COMPOUND))
            {
                attachment.remove("Special");
            }
        }
        return ItemStack.EMPTY;
    }

    public static float getAdditionalDamage(ItemStack gunStack)
    {
        CompoundTag tag = gunStack.getOrCreateTag();
        return tag.getFloat("AdditionalDamage");
    }

    public static AmmoContext findAmmo(Player player, ResourceLocation id)
    {
        if(player.isCreative() || player.getMainHandItem().getEnchantmentLevel(ModEnchantments.INFINITY.get()) != 0)
        {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            ItemStack ammo = item != null ? new ItemStack(item, Integer.MAX_VALUE) : ItemStack.EMPTY;
            return new AmmoContext(ammo, null);
        }
        for(int i = 0; i < player.getInventory().getContainerSize(); ++i)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if(isAmmo(stack, id))
            {
                return new AmmoContext(stack, player.getInventory());
            }
        }
        return AmmoContext.NONE;
    }

    public static ItemStack[] findAmmoStack(Player player, ResourceLocation id) // Refactor to return multiple stacks, reload to take as much of value as required from hash
    {
        if(!player.isAlive())
            return new ItemStack[]{};
        ArrayList<ItemStack> stacks = new ArrayList<>();
        if(player.isCreative() || player.getMainHandItem().getEnchantmentLevel(ModEnchantments.INFINITY.get()) != 0)
        {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            stacks.add(item != null ? new ItemStack(item, Integer.MAX_VALUE) : ItemStack.EMPTY);
            return stacks.toArray(new ItemStack[]{});
        }
        for(int i = 0; i < player.getInventory().getContainerSize(); ++i)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if(isAmmo(stack, id)) {
                stacks.add(stack);
            }
        }
        return stacks.toArray(new ItemStack[]{});
    }

    public static int getTotalAmmoCount(ItemStack[] itemStacks) {
        int total = 0;
        for (ItemStack stack : itemStacks) {
            if (!stack.isEmpty()) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean isAmmo(ItemStack stack, ResourceLocation id)
    {
        return stack != null && Objects.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()), id);
    }

    public static boolean hasAmmo(ItemStack gunStack)
    {
        CompoundTag tag = gunStack.getOrCreateTag();
        return tag.getBoolean("IgnoreAmmo") || tag.getInt("AmmoCount") > 0;
    }

    public static float getFovModifier(ItemStack stack, Gun modifiedGun)
    {
        float modifier = 0.0F;
        if(hasAttachmentEquipped(stack, modifiedGun, IAttachment.Type.SCOPE))
        {
            Scope scope = Gun.getScope(stack);
            if(scope != null)
            {
                if(scope.getFovModifier() < 1.0F)
                {
                    return Mth.clamp(scope.getFovModifier(), 0.01F, 1.0F);
                }
            }
        }
        Modules.Zoom zoom = modifiedGun.getModules().getZoom();
        return zoom != null ? modifier + zoom.getFovModifier() : 0F;
    }

    public static class Builder
    {
        private final Gun gun;

        private Builder()
        {
            this.gun = new Gun();
        }

        public static Builder create()
        {
            return new Builder();
        }

        public Gun build()
        {
            return this.gun.copy(); //Copy since the builder could be used again
        }

        public Builder setFireMode(FireMode fireMode)
        {
            this.gun.general.fireMode = fireMode;
            return this;
        }

        public Builder setBurstAmount(int burstAmount)
        {
            this.gun.general.burstAmount = burstAmount;
            return this;
        }

        public Builder setBurstDelay(int burstDelay)
        {
            this.gun.general.burstDelay = burstDelay;
            return this;
        }

        public Builder setFireRate(int rate)
        {
            this.gun.general.rate = rate;
            return this;
        }

        public Builder setFireTimer(int fireTimer)
        {
            this.gun.general.fireTimer = fireTimer;
            return this;
        }

        public Builder setMaxHoldFire(int maxHoldFire)
        {
            this.gun.general.maxHoldFire = maxHoldFire;
            return this;
        }

        public Builder setOverheatTimer(int overheatTimer)
        {
            this.gun.general.overheatTimer = overheatTimer;
            return this;
        }

        public Builder setDrawTimer(int drawTimer)
        {
            this.gun.general.drawTimer = drawTimer;
            return this;
        }

        public Builder setSilenced(boolean silenced)
        {
            this.gun.general.silenced = silenced;
            return this;
        }

        public Builder setCustomFiring(boolean customFiring)
        {
            this.gun.general.customFiring = customFiring;
            return this;
        }

        public Builder setGripType(GripType gripType)
        {
            this.gun.general.gripType = gripType;
            return this;
        }

        public Builder setReloadItem(Item item)
        {
            this.gun.reloads.reloadItem = ForgeRegistries.ITEMS.getKey(item);
            return this;
        }

        public Builder setMaxAmmo(int maxAmmo)
        {
            this.gun.reloads.maxAmmo = maxAmmo;
            return this;
        }

        public Builder setReloadType(ReloadType reloadType)
        {
            this.gun.reloads.reloadType = reloadType;
            return this;
        }

        public Builder setReloadTimer(int reloadTimer)
        {
            this.gun.reloads.reloadTimer = reloadTimer;
            return this;
        }

        public Builder setAdditionalReloadTimer(int additionalReloadTimer)
        {
            this.gun.reloads.additionalReloadTimer = additionalReloadTimer;
            return this;
        }

        public Builder setReloadAmount(int reloadAmount)
        {
            this.gun.reloads.reloadAmount = reloadAmount;
            return this;
        }

        public Builder setRecoilAngle(float recoilAngle)
        {
            this.gun.general.recoilAngle = recoilAngle;
            return this;
        }

        public Builder setRecoilKick(float recoilKick)
        {
            this.gun.general.recoilKick = recoilKick;
            return this;
        }

        public Builder setRecoilDurationOffset(float recoilDurationOffset)
        {
            this.gun.general.recoilDurationOffset = recoilDurationOffset;
            return this;
        }

        public Builder setRecoilAdsReduction(float recoilAdsReduction)
        {
            this.gun.general.recoilAdsReduction = recoilAdsReduction;
            return this;
        }

        public Builder setProjectileAmount(int projectileAmount)
        {
            this.gun.general.projectileAmount = projectileAmount;
            return this;
        }

        public Builder setAlwaysSpread(boolean alwaysSpread)
        {
            this.gun.general.alwaysSpread = alwaysSpread;
            return this;
        }

        public Builder setShooterPushback(float shooterPushback)
        {
            this.gun.general.shooterPushback = shooterPushback;
            return this;
        }

        public Builder setSpread(float spread)
        {
            this.gun.general.spread = spread;
            return this;
        }

        public Builder setCanBeBlueprinted(boolean canBeBlueprinted)
        {
            this.gun.general.canBeBlueprinted = canBeBlueprinted;
            return this;
        }

        public Builder setInfinityDisabled(boolean infinityDisabled)
        {
            this.gun.general.infinityDisabled = infinityDisabled;
            return this;
        }

        public Builder setWitheredDisabled(boolean witheredDisabled)
        {
            this.gun.general.witheredDisabled = witheredDisabled;
            return this;
        }

        public Builder setCanFireUnderwater(boolean canFireUnderwater)
        {
            this.gun.general.canFireUnderwater = canFireUnderwater;
            return this;
        }

        public Builder setAmmo(Item item)
        {
            this.gun.projectile.item = ForgeRegistries.ITEMS.getKey(item);
            return this;
        }

        public Builder setEjectsCasing(boolean ejectsCasing)
        {
            this.gun.projectile.ejectsCasing = ejectsCasing;
            return this;
        }

        public Builder setProjectileVisible(boolean visible)
        {
            this.gun.projectile.visible = visible;
            return this;
        }

        public Builder setProjectileSize(float size)
        {
            this.gun.projectile.size = size;
            return this;
        }

        public Builder setProjectileSpeed(double speed)
        {
            this.gun.projectile.speed = speed;
            return this;
        }

        public Builder setProjectileLife(int life)
        {
            this.gun.projectile.life = life;
            return this;
        }

        public Builder setProjectileAffectedByGravity(boolean gravity)
        {
            this.gun.projectile.gravity = gravity;
            return this;
        }

        public Builder setProjectileTrailColor(int trailColor)
        {
            this.gun.projectile.trailColor = trailColor;
            return this;
        }

        public Builder setProjectileTrailLengthMultiplier(int trailLengthMultiplier)
        {
            this.gun.projectile.trailLengthMultiplier = trailLengthMultiplier;
            return this;
        }

        public Builder setHideTrail(boolean hideTrail)
        {
            this.gun.projectile.hideTrail = hideTrail;
            return this;
        }

        public Builder setNoProjectile(boolean noProjectile)
        {
            this.gun.projectile.noProjectile = noProjectile;
            return this;
        }

        public Builder setHitsRubberFruit(boolean hitsRubberFruit)
        {
            this.gun.projectile.hitsRubberFruit = hitsRubberFruit;
            return this;
        }

        public Builder setIgnoresBlocks(boolean ignoresBlocks)
        {
            this.gun.projectile.ignoresBlocks = ignoresBlocks;
            return this;
        }

        public Builder setCollateral(boolean collateral)
        {
            this.gun.projectile.collateral = collateral;
            return this;
        }

        public Builder setDamage(float damage)
        {
            this.gun.projectile.damage = damage;
            return this;
        }

        public Builder setHeadshotMultiplier(float headshotMultiplier)
        {
            this.gun.projectile.headshotMultiplier = headshotMultiplier;
            return this;
        }

        public Builder setAdvantage(ResourceLocation advantage)
        {
            this.gun.projectile.advantage = advantage;
            return this;
        }

        public Builder setReduceDamageOverLife(boolean damageReduceOverLife)
        {
            this.gun.projectile.damageReduceOverLife = damageReduceOverLife;
            return this;
        }

        public Builder setSelfPotionEffect(boolean selfPotionEffect)
        {
            this.gun.potionEffect.selfPotionEffect = selfPotionEffect;
            return this;
        }

        public Builder setPotionEffect(ResourceLocation potionEffect)
        {
            this.gun.potionEffect.potionEffect = potionEffect;
            return this;
        }

        public Builder setPotionEffectStrength(int strength)
        {
            this.gun.potionEffect.potionEffectStrength = strength;
            return this;
        }

        public Builder setPotionEffectDuration(int duration)
        {
            this.gun.potionEffect.potionEffectDuration = duration;
            return this;
        }

        public Builder setFireSound(SoundEvent sound)
        {
            this.gun.sounds.fire = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        public Builder setReloadStart(SoundEvent sound)
        {
            this.gun.sounds.reloadStart = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        public Builder setReloadLoadSound(SoundEvent sound)
        {
            this.gun.sounds.reloadLoad = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        public Builder setReloadEndSound(SoundEvent sound)
        {
            this.gun.sounds.reloadEnd = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        public Builder setEjectorPullSound(SoundEvent sound)
        {
            this.gun.sounds.ejectorPull = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        public Builder setEjectorReleaseSound(SoundEvent sound)
        {
            this.gun.sounds.ejectorRelease = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        public Builder setSilencedFireSound(SoundEvent sound)
        {
            this.gun.sounds.silencedFire = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        public Builder setEnchantedFireSound(SoundEvent sound)
        {
            this.gun.sounds.enchantedFire = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        public Builder setPreFireSound(SoundEvent sound)
        {
            this.gun.sounds.preFire = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            return this;
        }

        //@Deprecated(since = "1.3.0", forRemoval = true)
        public Builder setMuzzleFlash(double size, double xOffset, double yOffset, double zOffset)
        {
            Display.Flash flash = new Display.Flash();
            flash.size = size;
            flash.xOffset = xOffset;
            flash.yOffset = yOffset;
            flash.zOffset = zOffset;
            this.gun.display.flash = flash;
            return this;
        }

        public Builder setZoom(float fovModifier, double xOffset, double yOffset, double zOffset)
        {
            Modules.Zoom zoom = new Modules.Zoom();
            zoom.fovModifier = fovModifier;
            zoom.xOffset = xOffset;
            zoom.yOffset = yOffset;
            zoom.zOffset = zOffset;
            this.gun.modules.zoom = zoom;
            return this;
        }

        //@Deprecated(since = "1.3.0", forRemoval = true)
        public Builder setZoom(Modules.Zoom.Builder builder)
        {
            this.gun.modules.zoom = builder.build();
            return this;
        }

        //@Deprecated(since = "1.3.0", forRemoval = true)
        public Builder setScope(float scale, double xOffset, double yOffset, double zOffset)
        {
            ScaledPositioned positioned = new ScaledPositioned();
            positioned.scale = scale;
            positioned.xOffset = xOffset;
            positioned.yOffset = yOffset;
            positioned.zOffset = zOffset;
            this.gun.modules.attachments.scope = positioned;
            return this;
        }

        //@Deprecated(since = "1.3.0", forRemoval = true)
        public Builder setBarrel(float scale, double xOffset, double yOffset, double zOffset)
        {
            ScaledPositioned positioned = new ScaledPositioned();
            positioned.scale = scale;
            positioned.xOffset = xOffset;
            positioned.yOffset = yOffset;
            positioned.zOffset = zOffset;
            this.gun.modules.attachments.barrel = positioned;
            return this;
        }

        //@Deprecated(since = "1.3.0", forRemoval = true)
        public Builder setStock(float scale, double xOffset, double yOffset, double zOffset)
        {
            ScaledPositioned positioned = new ScaledPositioned();
            positioned.scale = scale;
            positioned.xOffset = xOffset;
            positioned.yOffset = yOffset;
            positioned.zOffset = zOffset;
            this.gun.modules.attachments.stock = positioned;
            return this;
        }

        //@Deprecated(since = "1.3.0", forRemoval = true)
        public Builder setUnderBarrel(float scale, double xOffset, double yOffset, double zOffset)
        {
            ScaledPositioned positioned = new ScaledPositioned();
            positioned.scale = scale;
            positioned.xOffset = xOffset;
            positioned.yOffset = yOffset;
            positioned.zOffset = zOffset;
            this.gun.modules.attachments.underBarrel = positioned;
            return this;
        }

        public Builder setMagazine(float scale, double xOffset, double yOffset, double zOffset)
        {
            ScaledPositioned positioned = new ScaledPositioned();
            positioned.scale = scale;
            positioned.xOffset = xOffset;
            positioned.yOffset = yOffset;
            positioned.zOffset = zOffset;
            this.gun.modules.attachments.magazine = positioned;
            return this;
        }

        public Builder setSpecial(float scale, double xOffset, double yOffset, double zOffset)
        {
            ScaledPositioned positioned = new ScaledPositioned();
            positioned.scale = scale;
            positioned.xOffset = xOffset;
            positioned.yOffset = yOffset;
            positioned.zOffset = zOffset;
            this.gun.modules.attachments.special = positioned;
            return this;
        }
    }
}
