package net.narutomod.entity;

import net.minecraft.world.World;
import net.minecraft.util.CombatRules;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumHand;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.item.ItemStack;
import net.minecraft.init.SoundEvents;

import net.narutomod.item.ItemTotsukaSword;
import net.narutomod.item.ItemChokuto;
import net.narutomod.item.ItemShuriken;
import net.narutomod.item.ItemSharingan;
import net.narutomod.item.ItemJutsu;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.procedure.ProcedureSusanoo;
import net.narutomod.PlayerTracker;
import net.narutomod.Particles;
import net.narutomod.Chakra;
import net.narutomod.ElementsNarutomodMod;

import javax.annotation.Nullable;

@ElementsNarutomodMod.ModElement.Tag
public abstract class EntitySusanooBase extends EntityCreature implements IRangedAttackMob {
	private static final DataParameter<Integer> OWNER_ID = EntityDataManager.<Integer>createKey(EntitySusanooBase.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> FLAME_COLOR = EntityDataManager.<Integer>createKey(EntitySusanooBase.class, DataSerializers.VARINT);
	public static final double BXP_REQUIRED_L0 = 20000.0d;
	public static final double BXP_REQUIRED_L1 = 30000.0d;
	public static final double BXP_REQUIRED_L2 = 40000.0d;
	public static final double BXP_REQUIRED_L3 = 50000.0d;
	public static final double BXP_REQUIRED_L4 = 100000.0d;
	protected double chakraUsage = 60d; // per second
	protected double chakraUsageModifier = 4d;
	protected double playerXp;
	//private EntityLivingBase ownerPlayer = null;
	
	public EntitySusanooBase(World world) {
		super(world);
		this.experienceValue = 5;
		this.isImmuneToFire = true;
		//this.noClip = true;
		this.stepHeight = 0.5F;
		this.setNoAI(true);
		this.enablePersistence();
		this.tasks.addTask(1, new EntityAILookIdle(this));
	}

	public EntitySusanooBase(EntityLivingBase player) {
		this(player.world);
		this.setLocationAndAngles(player.posX, player.posY, player.posZ, player.rotationYaw, 0.0F);
		this.setOwnerPlayer(player);
		if (player instanceof EntityPlayer) {
			this.playerXp = PlayerTracker.getBattleXp((EntityPlayer)player);
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(MathHelper.sqrt(this.playerXp));
			 //.applyModifier(new AttributeModifier("susanoo.health", 2d * ((EntityPlayer)player).experienceLevel, 0));
			//this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
			 //.applyModifier(new AttributeModifier("susanoo.damage", ((EntityPlayer)player).experienceLevel, 0));
		}
		ItemStack helmetstack = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		if (helmetstack.getItem() instanceof ItemSharingan.Base) {
			if (ProcedureUtils.isOriginalOwner(player, helmetstack)) {
				this.chakraUsageModifier = 1d;
			}
			int color = ((ItemSharingan.Base)helmetstack.getItem()).getColor(helmetstack);
			if (color != 0) {
				this.setFlameColor(color);
			}
		}
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(ProcedureUtils.getModifiedSpeed(player) * 0.3d);
		this.setHealth(this.getMaxHealth());
		this.setAlwaysRenderNameTag(false);
		player.startRiding(this);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(OWNER_ID, Integer.valueOf(-1));
		this.dataManager.register(FLAME_COLOR, Integer.valueOf(0x202C183D));
	}

	@Nullable
	public EntityLivingBase getOwnerPlayer() {
		Entity entity = this.world.getEntityByID(((Integer)this.dataManager.get(OWNER_ID)).intValue());
		return entity instanceof EntityLivingBase ? (EntityLivingBase)entity : null;
	}

	protected void setOwnerPlayer(EntityLivingBase owner) {
		this.dataManager.set(OWNER_ID, Integer.valueOf(owner.getEntityId()));
	}

	protected void setFlameColor(int color) {
		this.dataManager.set(FLAME_COLOR, Integer.valueOf(color));
	}

	public int getFlameColor() {
		return ((Integer)this.dataManager.get(FLAME_COLOR)).intValue();
	}

	public abstract boolean shouldShowSword();

    public abstract void setShowSword(boolean show);

	@Override
	public EnumCreatureAttribute getCreatureAttribute() {
		return EnumCreatureAttribute.UNDEFINED;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	@Override
	protected boolean canDropLoot() {
		return false;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getAttributeMap().registerAttribute(ProcedureUtils.MAXHEALTH);
		this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
		this.getAttributeMap().registerAttribute(EntityPlayer.REACH_DISTANCE);
		this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(100.0D);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.05D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50.0D);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(10.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
		this.getEntityAttribute(EntityPlayer.REACH_DISTANCE).setBaseValue(7.0D);
	}

	@Override
	public IAttributeInstance getEntityAttribute(IAttribute attribute) {
		return super.getEntityAttribute(attribute == SharedMonsterAttributes.MAX_HEALTH ? ProcedureUtils.MAXHEALTH : attribute);
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (source.getImmediateSource() instanceof EntityPlayer && source.getImmediateSource().equals(getControllingPassenger()))
			return false;
		if (source.getImmediateSource() instanceof EntityCreature && source.getImmediateSource().equals(this))
			return false;
		if (source.getImmediateSource() instanceof net.minecraft.entity.projectile.EntityArrow)
			return false;
		if (source.getImmediateSource() instanceof net.minecraft.entity.projectile.EntityPotion)
			return false;
		if (source == DamageSource.FALL)
			return false;
		if (source == DamageSource.CACTUS)
			return false;
		if (source == DamageSource.DROWN)
			return false;
		if (source == DamageSource.MAGIC)
			return false;
		if (source == DamageSource.WITHER)
			return false;
		if (source == ProcedureUtils.AMATERASU)
			return false;
		float f = this.getHealth();
		boolean flag = super.attackEntityFrom(source, amount);
		EntityLivingBase summoner = this.getOwnerPlayer();
		if (flag && summoner != null && !this.isEntityAlive()) {
			summoner.attackEntityFrom(source, CombatRules.getDamageAfterAbsorb(amount, (float)this.getTotalArmorValue(), 0f) - f);
		}
		return flag;
	}

	@Override
	public boolean attackEntityAsMob(Entity entityIn) {
		Entity passenger = this.getControllingPassenger();
		if (passenger instanceof EntityLivingBase) {
			float f = (float)this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
			float f2 = 1.0f;
			if (passenger instanceof EntityPlayer) {
				f2 = ((EntityPlayer)passenger).getCooledAttackStrength(0.5f);
				((EntityPlayer)passenger).resetCooldown();
			}
			f *= f2;
			ItemStack stack = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
			DamageSource ds = ItemJutsu.causeJutsuDamage(this, this.getOwnerPlayer());
			if (stack.getItem() == ItemTotsukaSword.block && entityIn instanceof EntityLivingBase
			 && Chakra.pathway((EntityLivingBase)entityIn).getAmount() < 5d) {
				ds = ds.setDamageIsAbsolute().setDamageBypassesArmor();
				f *= 2.0f + this.rand.nextFloat();
			}
			boolean flag = entityIn.attackEntityFrom(ds, f);
			if (flag && entityIn instanceof EntityLivingBase) {
				if (!stack.isEmpty()) {
					stack.getItem().hitEntity(stack, (EntityLivingBase)entityIn, this);
				}
				if (f2 > 0.8f) {
					((EntityLivingBase)entityIn).knockBack(this, f2 * 2.5F, 
					 (double)MathHelper.sin(this.rotationYaw * 0.017453292F), (double)(-MathHelper.cos(this.rotationYaw * 0.017453292F)));
					this.motionX *= 0.6D;
					this.motionZ *= 0.6D;
				}
			}
			return flag;
		} else {
			return super.attackEntityAsMob(entityIn);
		}
	}

	@Override
	public boolean processInteract(EntityPlayer entity, EnumHand hand) {
		super.processInteract(entity, hand);
		if (!this.world.isRemote && entity.equals(this.getOwnerPlayer())) {
			entity.startRiding(this);
			return true;
		}
		return false;
	}

	@Override
	public void travel(float ti, float tj, float tk) {
		if (this.isBeingRidden() && this.isAIDisabled()) {
			Entity entity = this.getControllingPassenger();
			this.rotationYaw = entity.rotationYaw;
			this.prevRotationYaw = this.rotationYaw;
			this.rotationPitch = entity.rotationPitch;
			this.setRotation(this.rotationYaw, this.rotationPitch);
			this.jumpMovementFactor = this.getAIMoveSpeed();
			this.renderYawOffset = entity.rotationYaw;
			this.rotationYawHead = entity.rotationYaw;
			this.stepHeight = this.height / 3.0F;
			if (entity instanceof EntityLivingBase) {
				this.setAIMoveSpeed((float) this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
				float forward = ((EntityLivingBase) entity).moveForward;
				float strafe = ((EntityLivingBase) entity).moveStrafing;
				super.travel(strafe, 0.0F, forward);
			}
		} else {
			this.jumpMovementFactor = 0.02F;
			super.travel(ti, tj, tk);
		}
	}

	@Override
	public double getMountedYOffset() {
		return 0.35D;
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public Entity getControllingPassenger() {
		return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
	}

	@Override
	public boolean canBeSteered() {
		return true;
	}

	@Override
	public boolean shouldDismountInWater(Entity rider) {
		return false;
	}

	@Override
	public void applyEntityCollision(Entity entityIn) {
		if (!this.isRidingSameEntity(entityIn) && !entityIn.noClip && !entityIn.isBeingRidden()) {
            double d0 = entityIn.posX - this.posX;
            double d1 = entityIn.posZ - this.posZ;
            double d2 = MathHelper.absMax(d0, d1);
            if (d2 >= 0.01D) {
                d2 = (double)MathHelper.sqrt(d2);
                d0 = d0 / d2;
                d1 = d1 / d2;
                double d3 = 1.0D / d2;
                if (d3 > 1.0D) {
                    d3 = 1.0D;
                }
                d0 = d0 * d3;
                d1 = d1 * d3;
                d0 = d0 * 0.05D;
                d1 = d1 * 0.05D;
                d0 = d0 * (double)(1.0F - this.entityCollisionReduction);
                d1 = d1 * (double)(1.0F - this.entityCollisionReduction);
                entityIn.addVelocity(d0, 0.0D, d1);
            }
		}
	}

	private void clampMotion(double d) {
		if (Math.abs(this.motionX) > d)
			this.motionX = (this.motionX > 0.0D) ? d : -d;
		if (Math.abs(this.motionY) > d)
			this.motionY = (this.motionY > 0.0D) ? d : -d;
		if (Math.abs(this.motionZ) > d)
			this.motionZ = (this.motionZ > 0.0D) ? d : -d;
	}

	protected void consumeChakra() {
		if (this.ticksExisted % 20 == 0) {
			if (!Chakra.pathway(this.getOwnerPlayer()).consume(this.chakraUsage * this.chakraUsageModifier)) {
				this.setDead();
			}
		}
	}

	@Override
	public void onLivingUpdate() {
		EntityLivingBase ownerPlayer = this.getOwnerPlayer();
		boolean flag = ownerPlayer instanceof EntityPlayer;
		if (!this.world.isRemote && (ownerPlayer == null || !ownerPlayer.isEntityAlive() || 
		 (ownerPlayer instanceof EntityPlayerMP && ((EntityPlayerMP)ownerPlayer).hasDisconnected()) ||
		 (!flag && !this.isBeingRidden()))) {
			this.setDead();
		}
		if (flag && !((EntityPlayer)ownerPlayer).isCreative()) {
			if (this.isBeingRidden()) {
				ownerPlayer.setSneaking(false);
			} else if (!this.world.isRemote) {
				ProcedureSusanoo.execute((EntityPlayer)ownerPlayer);
			}
			if (!this.world.isRemote) {
				this.consumeChakra();
			}
		}

		this.updateArmSwingProgress();
		super.onLivingUpdate();
		
		this.clampMotion(0.05D);

		if (this.ticksExisted % 30 == 0) {
			this.playSound((net.minecraft.util.SoundEvent) net.minecraft.util.SoundEvent.REGISTRY
			 .getObject(new ResourceLocation("block.fire.ambient")), 1.0F, this.rand.nextFloat() * 0.7F + 0.3F);
		}
		for (int i = 0; i < (int) this.height; i++) {
			double d0 = this.posX + (this.rand.nextFloat() - 0.5D) * this.width;
			double d1 = this.posY + this.rand.nextFloat() * this.height;
			double d2 = this.posZ + (this.rand.nextFloat() - 0.5D) * this.width;
			this.world.spawnAlwaysVisibleParticle(Particles.Types.FLAME.getID(), d0, d1, d2, 0.0D, 0.05D, 0.0D, this.getFlameColor(), (int)(this.width * 15f));
		}
	}

	@Override
	protected void onDeathUpdate() {
		this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 1.0f, this.rand.nextFloat() * 0.4f + 0.7f);
		this.setDead();
	}

	@Override
    public void setSwingingArms(boolean swingingArms) {
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
    }

    public void attackEntityRanged(double x, double y, double z) {
    }

    public void createBullet(float size) {
    }

    public void killBullet() {
    }

    protected void showHeldWeapons() {
		EntityLivingBase owner = this.getOwnerPlayer();
		if (!this.world.isRemote && owner != null) {
			boolean flag = owner.getHeldItemMainhand().getItem() == ItemChokuto.block;
			if (this.shouldShowSword() != flag) {
				this.setShowSword(flag);
			}
			if (owner.getHeldItemMainhand().getItem() == ItemShuriken.block) {
				this.createBullet((float)this.getEntityData().getDouble("entityModelScale") * 0.5f);
			} else {
				this.killBullet();
			}
		}
    }

	public static class AIAttackRangedAndMoveTowardsTarget extends EntityAIBase {
	    private final EntitySusanooBase entityHost;
	    private final IRangedAttackMob rangedAttackEntityHost;
	    private EntityLivingBase attackTarget;
	    private int rangedAttackTime;
	    private final double entityMoveSpeed;
	    private final int maxRangedAttackTime;
	    private final float minAttackRadius;
		
	    public AIAttackRangedAndMoveTowardsTarget(IRangedAttackMob attacker, double movespeed, int maxAttackTime, float minAttackDistanceIn) {
	        if (!(attacker instanceof EntitySusanooBase)) {
	            throw new IllegalArgumentException("AIAttackRangedAndMoveTowardsTarget requires Mob implements EntitySusanooBase");
	        } else {
	            this.rangedAttackEntityHost = attacker;
	            this.entityHost = (EntitySusanooBase)attacker;
	            this.entityMoveSpeed = movespeed;
	            this.maxRangedAttackTime = maxAttackTime;
	    		this.rangedAttackTime = maxAttackTime;
	            this.minAttackRadius = minAttackDistanceIn;
	            this.setMutexBits(3);
	        }
	    }

	    @Override
	    public boolean shouldExecute() {
	        EntityLivingBase entitylivingbase = this.entityHost.getAttackTarget();
	        if (entitylivingbase == null) {
	        	return false;
	        } else if (entitylivingbase.getDistance(this.entityHost) < ProcedureUtils.getReachDistance(this.entityHost) + this.minAttackRadius) {
	        	return false;
	        } else {
	        	this.attackTarget = entitylivingbase;
	            return true;
	        }
	    }
		
	    @Override
	    public boolean shouldContinueExecuting() {
	        return this.shouldExecute() || !this.entityHost.getNavigator().noPath();
	    }
		
		@Override
		public void resetTask() {
		    this.attackTarget = null;
		    //this.rangedAttackTime = this.maxRangedAttackTime;
		}
		
		@Override
		public void updateTask() {
		    double d0 = this.entityHost.getDistance(this.attackTarget.posX, this.attackTarget.getEntityBoundingBox().minY, this.attackTarget.posZ);
		    if (d0 >= ProcedureUtils.getReachDistance(this.entityHost) + this.minAttackRadius) {
		        this.entityHost.getNavigator().tryMoveToEntityLiving(this.attackTarget, this.entityMoveSpeed);
		    } else {
		        this.entityHost.getNavigator().clearPath();
		    }
		    this.entityHost.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);
		    if (--this.rangedAttackTime <= 0) {
		        this.rangedAttackEntityHost.attackEntityWithRangedAttack(this.attackTarget, 1.0F);
		        this.rangedAttackTime = this.maxRangedAttackTime;
		    }
		}
	}
}
