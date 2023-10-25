package net.minecraft.world;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.实体Player;
import net.minecraft.entity.实体;
import net.minecraft.entity.实体Living;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.阻止位置;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;

public abstract class World implements IBlockAccess
{
    private int seaLevel = 63;
    protected boolean scheduledUpdatesAreImmediate;
    public final List<实体> loaded实体List = Lists.<实体>newArrayList();
    protected final List<实体> unloaded实体List = Lists.<实体>newArrayList();
    public final List<TileEntity> loadedTileEntityList = Lists.<TileEntity>newArrayList();
    public final List<TileEntity> tickableTileEntities = Lists.<TileEntity>newArrayList();
    private final List<TileEntity> addedTileEntityList = Lists.<TileEntity>newArrayList();
    private final List<TileEntity> tileEntitiesToBeRemoved = Lists.<TileEntity>newArrayList();
    public final List<实体Player> playerEntities = Lists.<实体Player>newArrayList();
    public final List<实体> weatherEffects = Lists.<实体>newArrayList();
    protected final IntHashMap<实体> entitiesById = new IntHashMap();
    private long cloudColour = 16777215L;
    private int skylightSubtracted;
    protected int updateLCG = (new Random()).nextInt();
    protected final int DIST_HASH_MAGIC = 1013904223;
    protected float prevRainingStrength;
    protected float rainingStrength;
    protected float prevThunderingStrength;
    protected float thunderingStrength;
    private int lastLightningBolt;
    public final Random rand = new Random();
    public final WorldProvider provider;
    protected List<IWorldAccess> worldAccesses = Lists.<IWorldAccess>newArrayList();
    protected IChunkProvider chunkProvider;
    protected final ISaveHandler saveHandler;
    protected WorldInfo worldInfo;
    protected boolean findingSpawnPoint;
    protected MapStorage mapStorage;
    protected VillageCollection villageCollectionObj;
    public final Profiler theProfiler;
    private final Calendar theCalendar = Calendar.getInstance();
    protected Scoreboard worldScoreboard = new Scoreboard();
    public final boolean isRemote;
    protected Set<ChunkCoordIntPair> activeChunkSet = Sets.<ChunkCoordIntPair>newHashSet();
    private int ambientTickCountdown;
    protected boolean spawnHostileMobs;
    protected boolean spawnPeacefulMobs;
    private boolean processingLoadedTiles;
    private final WorldBorder worldBorder;
    int[] lightUpdateBlockList;

    protected World(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client)
    {
        this.ambientTickCountdown = this.rand.nextInt(12000);
        this.spawnHostileMobs = true;
        this.spawnPeacefulMobs = true;
        this.lightUpdateBlockList = new int[32768];
        this.saveHandler = saveHandlerIn;
        this.theProfiler = profilerIn;
        this.worldInfo = info;
        this.provider = providerIn;
        this.isRemote = client;
        this.worldBorder = providerIn.getWorldBorder();
    }

    public World init()
    {
        return this;
    }

    public BiomeGenBase getBiomeGenForCoords(final 阻止位置 pos)
    {
        if (this.isBlockLoaded(pos))
        {
            Chunk chunk = this.getChunkFromBlockCoords(pos);

            try
            {
                return chunk.getBiome(pos, this.provider.getWorldChunkManager());
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting biome");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Coordinates of biome request");
                crashreportcategory.addCrashSectionCallable("Location", new Callable<String>()
                {
                    public String call() throws Exception
                    {
                        return CrashReportCategory.getCoordinateInfo(pos);
                    }
                });
                throw new ReportedException(crashreport);
            }
        }
        else
        {
            return this.provider.getWorldChunkManager().getBiomeGenerator(pos, BiomeGenBase.plains);
        }
    }

    public WorldChunkManager getWorldChunkManager()
    {
        return this.provider.getWorldChunkManager();
    }

    protected abstract IChunkProvider createChunkProvider();

    public void initialize(WorldSettings settings)
    {
        this.worldInfo.setServerInitialized(true);
    }

    public void setInitialSpawnLocation()
    {
        this.setSpawnPoint(new 阻止位置(8, 64, 8));
    }

    public Block getGroundAboveSeaLevel(阻止位置 pos)
    {
        阻止位置 blockpos;

        for (blockpos = new 阻止位置(pos.getX(), this.getSeaLevel(), pos.getZ()); !this.isAirBlock(blockpos.up()); blockpos = blockpos.up())
        {
            ;
        }

        return this.getBlockState(blockpos).getBlock();
    }

    private boolean isValid(阻止位置 pos)
    {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000 && pos.getY() >= 0 && pos.getY() < 256;
    }

    public boolean isAirBlock(阻止位置 pos)
    {
        return this.getBlockState(pos).getBlock().getMaterial() == Material.air;
    }

    public boolean isBlockLoaded(阻止位置 pos)
    {
        return this.isBlockLoaded(pos, true);
    }

    public boolean isBlockLoaded(阻止位置 pos, boolean allowEmpty)
    {
        return !this.isValid(pos) ? false : this.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4, allowEmpty);
    }

    public boolean isAreaLoaded(阻止位置 center, int radius)
    {
        return this.isAreaLoaded(center, radius, true);
    }

    public boolean isAreaLoaded(阻止位置 center, int radius, boolean allowEmpty)
    {
        return this.isAreaLoaded(center.getX() - radius, center.getY() - radius, center.getZ() - radius, center.getX() + radius, center.getY() + radius, center.getZ() + radius, allowEmpty);
    }

    public boolean isAreaLoaded(阻止位置 from, 阻止位置 to)
    {
        return this.isAreaLoaded(from, to, true);
    }

    public boolean isAreaLoaded(阻止位置 from, 阻止位置 to, boolean allowEmpty)
    {
        return this.isAreaLoaded(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), allowEmpty);
    }

    public boolean isAreaLoaded(StructureBoundingBox box)
    {
        return this.isAreaLoaded(box, true);
    }

    public boolean isAreaLoaded(StructureBoundingBox box, boolean allowEmpty)
    {
        return this.isAreaLoaded(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, allowEmpty);
    }

    private boolean isAreaLoaded(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd, boolean allowEmpty)
    {
        if (yEnd >= 0 && yStart < 256)
        {
            xStart = xStart >> 4;
            zStart = zStart >> 4;
            xEnd = xEnd >> 4;
            zEnd = zEnd >> 4;

            for (int i = xStart; i <= xEnd; ++i)
            {
                for (int j = zStart; j <= zEnd; ++j)
                {
                    if (!this.isChunkLoaded(i, j, allowEmpty))
                    {
                        return false;
                    }
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty)
    {
        return this.chunkProvider.chunkExists(x, z) && (allowEmpty || !this.chunkProvider.provideChunk(x, z).isEmpty());
    }

    public Chunk getChunkFromBlockCoords(阻止位置 pos)
    {
        return this.getChunkFromChunkCoords(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ)
    {
        return this.chunkProvider.provideChunk(chunkX, chunkZ);
    }

    public boolean setBlockState(阻止位置 pos, IBlockState newState, int flags)
    {
        if (!this.isValid(pos))
        {
            return false;
        }
        else if (!this.isRemote && this.worldInfo.getTerrainType() == WorldType.DEBUG_WORLD)
        {
            return false;
        }
        else
        {
            Chunk chunk = this.getChunkFromBlockCoords(pos);
            Block block = newState.getBlock();
            IBlockState iblockstate = chunk.setBlockState(pos, newState);

            if (iblockstate == null)
            {
                return false;
            }
            else
            {
                Block block1 = iblockstate.getBlock();

                if (block.getLightOpacity() != block1.getLightOpacity() || block.getLightValue() != block1.getLightValue())
                {
                    this.theProfiler.startSection("checkLight");
                    this.checkLight(pos);
                    this.theProfiler.endSection();
                }

                if ((flags & 2) != 0 && (!this.isRemote || (flags & 4) == 0) && chunk.isPopulated())
                {
                    this.markBlockForUpdate(pos);
                }

                if (!this.isRemote && (flags & 1) != 0)
                {
                    this.notifyNeighborsRespectDebug(pos, iblockstate.getBlock());

                    if (block.hasComparatorInputOverride())
                    {
                        this.updateComparatorOutputLevel(pos, block);
                    }
                }

                return true;
            }
        }
    }

    public boolean setBlockToAir(阻止位置 pos)
    {
        return this.setBlockState(pos, Blocks.air.getDefaultState(), 3);
    }

    public boolean destroyBlock(阻止位置 pos, boolean dropBlock)
    {
        IBlockState iblockstate = this.getBlockState(pos);
        Block block = iblockstate.getBlock();

        if (block.getMaterial() == Material.air)
        {
            return false;
        }
        else
        {
            this.playAuxSFX(2001, pos, Block.getStateId(iblockstate));

            if (dropBlock)
            {
                block.dropBlockAsItem(this, pos, iblockstate, 0);
            }

            return this.setBlockState(pos, Blocks.air.getDefaultState(), 3);
        }
    }

    public boolean setBlockState(阻止位置 pos, IBlockState state)
    {
        return this.setBlockState(pos, state, 3);
    }

    public void markBlockForUpdate(阻止位置 pos)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).markBlockForUpdate(pos);
        }
    }

    public void notifyNeighborsRespectDebug(阻止位置 pos, Block blockType)
    {
        if (this.worldInfo.getTerrainType() != WorldType.DEBUG_WORLD)
        {
            this.notifyNeighborsOfStateChange(pos, blockType);
        }
    }

    public void markBlocksDirtyVertical(int x1, int z1, int x2, int z2)
    {
        if (x2 > z2)
        {
            int i = z2;
            z2 = x2;
            x2 = i;
        }

        if (!this.provider.getHasNoSky())
        {
            for (int j = x2; j <= z2; ++j)
            {
                this.checkLightFor(EnumSkyBlock.SKY, new 阻止位置(x1, j, z1));
            }
        }

        this.markBlockRangeForRenderUpdate(x1, x2, z1, x1, z2, z1);
    }

    public void markBlockRangeForRenderUpdate(阻止位置 rangeMin, 阻止位置 rangeMax)
    {
        this.markBlockRangeForRenderUpdate(rangeMin.getX(), rangeMin.getY(), rangeMin.getZ(), rangeMax.getX(), rangeMax.getY(), rangeMax.getZ());
    }

    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2);
        }
    }

    public void notifyNeighborsOfStateChange(阻止位置 pos, Block blockType)
    {
        this.notifyBlockOfStateChange(pos.west(), blockType);
        this.notifyBlockOfStateChange(pos.east(), blockType);
        this.notifyBlockOfStateChange(pos.down(), blockType);
        this.notifyBlockOfStateChange(pos.up(), blockType);
        this.notifyBlockOfStateChange(pos.north(), blockType);
        this.notifyBlockOfStateChange(pos.south(), blockType);
    }

    public void notifyNeighborsOfStateExcept(阻止位置 pos, Block blockType, EnumFacing skipSide)
    {
        if (skipSide != EnumFacing.WEST)
        {
            this.notifyBlockOfStateChange(pos.west(), blockType);
        }

        if (skipSide != EnumFacing.EAST)
        {
            this.notifyBlockOfStateChange(pos.east(), blockType);
        }

        if (skipSide != EnumFacing.DOWN)
        {
            this.notifyBlockOfStateChange(pos.down(), blockType);
        }

        if (skipSide != EnumFacing.UP)
        {
            this.notifyBlockOfStateChange(pos.up(), blockType);
        }

        if (skipSide != EnumFacing.NORTH)
        {
            this.notifyBlockOfStateChange(pos.north(), blockType);
        }

        if (skipSide != EnumFacing.SOUTH)
        {
            this.notifyBlockOfStateChange(pos.south(), blockType);
        }
    }

    public void notifyBlockOfStateChange(阻止位置 pos, final Block blockIn)
    {
        if (!this.isRemote)
        {
            IBlockState iblockstate = this.getBlockState(pos);

            try
            {
                iblockstate.getBlock().onNeighborBlockChange(this, pos, iblockstate, blockIn);
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception while updating neighbours");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being updated");
                crashreportcategory.addCrashSectionCallable("Source block type", new Callable<String>()
                {
                    public String call() throws Exception
                    {
                        try
                        {
                            return String.format("ID #%d (%s // %s)", new Object[] {Integer.valueOf(Block.getIdFromBlock(blockIn)), blockIn.getUnlocalizedName(), blockIn.getClass().getCanonicalName()});
                        }
                        catch (Throwable var2)
                        {
                            return "ID #" + Block.getIdFromBlock(blockIn);
                        }
                    }
                });
                CrashReportCategory.addBlockInfo(crashreportcategory, pos, iblockstate);
                throw new ReportedException(crashreport);
            }
        }
    }

    public boolean isBlockTickPending(阻止位置 pos, Block blockType)
    {
        return false;
    }

    public boolean canSeeSky(阻止位置 pos)
    {
        return this.getChunkFromBlockCoords(pos).canSeeSky(pos);
    }

    public boolean canBlockSeeSky(阻止位置 pos)
    {
        if (pos.getY() >= this.getSeaLevel())
        {
            return this.canSeeSky(pos);
        }
        else
        {
            阻止位置 blockpos = new 阻止位置(pos.getX(), this.getSeaLevel(), pos.getZ());

            if (!this.canSeeSky(blockpos))
            {
                return false;
            }
            else
            {
                for (blockpos = blockpos.down(); blockpos.getY() > pos.getY(); blockpos = blockpos.down())
                {
                    Block block = this.getBlockState(blockpos).getBlock();

                    if (block.getLightOpacity() > 0 && !block.getMaterial().isLiquid())
                    {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public int getLight(阻止位置 pos)
    {
        if (pos.getY() < 0)
        {
            return 0;
        }
        else
        {
            if (pos.getY() >= 256)
            {
                pos = new 阻止位置(pos.getX(), 255, pos.getZ());
            }

            return this.getChunkFromBlockCoords(pos).getLightSubtracted(pos, 0);
        }
    }

    public int getLightFromNeighbors(阻止位置 pos)
    {
        return this.getLight(pos, true);
    }

    public int getLight(阻止位置 pos, boolean checkNeighbors)
    {
        if (pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000)
        {
            if (checkNeighbors && this.getBlockState(pos).getBlock().getUseNeighborBrightness())
            {
                int i1 = this.getLight(pos.up(), false);
                int i = this.getLight(pos.east(), false);
                int j = this.getLight(pos.west(), false);
                int k = this.getLight(pos.south(), false);
                int l = this.getLight(pos.north(), false);

                if (i > i1)
                {
                    i1 = i;
                }

                if (j > i1)
                {
                    i1 = j;
                }

                if (k > i1)
                {
                    i1 = k;
                }

                if (l > i1)
                {
                    i1 = l;
                }

                return i1;
            }
            else if (pos.getY() < 0)
            {
                return 0;
            }
            else
            {
                if (pos.getY() >= 256)
                {
                    pos = new 阻止位置(pos.getX(), 255, pos.getZ());
                }

                Chunk chunk = this.getChunkFromBlockCoords(pos);
                return chunk.getLightSubtracted(pos, this.skylightSubtracted);
            }
        }
        else
        {
            return 15;
        }
    }

    public 阻止位置 getHeight(阻止位置 pos)
    {
        int i;

        if (pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000)
        {
            if (this.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4, true))
            {
                i = this.getChunkFromChunkCoords(pos.getX() >> 4, pos.getZ() >> 4).getHeightValue(pos.getX() & 15, pos.getZ() & 15);
            }
            else
            {
                i = 0;
            }
        }
        else
        {
            i = this.getSeaLevel() + 1;
        }

        return new 阻止位置(pos.getX(), i, pos.getZ());
    }

    public int getChunksLowestHorizon(int x, int z)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
        {
            if (!this.isChunkLoaded(x >> 4, z >> 4, true))
            {
                return 0;
            }
            else
            {
                Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
                return chunk.getLowestHeight();
            }
        }
        else
        {
            return this.getSeaLevel() + 1;
        }
    }

    public int getLightFromNeighborsFor(EnumSkyBlock type, 阻止位置 pos)
    {
        if (this.provider.getHasNoSky() && type == EnumSkyBlock.SKY)
        {
            return 0;
        }
        else
        {
            if (pos.getY() < 0)
            {
                pos = new 阻止位置(pos.getX(), 0, pos.getZ());
            }

            if (!this.isValid(pos))
            {
                return type.defaultLightValue;
            }
            else if (!this.isBlockLoaded(pos))
            {
                return type.defaultLightValue;
            }
            else if (this.getBlockState(pos).getBlock().getUseNeighborBrightness())
            {
                int i1 = this.getLightFor(type, pos.up());
                int i = this.getLightFor(type, pos.east());
                int j = this.getLightFor(type, pos.west());
                int k = this.getLightFor(type, pos.south());
                int l = this.getLightFor(type, pos.north());

                if (i > i1)
                {
                    i1 = i;
                }

                if (j > i1)
                {
                    i1 = j;
                }

                if (k > i1)
                {
                    i1 = k;
                }

                if (l > i1)
                {
                    i1 = l;
                }

                return i1;
            }
            else
            {
                Chunk chunk = this.getChunkFromBlockCoords(pos);
                return chunk.getLightFor(type, pos);
            }
        }
    }

    public int getLightFor(EnumSkyBlock type, 阻止位置 pos)
    {
        if (pos.getY() < 0)
        {
            pos = new 阻止位置(pos.getX(), 0, pos.getZ());
        }

        if (!this.isValid(pos))
        {
            return type.defaultLightValue;
        }
        else if (!this.isBlockLoaded(pos))
        {
            return type.defaultLightValue;
        }
        else
        {
            Chunk chunk = this.getChunkFromBlockCoords(pos);
            return chunk.getLightFor(type, pos);
        }
    }

    public void setLightFor(EnumSkyBlock type, 阻止位置 pos, int lightValue)
    {
        if (this.isValid(pos))
        {
            if (this.isBlockLoaded(pos))
            {
                Chunk chunk = this.getChunkFromBlockCoords(pos);
                chunk.setLightFor(type, pos, lightValue);
                this.notifyLightSet(pos);
            }
        }
    }

    public void notifyLightSet(阻止位置 pos)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).notifyLightSet(pos);
        }
    }

    public int getCombinedLight(阻止位置 pos, int lightValue)
    {
        int i = this.getLightFromNeighborsFor(EnumSkyBlock.SKY, pos);
        int j = this.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos);

        if (j < lightValue)
        {
            j = lightValue;
        }

        return i << 20 | j << 4;
    }

    public float getLightBrightness(阻止位置 pos)
    {
        return this.provider.getLightBrightnessTable()[this.getLightFromNeighbors(pos)];
    }

    public IBlockState getBlockState(阻止位置 pos)
    {
        if (!this.isValid(pos))
        {
            return Blocks.air.getDefaultState();
        }
        else
        {
            Chunk chunk = this.getChunkFromBlockCoords(pos);
            return chunk.getBlockState(pos);
        }
    }

    public boolean isDaytime()
    {
        return this.skylightSubtracted < 4;
    }

    public MovingObjectPosition rayTraceBlocks(Vec3 p_72933_1_, Vec3 p_72933_2_)
    {
        return this.rayTraceBlocks(p_72933_1_, p_72933_2_, false, false, false);
    }

    public MovingObjectPosition rayTraceBlocks(Vec3 start, Vec3 end, boolean stopOnLiquid)
    {
        return this.rayTraceBlocks(start, end, stopOnLiquid, false, false);
    }

    public MovingObjectPosition rayTraceBlocks(Vec3 vec31, Vec3 vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock)
    {
        if (!Double.isNaN(vec31.xCoord) && !Double.isNaN(vec31.yCoord) && !Double.isNaN(vec31.zCoord))
        {
            if (!Double.isNaN(vec32.xCoord) && !Double.isNaN(vec32.yCoord) && !Double.isNaN(vec32.zCoord))
            {
                int i = MathHelper.floor_double(vec32.xCoord);
                int j = MathHelper.floor_double(vec32.yCoord);
                int k = MathHelper.floor_double(vec32.zCoord);
                int l = MathHelper.floor_double(vec31.xCoord);
                int i1 = MathHelper.floor_double(vec31.yCoord);
                int j1 = MathHelper.floor_double(vec31.zCoord);
                阻止位置 blockpos = new 阻止位置(l, i1, j1);
                IBlockState iblockstate = this.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if ((!ignoreBlockWithoutBoundingBox || block.getCollisionBoundingBox(this, blockpos, iblockstate) != null) && block.canCollideCheck(iblockstate, stopOnLiquid))
                {
                    MovingObjectPosition movingobjectposition = block.collisionRayTrace(this, blockpos, vec31, vec32);

                    if (movingobjectposition != null)
                    {
                        return movingobjectposition;
                    }
                }

                MovingObjectPosition movingobjectposition2 = null;
                int k1 = 200;

                while (k1-- >= 0)
                {
                    if (Double.isNaN(vec31.xCoord) || Double.isNaN(vec31.yCoord) || Double.isNaN(vec31.zCoord))
                    {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k)
                    {
                        return returnLastUncollidableBlock ? movingobjectposition2 : null;
                    }

                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l)
                    {
                        d0 = (double)l + 1.0D;
                    }
                    else if (i < l)
                    {
                        d0 = (double)l + 0.0D;
                    }
                    else
                    {
                        flag2 = false;
                    }

                    if (j > i1)
                    {
                        d1 = (double)i1 + 1.0D;
                    }
                    else if (j < i1)
                    {
                        d1 = (double)i1 + 0.0D;
                    }
                    else
                    {
                        flag = false;
                    }

                    if (k > j1)
                    {
                        d2 = (double)j1 + 1.0D;
                    }
                    else if (k < j1)
                    {
                        d2 = (double)j1 + 0.0D;
                    }
                    else
                    {
                        flag1 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = vec32.xCoord - vec31.xCoord;
                    double d7 = vec32.yCoord - vec31.yCoord;
                    double d8 = vec32.zCoord - vec31.zCoord;

                    if (flag2)
                    {
                        d3 = (d0 - vec31.xCoord) / d6;
                    }

                    if (flag)
                    {
                        d4 = (d1 - vec31.yCoord) / d7;
                    }

                    if (flag1)
                    {
                        d5 = (d2 - vec31.zCoord) / d8;
                    }

                    if (d3 == -0.0D)
                    {
                        d3 = -1.0E-4D;
                    }

                    if (d4 == -0.0D)
                    {
                        d4 = -1.0E-4D;
                    }

                    if (d5 == -0.0D)
                    {
                        d5 = -1.0E-4D;
                    }

                    EnumFacing enumfacing;

                    if (d3 < d4 && d3 < d5)
                    {
                        enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3(d0, vec31.yCoord + d7 * d3, vec31.zCoord + d8 * d3);
                    }
                    else if (d4 < d5)
                    {
                        enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3(vec31.xCoord + d6 * d4, d1, vec31.zCoord + d8 * d4);
                    }
                    else
                    {
                        enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3(vec31.xCoord + d6 * d5, vec31.yCoord + d7 * d5, d2);
                    }

                    l = MathHelper.floor_double(vec31.xCoord) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    i1 = MathHelper.floor_double(vec31.yCoord) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    j1 = MathHelper.floor_double(vec31.zCoord) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    blockpos = new 阻止位置(l, i1, j1);
                    IBlockState iblockstate1 = this.getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();

                    if (!ignoreBlockWithoutBoundingBox || block1.getCollisionBoundingBox(this, blockpos, iblockstate1) != null)
                    {
                        if (block1.canCollideCheck(iblockstate1, stopOnLiquid))
                        {
                            MovingObjectPosition movingobjectposition1 = block1.collisionRayTrace(this, blockpos, vec31, vec32);

                            if (movingobjectposition1 != null)
                            {
                                return movingobjectposition1;
                            }
                        }
                        else
                        {
                            movingobjectposition2 = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec31, enumfacing, blockpos);
                        }
                    }
                }

                return returnLastUncollidableBlock ? movingobjectposition2 : null;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    public void playSoundAtEntity(实体 实体In, String name, float volume, float pitch)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).playSound(name, 实体In.X坐标, 实体In.Y坐标, 实体In.Z坐标, volume, pitch);
        }
    }

    public void playSoundToNearExcept(实体Player player, String name, float volume, float pitch)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).playSoundToNearExcept(player, name, player.X坐标, player.Y坐标, player.Z坐标, volume, pitch);
        }
    }

    public void playSoundEffect(double x, double y, double z, String soundName, float volume, float pitch)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).playSound(soundName, x, y, z, volume, pitch);
        }
    }

    public void playSound(double x, double y, double z, String soundName, float volume, float pitch, boolean distanceDelay)
    {
    }

    public void playRecord(阻止位置 pos, String name)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).playRecord(name, pos);
        }
    }

    public void spawnParticle(EnumParticleTypes particleType, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... p_175688_14_)
    {
        this.spawnParticle(particleType.getParticleID(), particleType.getShouldIgnoreRange(), xCoord, yCoord, zCoord, xOffset, yOffset, zOffset, p_175688_14_);
    }

    public void spawnParticle(EnumParticleTypes particleType, boolean p_175682_2_, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... p_175682_15_)
    {
        this.spawnParticle(particleType.getParticleID(), particleType.getShouldIgnoreRange() | p_175682_2_, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset, p_175682_15_);
    }

    private void spawnParticle(int particleID, boolean p_175720_2_, double xCood, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int... p_175720_15_)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).spawnParticle(particleID, p_175720_2_, xCood, yCoord, zCoord, xOffset, yOffset, zOffset, p_175720_15_);
        }
    }

    public boolean addWeatherEffect(实体 实体In)
    {
        this.weatherEffects.add(实体In);
        return true;
    }

    public boolean spawnEntityInWorld(实体 实体In)
    {
        int i = MathHelper.floor_double(实体In.X坐标 / 16.0D);
        int j = MathHelper.floor_double(实体In.Z坐标 / 16.0D);
        boolean flag = 实体In.forceSpawn;

        if (实体In instanceof 实体Player)
        {
            flag = true;
        }

        if (!flag && !this.isChunkLoaded(i, j, true))
        {
            return false;
        }
        else
        {
            if (实体In instanceof 实体Player)
            {
                实体Player entityplayer = (实体Player) 实体In;
                this.playerEntities.add(entityplayer);
                this.updateAllPlayersSleepingFlag();
            }

            this.getChunkFromChunkCoords(i, j).addEntity(实体In);
            this.loaded实体List.add(实体In);
            this.onEntityAdded(实体In);
            return true;
        }
    }

    protected void onEntityAdded(实体 实体In)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).onEntityAdded(实体In);
        }
    }

    protected void onEntityRemoved(实体 实体In)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).onEntityRemoved(实体In);
        }
    }

    public void removeEntity(实体 实体In)
    {
        if (实体In.riddenBy实体 != null)
        {
            实体In.riddenBy实体.mountEntity((实体)null);
        }

        if (实体In.riding实体 != null)
        {
            实体In.mountEntity((实体)null);
        }

        实体In.setDead();

        if (实体In instanceof 实体Player)
        {
            this.playerEntities.remove(实体In);
            this.updateAllPlayersSleepingFlag();
            this.onEntityRemoved(实体In);
        }
    }

    public void removePlayerEntityDangerously(实体 实体In)
    {
        实体In.setDead();

        if (实体In instanceof 实体Player)
        {
            this.playerEntities.remove(实体In);
            this.updateAllPlayersSleepingFlag();
        }

        int i = 实体In.chunkCoordX;
        int j = 实体In.chunkCoordZ;

        if (实体In.addedToChunk && this.isChunkLoaded(i, j, true))
        {
            this.getChunkFromChunkCoords(i, j).removeEntity(实体In);
        }

        this.loaded实体List.remove(实体In);
        this.onEntityRemoved(实体In);
    }

    public void addWorldAccess(IWorldAccess worldAccess)
    {
        this.worldAccesses.add(worldAccess);
    }

    public void removeWorldAccess(IWorldAccess worldAccess)
    {
        this.worldAccesses.remove(worldAccess);
    }

    public List<AxisAlignedBB> getCollidingBoundingBoxes(实体 实体In, AxisAlignedBB bb)
    {
        List<AxisAlignedBB> list = Lists.<AxisAlignedBB>newArrayList();
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX + 1.0D);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY + 1.0D);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ + 1.0D);
        WorldBorder worldborder = this.getWorldBorder();
        boolean flag = 实体In.isOutsideBorder();
        boolean flag1 = this.isInsideBorder(worldborder, 实体In);
        IBlockState iblockstate = Blocks.stone.getDefaultState();
        阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = i1; l1 < j1; ++l1)
            {
                if (this.isBlockLoaded(blockpos$mutableblockpos.set(k1, 64, l1)))
                {
                    for (int i2 = k - 1; i2 < l; ++i2)
                    {
                        blockpos$mutableblockpos.set(k1, i2, l1);

                        if (flag && flag1)
                        {
                            实体In.setOutsideBorder(false);
                        }
                        else if (!flag && !flag1)
                        {
                            实体In.setOutsideBorder(true);
                        }

                        IBlockState iblockstate1 = iblockstate;

                        if (worldborder.contains(blockpos$mutableblockpos) || !flag1)
                        {
                            iblockstate1 = this.getBlockState(blockpos$mutableblockpos);
                        }

                        iblockstate1.getBlock().addCollisionBoxesToList(this, blockpos$mutableblockpos, iblockstate1, bb, list, 实体In);
                    }
                }
            }
        }

        double d0 = 0.25D;
        List<实体> list1 = this.getEntitiesWithinAABBExcludingEntity(实体In, bb.expand(d0, d0, d0));

        for (int j2 = 0; j2 < list1.size(); ++j2)
        {
            if (实体In.riddenBy实体 != list1 && 实体In.riding实体 != list1)
            {
                AxisAlignedBB axisalignedbb = ((实体)list1.get(j2)).getCollisionBoundingBox();

                if (axisalignedbb != null && axisalignedbb.intersectsWith(bb))
                {
                    list.add(axisalignedbb);
                }

                axisalignedbb = 实体In.getCollisionBox((实体)list1.get(j2));

                if (axisalignedbb != null && axisalignedbb.intersectsWith(bb))
                {
                    list.add(axisalignedbb);
                }
            }
        }

        return list;
    }

    public boolean isInsideBorder(WorldBorder worldBorderIn, 实体 实体In)
    {
        double d0 = worldBorderIn.minX();
        double d1 = worldBorderIn.minZ();
        double d2 = worldBorderIn.maxX();
        double d3 = worldBorderIn.maxZ();

        if (实体In.isOutsideBorder())
        {
            ++d0;
            ++d1;
            --d2;
            --d3;
        }
        else
        {
            --d0;
            --d1;
            ++d2;
            ++d3;
        }

        return 实体In.X坐标 > d0 && 实体In.X坐标 < d2 && 实体In.Z坐标 > d1 && 实体In.Z坐标 < d3;
    }

    public List<AxisAlignedBB> getCollisionBoxes(AxisAlignedBB bb)
    {
        List<AxisAlignedBB> list = Lists.<AxisAlignedBB>newArrayList();
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX + 1.0D);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY + 1.0D);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ + 1.0D);
        阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = i1; l1 < j1; ++l1)
            {
                if (this.isBlockLoaded(blockpos$mutableblockpos.set(k1, 64, l1)))
                {
                    for (int i2 = k - 1; i2 < l; ++i2)
                    {
                        blockpos$mutableblockpos.set(k1, i2, l1);
                        IBlockState iblockstate;

                        if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000)
                        {
                            iblockstate = this.getBlockState(blockpos$mutableblockpos);
                        }
                        else
                        {
                            iblockstate = Blocks.bedrock.getDefaultState();
                        }

                        iblockstate.getBlock().addCollisionBoxesToList(this, blockpos$mutableblockpos, iblockstate, bb, list, (实体)null);
                    }
                }
            }
        }

        return list;
    }

    public int calculateSkylightSubtracted(float p_72967_1_)
    {
        float f = this.getCelestialAngle(p_72967_1_);
        float f1 = 1.0F - (MathHelper.cos(f * (float)Math.PI * 2.0F) * 2.0F + 0.5F);
        f1 = MathHelper.clamp_float(f1, 0.0F, 1.0F);
        f1 = 1.0F - f1;
        f1 = (float)((double)f1 * (1.0D - (double)(this.getRainStrength(p_72967_1_) * 5.0F) / 16.0D));
        f1 = (float)((double)f1 * (1.0D - (double)(this.getThunderStrength(p_72967_1_) * 5.0F) / 16.0D));
        f1 = 1.0F - f1;
        return (int)(f1 * 11.0F);
    }

    public float getSunBrightness(float p_72971_1_)
    {
        float f = this.getCelestialAngle(p_72971_1_);
        float f1 = 1.0F - (MathHelper.cos(f * (float)Math.PI * 2.0F) * 2.0F + 0.2F);
        f1 = MathHelper.clamp_float(f1, 0.0F, 1.0F);
        f1 = 1.0F - f1;
        f1 = (float)((double)f1 * (1.0D - (double)(this.getRainStrength(p_72971_1_) * 5.0F) / 16.0D));
        f1 = (float)((double)f1 * (1.0D - (double)(this.getThunderStrength(p_72971_1_) * 5.0F) / 16.0D));
        return f1 * 0.8F + 0.2F;
    }

    public Vec3 getSkyColor(实体 实体In, float partialTicks)
    {
        float f = this.getCelestialAngle(partialTicks);
        float f1 = MathHelper.cos(f * (float)Math.PI * 2.0F) * 2.0F + 0.5F;
        f1 = MathHelper.clamp_float(f1, 0.0F, 1.0F);
        int i = MathHelper.floor_double(实体In.X坐标);
        int j = MathHelper.floor_double(实体In.Y坐标);
        int k = MathHelper.floor_double(实体In.Z坐标);
        阻止位置 blockpos = new 阻止位置(i, j, k);
        BiomeGenBase biomegenbase = this.getBiomeGenForCoords(blockpos);
        float f2 = biomegenbase.getFloatTemperature(blockpos);
        int l = biomegenbase.getSkyColorByTemp(f2);
        float f3 = (float)(l >> 16 & 255) / 255.0F;
        float f4 = (float)(l >> 8 & 255) / 255.0F;
        float f5 = (float)(l & 255) / 255.0F;
        f3 = f3 * f1;
        f4 = f4 * f1;
        f5 = f5 * f1;
        float f6 = this.getRainStrength(partialTicks);

        if (f6 > 0.0F)
        {
            float f7 = (f3 * 0.3F + f4 * 0.59F + f5 * 0.11F) * 0.6F;
            float f8 = 1.0F - f6 * 0.75F;
            f3 = f3 * f8 + f7 * (1.0F - f8);
            f4 = f4 * f8 + f7 * (1.0F - f8);
            f5 = f5 * f8 + f7 * (1.0F - f8);
        }

        float f10 = this.getThunderStrength(partialTicks);

        if (f10 > 0.0F)
        {
            float f11 = (f3 * 0.3F + f4 * 0.59F + f5 * 0.11F) * 0.2F;
            float f9 = 1.0F - f10 * 0.75F;
            f3 = f3 * f9 + f11 * (1.0F - f9);
            f4 = f4 * f9 + f11 * (1.0F - f9);
            f5 = f5 * f9 + f11 * (1.0F - f9);
        }

        if (this.lastLightningBolt > 0)
        {
            float f12 = (float)this.lastLightningBolt - partialTicks;

            if (f12 > 1.0F)
            {
                f12 = 1.0F;
            }

            f12 = f12 * 0.45F;
            f3 = f3 * (1.0F - f12) + 0.8F * f12;
            f4 = f4 * (1.0F - f12) + 0.8F * f12;
            f5 = f5 * (1.0F - f12) + 1.0F * f12;
        }

        return new Vec3((double)f3, (double)f4, (double)f5);
    }

    public float getCelestialAngle(float partialTicks)
    {
        return this.provider.calculateCelestialAngle(this.worldInfo.getWorldTime(), partialTicks);
    }

    public int getMoonPhase()
    {
        return this.provider.getMoonPhase(this.worldInfo.getWorldTime());
    }

    public float getCurrentMoonPhaseFactor()
    {
        return WorldProvider.moonPhaseFactors[this.provider.getMoonPhase(this.worldInfo.getWorldTime())];
    }

    public float getCelestialAngleRadians(float partialTicks)
    {
        float f = this.getCelestialAngle(partialTicks);
        return f * (float)Math.PI * 2.0F;
    }

    public Vec3 getCloudColour(float partialTicks)
    {
        float f = this.getCelestialAngle(partialTicks);
        float f1 = MathHelper.cos(f * (float)Math.PI * 2.0F) * 2.0F + 0.5F;
        f1 = MathHelper.clamp_float(f1, 0.0F, 1.0F);
        float f2 = (float)(this.cloudColour >> 16 & 255L) / 255.0F;
        float f3 = (float)(this.cloudColour >> 8 & 255L) / 255.0F;
        float f4 = (float)(this.cloudColour & 255L) / 255.0F;
        float f5 = this.getRainStrength(partialTicks);

        if (f5 > 0.0F)
        {
            float f6 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.6F;
            float f7 = 1.0F - f5 * 0.95F;
            f2 = f2 * f7 + f6 * (1.0F - f7);
            f3 = f3 * f7 + f6 * (1.0F - f7);
            f4 = f4 * f7 + f6 * (1.0F - f7);
        }

        f2 = f2 * (f1 * 0.9F + 0.1F);
        f3 = f3 * (f1 * 0.9F + 0.1F);
        f4 = f4 * (f1 * 0.85F + 0.15F);
        float f9 = this.getThunderStrength(partialTicks);

        if (f9 > 0.0F)
        {
            float f10 = (f2 * 0.3F + f3 * 0.59F + f4 * 0.11F) * 0.2F;
            float f8 = 1.0F - f9 * 0.95F;
            f2 = f2 * f8 + f10 * (1.0F - f8);
            f3 = f3 * f8 + f10 * (1.0F - f8);
            f4 = f4 * f8 + f10 * (1.0F - f8);
        }

        return new Vec3((double)f2, (double)f3, (double)f4);
    }

    public Vec3 getFogColor(float partialTicks)
    {
        float f = this.getCelestialAngle(partialTicks);
        return this.provider.getFogColor(f, partialTicks);
    }

    public 阻止位置 getPrecipitationHeight(阻止位置 pos)
    {
        return this.getChunkFromBlockCoords(pos).getPrecipitationHeight(pos);
    }

    public 阻止位置 getTopSolidOrLiquidBlock(阻止位置 pos)
    {
        Chunk chunk = this.getChunkFromBlockCoords(pos);
        阻止位置 blockpos;
        阻止位置 blockpos1;

        for (blockpos = new 阻止位置(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ()); blockpos.getY() >= 0; blockpos = blockpos1)
        {
            blockpos1 = blockpos.down();
            Material material = chunk.getBlock(blockpos1).getMaterial();

            if (material.blocksMovement() && material != Material.leaves)
            {
                break;
            }
        }

        return blockpos;
    }

    public float getStarBrightness(float partialTicks)
    {
        float f = this.getCelestialAngle(partialTicks);
        float f1 = 1.0F - (MathHelper.cos(f * (float)Math.PI * 2.0F) * 2.0F + 0.25F);
        f1 = MathHelper.clamp_float(f1, 0.0F, 1.0F);
        return f1 * f1 * 0.5F;
    }

    public void scheduleUpdate(阻止位置 pos, Block blockIn, int delay)
    {
    }

    public void updateBlockTick(阻止位置 pos, Block blockIn, int delay, int priority)
    {
    }

    public void scheduleBlockUpdate(阻止位置 pos, Block blockIn, int delay, int priority)
    {
    }

    public void updateEntities()
    {
        this.theProfiler.startSection("entities");
        this.theProfiler.startSection("global");

        for (int i = 0; i < this.weatherEffects.size(); ++i)
        {
            实体 实体 = (实体)this.weatherEffects.get(i);

            try
            {
                ++实体.已存在的刻度;
                实体.onUpdate();
            }
            catch (Throwable throwable2)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable2, "Ticking entity");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being ticked");

                if (实体 == null)
                {
                    crashreportcategory.addCrashSection("Entity", "~~NULL~~");
                }
                else
                {
                    实体.addEntityCrashInfo(crashreportcategory);
                }

                throw new ReportedException(crashreport);
            }

            if (实体.isDead)
            {
                this.weatherEffects.remove(i--);
            }
        }

        this.theProfiler.endStartSection("remove");
        this.loaded实体List.removeAll(this.unloaded实体List);

        for (int k = 0; k < this.unloaded实体List.size(); ++k)
        {
            实体 实体1 = (实体)this.unloaded实体List.get(k);
            int j = 实体1.chunkCoordX;
            int l1 = 实体1.chunkCoordZ;

            if (实体1.addedToChunk && this.isChunkLoaded(j, l1, true))
            {
                this.getChunkFromChunkCoords(j, l1).removeEntity(实体1);
            }
        }

        for (int l = 0; l < this.unloaded实体List.size(); ++l)
        {
            this.onEntityRemoved((实体)this.unloaded实体List.get(l));
        }

        this.unloaded实体List.clear();
        this.theProfiler.endStartSection("regular");

        for (int i1 = 0; i1 < this.loaded实体List.size(); ++i1)
        {
            实体 实体2 = (实体)this.loaded实体List.get(i1);

            if (实体2.riding实体 != null)
            {
                if (!实体2.riding实体.isDead && 实体2.riding实体.riddenBy实体 == 实体2)
                {
                    continue;
                }

                实体2.riding实体.riddenBy实体 = null;
                实体2.riding实体 = null;
            }

            this.theProfiler.startSection("tick");

            if (!实体2.isDead)
            {
                try
                {
                    this.updateEntity(实体2);
                }
                catch (Throwable throwable1)
                {
                    CrashReport crashreport1 = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                    CrashReportCategory crashreportcategory2 = crashreport1.makeCategory("Entity being ticked");
                    实体2.addEntityCrashInfo(crashreportcategory2);
                    throw new ReportedException(crashreport1);
                }
            }

            this.theProfiler.endSection();
            this.theProfiler.startSection("remove");

            if (实体2.isDead)
            {
                int k1 = 实体2.chunkCoordX;
                int i2 = 实体2.chunkCoordZ;

                if (实体2.addedToChunk && this.isChunkLoaded(k1, i2, true))
                {
                    this.getChunkFromChunkCoords(k1, i2).removeEntity(实体2);
                }

                this.loaded实体List.remove(i1--);
                this.onEntityRemoved(实体2);
            }

            this.theProfiler.endSection();
        }

        this.theProfiler.endStartSection("blockEntities");
        this.processingLoadedTiles = true;
        Iterator<TileEntity> iterator = this.tickableTileEntities.iterator();

        while (iterator.hasNext())
        {
            TileEntity tileentity = (TileEntity)iterator.next();

            if (!tileentity.isInvalid() && tileentity.hasWorldObj())
            {
                阻止位置 blockpos = tileentity.getPos();

                if (this.isBlockLoaded(blockpos) && this.worldBorder.contains(blockpos))
                {
                    try
                    {
                        ((ITickable)tileentity).update();
                    }
                    catch (Throwable throwable)
                    {
                        CrashReport crashreport2 = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                        CrashReportCategory crashreportcategory1 = crashreport2.makeCategory("Block entity being ticked");
                        tileentity.addInfoToCrashReport(crashreportcategory1);
                        throw new ReportedException(crashreport2);
                    }
                }
            }

            if (tileentity.isInvalid())
            {
                iterator.remove();
                this.loadedTileEntityList.remove(tileentity);

                if (this.isBlockLoaded(tileentity.getPos()))
                {
                    this.getChunkFromBlockCoords(tileentity.getPos()).removeTileEntity(tileentity.getPos());
                }
            }
        }

        this.processingLoadedTiles = false;

        if (!this.tileEntitiesToBeRemoved.isEmpty())
        {
            this.tickableTileEntities.removeAll(this.tileEntitiesToBeRemoved);
            this.loadedTileEntityList.removeAll(this.tileEntitiesToBeRemoved);
            this.tileEntitiesToBeRemoved.clear();
        }

        this.theProfiler.endStartSection("pendingBlockEntities");

        if (!this.addedTileEntityList.isEmpty())
        {
            for (int j1 = 0; j1 < this.addedTileEntityList.size(); ++j1)
            {
                TileEntity tileentity1 = (TileEntity)this.addedTileEntityList.get(j1);

                if (!tileentity1.isInvalid())
                {
                    if (!this.loadedTileEntityList.contains(tileentity1))
                    {
                        this.addTileEntity(tileentity1);
                    }

                    if (this.isBlockLoaded(tileentity1.getPos()))
                    {
                        this.getChunkFromBlockCoords(tileentity1.getPos()).addTileEntity(tileentity1.getPos(), tileentity1);
                    }

                    this.markBlockForUpdate(tileentity1.getPos());
                }
            }

            this.addedTileEntityList.clear();
        }

        this.theProfiler.endSection();
        this.theProfiler.endSection();
    }

    public boolean addTileEntity(TileEntity tile)
    {
        boolean flag = this.loadedTileEntityList.add(tile);

        if (flag && tile instanceof ITickable)
        {
            this.tickableTileEntities.add(tile);
        }

        return flag;
    }

    public void addTileEntities(Collection<TileEntity> tileEntityCollection)
    {
        if (this.processingLoadedTiles)
        {
            this.addedTileEntityList.addAll(tileEntityCollection);
        }
        else
        {
            for (TileEntity tileentity : tileEntityCollection)
            {
                this.loadedTileEntityList.add(tileentity);

                if (tileentity instanceof ITickable)
                {
                    this.tickableTileEntities.add(tileentity);
                }
            }
        }
    }

    public void updateEntity(实体 ent)
    {
        this.updateEntityWithOptionalForce(ent, true);
    }

    public void updateEntityWithOptionalForce(实体 实体In, boolean forceUpdate)
    {
        int i = MathHelper.floor_double(实体In.X坐标);
        int j = MathHelper.floor_double(实体In.Z坐标);
        int k = 32;

        if (!forceUpdate || this.isAreaLoaded(i - k, 0, j - k, i + k, 0, j + k, true))
        {
            实体In.lastTickPosX = 实体In.X坐标;
            实体In.lastTickPosY = 实体In.Y坐标;
            实体In.lastTickPosZ = 实体In.Z坐标;
            实体In.prevRotationYaw = 实体In.旋转侧滑;
            实体In.prevRotationPitch = 实体In.rotationPitch;

            if (forceUpdate && 实体In.addedToChunk)
            {
                ++实体In.已存在的刻度;

                if (实体In.riding实体 != null)
                {
                    实体In.updateRidden();
                }
                else
                {
                    实体In.onUpdate();
                }
            }

            this.theProfiler.startSection("chunkCheck");

            if (Double.isNaN(实体In.X坐标) || Double.isInfinite(实体In.X坐标))
            {
                实体In.X坐标 = 实体In.lastTickPosX;
            }

            if (Double.isNaN(实体In.Y坐标) || Double.isInfinite(实体In.Y坐标))
            {
                实体In.Y坐标 = 实体In.lastTickPosY;
            }

            if (Double.isNaN(实体In.Z坐标) || Double.isInfinite(实体In.Z坐标))
            {
                实体In.Z坐标 = 实体In.lastTickPosZ;
            }

            if (Double.isNaN((double) 实体In.rotationPitch) || Double.isInfinite((double) 实体In.rotationPitch))
            {
                实体In.rotationPitch = 实体In.prevRotationPitch;
            }

            if (Double.isNaN((double) 实体In.旋转侧滑) || Double.isInfinite((double) 实体In.旋转侧滑))
            {
                实体In.旋转侧滑 = 实体In.prevRotationYaw;
            }

            int l = MathHelper.floor_double(实体In.X坐标 / 16.0D);
            int i1 = MathHelper.floor_double(实体In.Y坐标 / 16.0D);
            int j1 = MathHelper.floor_double(实体In.Z坐标 / 16.0D);

            if (!实体In.addedToChunk || 实体In.chunkCoordX != l || 实体In.chunkCoordY != i1 || 实体In.chunkCoordZ != j1)
            {
                if (实体In.addedToChunk && this.isChunkLoaded(实体In.chunkCoordX, 实体In.chunkCoordZ, true))
                {
                    this.getChunkFromChunkCoords(实体In.chunkCoordX, 实体In.chunkCoordZ).removeEntityAtIndex(实体In, 实体In.chunkCoordY);
                }

                if (this.isChunkLoaded(l, j1, true))
                {
                    实体In.addedToChunk = true;
                    this.getChunkFromChunkCoords(l, j1).addEntity(实体In);
                }
                else
                {
                    实体In.addedToChunk = false;
                }
            }

            this.theProfiler.endSection();

            if (forceUpdate && 实体In.addedToChunk && 实体In.riddenBy实体 != null)
            {
                if (!实体In.riddenBy实体.isDead && 实体In.riddenBy实体.riding实体 == 实体In)
                {
                    this.updateEntity(实体In.riddenBy实体);
                }
                else
                {
                    实体In.riddenBy实体.riding实体 = null;
                    实体In.riddenBy实体 = null;
                }
            }
        }
    }

    public boolean checkNoEntityCollision(AxisAlignedBB bb)
    {
        return this.checkNoEntityCollision(bb, (实体)null);
    }

    public boolean checkNoEntityCollision(AxisAlignedBB bb, 实体 实体In)
    {
        List<实体> list = this.getEntitiesWithinAABBExcludingEntity((实体)null, bb);

        for (int i = 0; i < list.size(); ++i)
        {
            实体 实体 = (实体)list.get(i);

            if (!实体.isDead && 实体.preventEntitySpawning && 实体 != 实体In && (实体In == null || 实体In.riding实体 != 实体 && 实体In.riddenBy实体 != 实体))
            {
                return false;
            }
        }

        return true;
    }

    public boolean checkBlockCollision(AxisAlignedBB bb)
    {
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ);
        阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

        for (int k1 = i; k1 <= j; ++k1)
        {
            for (int l1 = k; l1 <= l; ++l1)
            {
                for (int i2 = i1; i2 <= j1; ++i2)
                {
                    Block block = this.getBlockState(blockpos$mutableblockpos.set(k1, l1, i2)).getBlock();

                    if (block.getMaterial() != Material.air)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isAnyLiquid(AxisAlignedBB bb)
    {
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ);
        阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

        for (int k1 = i; k1 <= j; ++k1)
        {
            for (int l1 = k; l1 <= l; ++l1)
            {
                for (int i2 = i1; i2 <= j1; ++i2)
                {
                    Block block = this.getBlockState(blockpos$mutableblockpos.set(k1, l1, i2)).getBlock();

                    if (block.getMaterial().isLiquid())
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isFlammableWithin(AxisAlignedBB bb)
    {
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX + 1.0D);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY + 1.0D);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ + 1.0D);

        if (this.isAreaLoaded(i, k, i1, j, l, j1, true))
        {
            阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

            for (int k1 = i; k1 < j; ++k1)
            {
                for (int l1 = k; l1 < l; ++l1)
                {
                    for (int i2 = i1; i2 < j1; ++i2)
                    {
                        Block block = this.getBlockState(blockpos$mutableblockpos.set(k1, l1, i2)).getBlock();

                        if (block == Blocks.fire || block == Blocks.flowing_lava || block == Blocks.lava)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean handleMaterialAcceleration(AxisAlignedBB bb, Material materialIn, 实体 实体In)
    {
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX + 1.0D);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY + 1.0D);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ + 1.0D);

        if (!this.isAreaLoaded(i, k, i1, j, l, j1, true))
        {
            return false;
        }
        else
        {
            boolean flag = false;
            Vec3 vec3 = new Vec3(0.0D, 0.0D, 0.0D);
            阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

            for (int k1 = i; k1 < j; ++k1)
            {
                for (int l1 = k; l1 < l; ++l1)
                {
                    for (int i2 = i1; i2 < j1; ++i2)
                    {
                        blockpos$mutableblockpos.set(k1, l1, i2);
                        IBlockState iblockstate = this.getBlockState(blockpos$mutableblockpos);
                        Block block = iblockstate.getBlock();

                        if (block.getMaterial() == materialIn)
                        {
                            double d0 = (double)((float)(l1 + 1) - BlockLiquid.getLiquidHeightPercent(((Integer)iblockstate.getValue(BlockLiquid.LEVEL)).intValue()));

                            if ((double)l >= d0)
                            {
                                flag = true;
                                vec3 = block.modifyAcceleration(this, blockpos$mutableblockpos, 实体In, vec3);
                            }
                        }
                    }
                }
            }

            if (vec3.lengthVector() > 0.0D && 实体In.isPushedByWater())
            {
                vec3 = vec3.normalize();
                double d1 = 0.014D;
                实体In.通便X += vec3.xCoord * d1;
                实体In.motionY += vec3.yCoord * d1;
                实体In.通便Z += vec3.zCoord * d1;
            }

            return flag;
        }
    }

    public boolean isMaterialInBB(AxisAlignedBB bb, Material materialIn)
    {
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX + 1.0D);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY + 1.0D);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ + 1.0D);
        阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = k; l1 < l; ++l1)
            {
                for (int i2 = i1; i2 < j1; ++i2)
                {
                    if (this.getBlockState(blockpos$mutableblockpos.set(k1, l1, i2)).getBlock().getMaterial() == materialIn)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isAABBInMaterial(AxisAlignedBB bb, Material materialIn)
    {
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX + 1.0D);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY + 1.0D);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ + 1.0D);
        阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

        for (int k1 = i; k1 < j; ++k1)
        {
            for (int l1 = k; l1 < l; ++l1)
            {
                for (int i2 = i1; i2 < j1; ++i2)
                {
                    IBlockState iblockstate = this.getBlockState(blockpos$mutableblockpos.set(k1, l1, i2));
                    Block block = iblockstate.getBlock();

                    if (block.getMaterial() == materialIn)
                    {
                        int j2 = ((Integer)iblockstate.getValue(BlockLiquid.LEVEL)).intValue();
                        double d0 = (double)(l1 + 1);

                        if (j2 < 8)
                        {
                            d0 = (double)(l1 + 1) - (double)j2 / 8.0D;
                        }

                        if (d0 >= bb.minY)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public Explosion createExplosion(实体 实体In, double x, double y, double z, float strength, boolean isSmoking)
    {
        return this.newExplosion(实体In, x, y, z, strength, false, isSmoking);
    }

    public Explosion newExplosion(实体 实体In, double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking)
    {
        Explosion explosion = new Explosion(this, 实体In, x, y, z, strength, isFlaming, isSmoking);
        explosion.doExplosionA();
        explosion.doExplosionB(true);
        return explosion;
    }

    public float getBlockDensity(Vec3 vec, AxisAlignedBB bb)
    {
        double d0 = 1.0D / ((bb.maxX - bb.minX) * 2.0D + 1.0D);
        double d1 = 1.0D / ((bb.maxY - bb.minY) * 2.0D + 1.0D);
        double d2 = 1.0D / ((bb.maxZ - bb.minZ) * 2.0D + 1.0D);
        double d3 = (1.0D - Math.floor(1.0D / d0) * d0) / 2.0D;
        double d4 = (1.0D - Math.floor(1.0D / d2) * d2) / 2.0D;

        if (d0 >= 0.0D && d1 >= 0.0D && d2 >= 0.0D)
        {
            int i = 0;
            int j = 0;

            for (float f = 0.0F; f <= 1.0F; f = (float)((double)f + d0))
            {
                for (float f1 = 0.0F; f1 <= 1.0F; f1 = (float)((double)f1 + d1))
                {
                    for (float f2 = 0.0F; f2 <= 1.0F; f2 = (float)((double)f2 + d2))
                    {
                        double d5 = bb.minX + (bb.maxX - bb.minX) * (double)f;
                        double d6 = bb.minY + (bb.maxY - bb.minY) * (double)f1;
                        double d7 = bb.minZ + (bb.maxZ - bb.minZ) * (double)f2;

                        if (this.rayTraceBlocks(new Vec3(d5 + d3, d6, d7 + d4), vec) == null)
                        {
                            ++i;
                        }

                        ++j;
                    }
                }
            }

            return (float)i / (float)j;
        }
        else
        {
            return 0.0F;
        }
    }

    public boolean extinguishFire(实体Player player, 阻止位置 pos, EnumFacing side)
    {
        pos = pos.offset(side);

        if (this.getBlockState(pos).getBlock() == Blocks.fire)
        {
            this.playAuxSFXAtEntity(player, 1004, pos, 0);
            this.setBlockToAir(pos);
            return true;
        }
        else
        {
            return false;
        }
    }

    public String getDebugLoadedEntities()
    {
        return "All: " + this.loaded实体List.size();
    }

    public String getProviderName()
    {
        return this.chunkProvider.makeString();
    }

    public TileEntity getTileEntity(阻止位置 pos)
    {
        if (!this.isValid(pos))
        {
            return null;
        }
        else
        {
            TileEntity tileentity = null;

            if (this.processingLoadedTiles)
            {
                for (int i = 0; i < this.addedTileEntityList.size(); ++i)
                {
                    TileEntity tileentity1 = (TileEntity)this.addedTileEntityList.get(i);

                    if (!tileentity1.isInvalid() && tileentity1.getPos().equals(pos))
                    {
                        tileentity = tileentity1;
                        break;
                    }
                }
            }

            if (tileentity == null)
            {
                tileentity = this.getChunkFromBlockCoords(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.IMMEDIATE);
            }

            if (tileentity == null)
            {
                for (int j = 0; j < this.addedTileEntityList.size(); ++j)
                {
                    TileEntity tileentity2 = (TileEntity)this.addedTileEntityList.get(j);

                    if (!tileentity2.isInvalid() && tileentity2.getPos().equals(pos))
                    {
                        tileentity = tileentity2;
                        break;
                    }
                }
            }

            return tileentity;
        }
    }

    public void setTileEntity(阻止位置 pos, TileEntity tileEntityIn)
    {
        if (tileEntityIn != null && !tileEntityIn.isInvalid())
        {
            if (this.processingLoadedTiles)
            {
                tileEntityIn.setPos(pos);
                Iterator<TileEntity> iterator = this.addedTileEntityList.iterator();

                while (iterator.hasNext())
                {
                    TileEntity tileentity = (TileEntity)iterator.next();

                    if (tileentity.getPos().equals(pos))
                    {
                        tileentity.invalidate();
                        iterator.remove();
                    }
                }

                this.addedTileEntityList.add(tileEntityIn);
            }
            else
            {
                this.addTileEntity(tileEntityIn);
                this.getChunkFromBlockCoords(pos).addTileEntity(pos, tileEntityIn);
            }
        }
    }

    public void removeTileEntity(阻止位置 pos)
    {
        TileEntity tileentity = this.getTileEntity(pos);

        if (tileentity != null && this.processingLoadedTiles)
        {
            tileentity.invalidate();
            this.addedTileEntityList.remove(tileentity);
        }
        else
        {
            if (tileentity != null)
            {
                this.addedTileEntityList.remove(tileentity);
                this.loadedTileEntityList.remove(tileentity);
                this.tickableTileEntities.remove(tileentity);
            }

            this.getChunkFromBlockCoords(pos).removeTileEntity(pos);
        }
    }

    public void markTileEntityForRemoval(TileEntity tileEntityIn)
    {
        this.tileEntitiesToBeRemoved.add(tileEntityIn);
    }

    public boolean isBlockFullCube(阻止位置 pos)
    {
        IBlockState iblockstate = this.getBlockState(pos);
        AxisAlignedBB axisalignedbb = iblockstate.getBlock().getCollisionBoundingBox(this, pos, iblockstate);
        return axisalignedbb != null && axisalignedbb.getAverageEdgeLength() >= 1.0D;
    }

    public static boolean doesBlockHaveSolidTopSurface(IBlockAccess blockAccess, 阻止位置 pos)
    {
        IBlockState iblockstate = blockAccess.getBlockState(pos);
        Block block = iblockstate.getBlock();
        return block.getMaterial().isOpaque() && block.isFullCube() ? true : (block instanceof BlockStairs ? iblockstate.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP : (block instanceof BlockSlab ? iblockstate.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP : (block instanceof BlockHopper ? true : (block instanceof BlockSnow ? ((Integer)iblockstate.getValue(BlockSnow.LAYERS)).intValue() == 7 : false))));
    }

    public boolean isBlockNormalCube(阻止位置 pos, boolean _default)
    {
        if (!this.isValid(pos))
        {
            return _default;
        }
        else
        {
            Chunk chunk = this.chunkProvider.provideChunk(pos);

            if (chunk.isEmpty())
            {
                return _default;
            }
            else
            {
                Block block = this.getBlockState(pos).getBlock();
                return block.getMaterial().isOpaque() && block.isFullCube();
            }
        }
    }

    public void calculateInitialSkylight()
    {
        int i = this.calculateSkylightSubtracted(1.0F);

        if (i != this.skylightSubtracted)
        {
            this.skylightSubtracted = i;
        }
    }

    public void setAllowedSpawnTypes(boolean hostile, boolean peaceful)
    {
        this.spawnHostileMobs = hostile;
        this.spawnPeacefulMobs = peaceful;
    }

    public void tick()
    {
        this.updateWeather();
    }

    protected void calculateInitialWeather()
    {
        if (this.worldInfo.isRaining())
        {
            this.rainingStrength = 1.0F;

            if (this.worldInfo.isThundering())
            {
                this.thunderingStrength = 1.0F;
            }
        }
    }

    protected void updateWeather()
    {
        if (!this.provider.getHasNoSky())
        {
            if (!this.isRemote)
            {
                int i = this.worldInfo.getCleanWeatherTime();

                if (i > 0)
                {
                    --i;
                    this.worldInfo.setCleanWeatherTime(i);
                    this.worldInfo.setThunderTime(this.worldInfo.isThundering() ? 1 : 2);
                    this.worldInfo.setRainTime(this.worldInfo.isRaining() ? 1 : 2);
                }

                int j = this.worldInfo.getThunderTime();

                if (j <= 0)
                {
                    if (this.worldInfo.isThundering())
                    {
                        this.worldInfo.setThunderTime(this.rand.nextInt(12000) + 3600);
                    }
                    else
                    {
                        this.worldInfo.setThunderTime(this.rand.nextInt(168000) + 12000);
                    }
                }
                else
                {
                    --j;
                    this.worldInfo.setThunderTime(j);

                    if (j <= 0)
                    {
                        this.worldInfo.setThundering(!this.worldInfo.isThundering());
                    }
                }

                this.prevThunderingStrength = this.thunderingStrength;

                if (this.worldInfo.isThundering())
                {
                    this.thunderingStrength = (float)((double)this.thunderingStrength + 0.01D);
                }
                else
                {
                    this.thunderingStrength = (float)((double)this.thunderingStrength - 0.01D);
                }

                this.thunderingStrength = MathHelper.clamp_float(this.thunderingStrength, 0.0F, 1.0F);
                int k = this.worldInfo.getRainTime();

                if (k <= 0)
                {
                    if (this.worldInfo.isRaining())
                    {
                        this.worldInfo.setRainTime(this.rand.nextInt(12000) + 12000);
                    }
                    else
                    {
                        this.worldInfo.setRainTime(this.rand.nextInt(168000) + 12000);
                    }
                }
                else
                {
                    --k;
                    this.worldInfo.setRainTime(k);

                    if (k <= 0)
                    {
                        this.worldInfo.setRaining(!this.worldInfo.isRaining());
                    }
                }

                this.prevRainingStrength = this.rainingStrength;

                if (this.worldInfo.isRaining())
                {
                    this.rainingStrength = (float)((double)this.rainingStrength + 0.01D);
                }
                else
                {
                    this.rainingStrength = (float)((double)this.rainingStrength - 0.01D);
                }

                this.rainingStrength = MathHelper.clamp_float(this.rainingStrength, 0.0F, 1.0F);
            }
        }
    }

    protected void setActivePlayerChunksAndCheckLight()
    {
        this.activeChunkSet.clear();
        this.theProfiler.startSection("buildList");

        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            实体Player entityplayer = (实体Player)this.playerEntities.get(i);
            int j = MathHelper.floor_double(entityplayer.X坐标 / 16.0D);
            int k = MathHelper.floor_double(entityplayer.Z坐标 / 16.0D);
            int l = this.getRenderDistanceChunks();

            for (int i1 = -l; i1 <= l; ++i1)
            {
                for (int j1 = -l; j1 <= l; ++j1)
                {
                    this.activeChunkSet.add(new ChunkCoordIntPair(i1 + j, j1 + k));
                }
            }
        }

        this.theProfiler.endSection();

        if (this.ambientTickCountdown > 0)
        {
            --this.ambientTickCountdown;
        }

        this.theProfiler.startSection("playerCheckLight");

        if (!this.playerEntities.isEmpty())
        {
            int k1 = this.rand.nextInt(this.playerEntities.size());
            实体Player entityplayer1 = (实体Player)this.playerEntities.get(k1);
            int l1 = MathHelper.floor_double(entityplayer1.X坐标) + this.rand.nextInt(11) - 5;
            int i2 = MathHelper.floor_double(entityplayer1.Y坐标) + this.rand.nextInt(11) - 5;
            int j2 = MathHelper.floor_double(entityplayer1.Z坐标) + this.rand.nextInt(11) - 5;
            this.checkLight(new 阻止位置(l1, i2, j2));
        }

        this.theProfiler.endSection();
    }

    protected abstract int getRenderDistanceChunks();

    protected void playMoodSoundAndCheckLight(int p_147467_1_, int p_147467_2_, Chunk chunkIn)
    {
        this.theProfiler.endStartSection("moodSound");

        if (this.ambientTickCountdown == 0 && !this.isRemote)
        {
            this.updateLCG = this.updateLCG * 3 + 1013904223;
            int i = this.updateLCG >> 2;
            int j = i & 15;
            int k = i >> 8 & 15;
            int l = i >> 16 & 255;
            阻止位置 blockpos = new 阻止位置(j, l, k);
            Block block = chunkIn.getBlock(blockpos);
            j = j + p_147467_1_;
            k = k + p_147467_2_;

            if (block.getMaterial() == Material.air && this.getLight(blockpos) <= this.rand.nextInt(8) && this.getLightFor(EnumSkyBlock.SKY, blockpos) <= 0)
            {
                实体Player entityplayer = this.getClosestPlayer((double)j + 0.5D, (double)l + 0.5D, (double)k + 0.5D, 8.0D);

                if (entityplayer != null && entityplayer.getDistanceSq((double)j + 0.5D, (double)l + 0.5D, (double)k + 0.5D) > 4.0D)
                {
                    this.playSoundEffect((double)j + 0.5D, (double)l + 0.5D, (double)k + 0.5D, "ambient.cave.cave", 0.7F, 0.8F + this.rand.nextFloat() * 0.2F);
                    this.ambientTickCountdown = this.rand.nextInt(12000) + 6000;
                }
            }
        }

        this.theProfiler.endStartSection("checkLight");
        chunkIn.enqueueRelightChecks();
    }

    protected void updateBlocks()
    {
        this.setActivePlayerChunksAndCheckLight();
    }

    public void forceBlockUpdateTick(Block blockType, 阻止位置 pos, Random random)
    {
        this.scheduledUpdatesAreImmediate = true;
        blockType.updateTick(this, pos, this.getBlockState(pos), random);
        this.scheduledUpdatesAreImmediate = false;
    }

    public boolean canBlockFreezeWater(阻止位置 pos)
    {
        return this.canBlockFreeze(pos, false);
    }

    public boolean canBlockFreezeNoWater(阻止位置 pos)
    {
        return this.canBlockFreeze(pos, true);
    }

    public boolean canBlockFreeze(阻止位置 pos, boolean noWaterAdj)
    {
        BiomeGenBase biomegenbase = this.getBiomeGenForCoords(pos);
        float f = biomegenbase.getFloatTemperature(pos);

        if (f > 0.15F)
        {
            return false;
        }
        else
        {
            if (pos.getY() >= 0 && pos.getY() < 256 && this.getLightFor(EnumSkyBlock.BLOCK, pos) < 10)
            {
                IBlockState iblockstate = this.getBlockState(pos);
                Block block = iblockstate.getBlock();

                if ((block == Blocks.water || block == Blocks.flowing_water) && ((Integer)iblockstate.getValue(BlockLiquid.LEVEL)).intValue() == 0)
                {
                    if (!noWaterAdj)
                    {
                        return true;
                    }

                    boolean flag = this.isWater(pos.west()) && this.isWater(pos.east()) && this.isWater(pos.north()) && this.isWater(pos.south());

                    if (!flag)
                    {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private boolean isWater(阻止位置 pos)
    {
        return this.getBlockState(pos).getBlock().getMaterial() == Material.water;
    }

    public boolean canSnowAt(阻止位置 pos, boolean checkLight)
    {
        BiomeGenBase biomegenbase = this.getBiomeGenForCoords(pos);
        float f = biomegenbase.getFloatTemperature(pos);

        if (f > 0.15F)
        {
            return false;
        }
        else if (!checkLight)
        {
            return true;
        }
        else
        {
            if (pos.getY() >= 0 && pos.getY() < 256 && this.getLightFor(EnumSkyBlock.BLOCK, pos) < 10)
            {
                Block block = this.getBlockState(pos).getBlock();

                if (block.getMaterial() == Material.air && Blocks.snow_layer.canPlaceBlockAt(this, pos))
                {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean checkLight(阻止位置 pos)
    {
        boolean flag = false;

        if (!this.provider.getHasNoSky())
        {
            flag |= this.checkLightFor(EnumSkyBlock.SKY, pos);
        }

        flag = flag | this.checkLightFor(EnumSkyBlock.BLOCK, pos);
        return flag;
    }

    private int getRawLight(阻止位置 pos, EnumSkyBlock lightType)
    {
        if (lightType == EnumSkyBlock.SKY && this.canSeeSky(pos))
        {
            return 15;
        }
        else
        {
            Block block = this.getBlockState(pos).getBlock();
            int i = lightType == EnumSkyBlock.SKY ? 0 : block.getLightValue();
            int j = block.getLightOpacity();

            if (j >= 15 && block.getLightValue() > 0)
            {
                j = 1;
            }

            if (j < 1)
            {
                j = 1;
            }

            if (j >= 15)
            {
                return 0;
            }
            else if (i >= 14)
            {
                return i;
            }
            else
            {
                for (EnumFacing enumfacing : EnumFacing.values())
                {
                    阻止位置 blockpos = pos.offset(enumfacing);
                    int k = this.getLightFor(lightType, blockpos) - j;

                    if (k > i)
                    {
                        i = k;
                    }

                    if (i >= 14)
                    {
                        return i;
                    }
                }

                return i;
            }
        }
    }

    public boolean checkLightFor(EnumSkyBlock lightType, 阻止位置 pos)
    {
        if (!this.isAreaLoaded(pos, 17, false))
        {
            return false;
        }
        else
        {
            int i = 0;
            int j = 0;
            this.theProfiler.startSection("getBrightness");
            int k = this.getLightFor(lightType, pos);
            int l = this.getRawLight(pos, lightType);
            int i1 = pos.getX();
            int j1 = pos.getY();
            int k1 = pos.getZ();

            if (l > k)
            {
                this.lightUpdateBlockList[j++] = 133152;
            }
            else if (l < k)
            {
                this.lightUpdateBlockList[j++] = 133152 | k << 18;

                while (i < j)
                {
                    int l1 = this.lightUpdateBlockList[i++];
                    int i2 = (l1 & 63) - 32 + i1;
                    int j2 = (l1 >> 6 & 63) - 32 + j1;
                    int k2 = (l1 >> 12 & 63) - 32 + k1;
                    int l2 = l1 >> 18 & 15;
                    阻止位置 blockpos = new 阻止位置(i2, j2, k2);
                    int i3 = this.getLightFor(lightType, blockpos);

                    if (i3 == l2)
                    {
                        this.setLightFor(lightType, blockpos, 0);

                        if (l2 > 0)
                        {
                            int j3 = MathHelper.abs_int(i2 - i1);
                            int k3 = MathHelper.abs_int(j2 - j1);
                            int l3 = MathHelper.abs_int(k2 - k1);

                            if (j3 + k3 + l3 < 17)
                            {
                                阻止位置.Mutable阻止位置 blockpos$mutableblockpos = new 阻止位置.Mutable阻止位置();

                                for (EnumFacing enumfacing : EnumFacing.values())
                                {
                                    int i4 = i2 + enumfacing.getFrontOffsetX();
                                    int j4 = j2 + enumfacing.getFrontOffsetY();
                                    int k4 = k2 + enumfacing.getFrontOffsetZ();
                                    blockpos$mutableblockpos.set(i4, j4, k4);
                                    int l4 = Math.max(1, this.getBlockState(blockpos$mutableblockpos).getBlock().getLightOpacity());
                                    i3 = this.getLightFor(lightType, blockpos$mutableblockpos);

                                    if (i3 == l2 - l4 && j < this.lightUpdateBlockList.length)
                                    {
                                        this.lightUpdateBlockList[j++] = i4 - i1 + 32 | j4 - j1 + 32 << 6 | k4 - k1 + 32 << 12 | l2 - l4 << 18;
                                    }
                                }
                            }
                        }
                    }
                }

                i = 0;
            }

            this.theProfiler.endSection();
            this.theProfiler.startSection("checkedPosition < toCheckCount");

            while (i < j)
            {
                int i5 = this.lightUpdateBlockList[i++];
                int j5 = (i5 & 63) - 32 + i1;
                int k5 = (i5 >> 6 & 63) - 32 + j1;
                int l5 = (i5 >> 12 & 63) - 32 + k1;
                阻止位置 blockpos1 = new 阻止位置(j5, k5, l5);
                int i6 = this.getLightFor(lightType, blockpos1);
                int j6 = this.getRawLight(blockpos1, lightType);

                if (j6 != i6)
                {
                    this.setLightFor(lightType, blockpos1, j6);

                    if (j6 > i6)
                    {
                        int k6 = Math.abs(j5 - i1);
                        int l6 = Math.abs(k5 - j1);
                        int i7 = Math.abs(l5 - k1);
                        boolean flag = j < this.lightUpdateBlockList.length - 6;

                        if (k6 + l6 + i7 < 17 && flag)
                        {
                            if (this.getLightFor(lightType, blockpos1.west()) < j6)
                            {
                                this.lightUpdateBlockList[j++] = j5 - 1 - i1 + 32 + (k5 - j1 + 32 << 6) + (l5 - k1 + 32 << 12);
                            }

                            if (this.getLightFor(lightType, blockpos1.east()) < j6)
                            {
                                this.lightUpdateBlockList[j++] = j5 + 1 - i1 + 32 + (k5 - j1 + 32 << 6) + (l5 - k1 + 32 << 12);
                            }

                            if (this.getLightFor(lightType, blockpos1.down()) < j6)
                            {
                                this.lightUpdateBlockList[j++] = j5 - i1 + 32 + (k5 - 1 - j1 + 32 << 6) + (l5 - k1 + 32 << 12);
                            }

                            if (this.getLightFor(lightType, blockpos1.up()) < j6)
                            {
                                this.lightUpdateBlockList[j++] = j5 - i1 + 32 + (k5 + 1 - j1 + 32 << 6) + (l5 - k1 + 32 << 12);
                            }

                            if (this.getLightFor(lightType, blockpos1.north()) < j6)
                            {
                                this.lightUpdateBlockList[j++] = j5 - i1 + 32 + (k5 - j1 + 32 << 6) + (l5 - 1 - k1 + 32 << 12);
                            }

                            if (this.getLightFor(lightType, blockpos1.south()) < j6)
                            {
                                this.lightUpdateBlockList[j++] = j5 - i1 + 32 + (k5 - j1 + 32 << 6) + (l5 + 1 - k1 + 32 << 12);
                            }
                        }
                    }
                }
            }

            this.theProfiler.endSection();
            return true;
        }
    }

    public boolean tickUpdates(boolean p_72955_1_)
    {
        return false;
    }

    public List<NextTickListEntry> getPendingBlockUpdates(Chunk chunkIn, boolean p_72920_2_)
    {
        return null;
    }

    public List<NextTickListEntry> func_175712_a(StructureBoundingBox structureBB, boolean p_175712_2_)
    {
        return null;
    }

    public List<实体> getEntitiesWithinAABBExcludingEntity(实体 实体In, AxisAlignedBB bb)
    {
        return this.getEntitiesInAABBexcluding(实体In, bb, EntitySelectors.NOT_SPECTATING);
    }

    public List<实体> getEntitiesInAABBexcluding(实体 实体In, AxisAlignedBB boundingBox, Predicate <? super 实体> predicate)
    {
        List<实体> list = Lists.<实体>newArrayList();
        int i = MathHelper.floor_double((boundingBox.minX - 2.0D) / 16.0D);
        int j = MathHelper.floor_double((boundingBox.maxX + 2.0D) / 16.0D);
        int k = MathHelper.floor_double((boundingBox.minZ - 2.0D) / 16.0D);
        int l = MathHelper.floor_double((boundingBox.maxZ + 2.0D) / 16.0D);

        for (int i1 = i; i1 <= j; ++i1)
        {
            for (int j1 = k; j1 <= l; ++j1)
            {
                if (this.isChunkLoaded(i1, j1, true))
                {
                    this.getChunkFromChunkCoords(i1, j1).getEntitiesWithinAABBForEntity(实体In, boundingBox, list, predicate);
                }
            }
        }

        return list;
    }

    public <T extends 实体> List<T> getEntities(Class <? extends T > entityType, Predicate <? super T > filter)
    {
        List<T> list = Lists.<T>newArrayList();

        for (实体 实体 : this.loaded实体List)
        {
            if (entityType.isAssignableFrom(实体.getClass()) && filter.apply((T) 实体))
            {
                list.add((T) 实体);
            }
        }

        return list;
    }

    public <T extends 实体> List<T> getPlayers(Class <? extends T > playerType, Predicate <? super T > filter)
    {
        List<T> list = Lists.<T>newArrayList();

        for (实体 实体 : this.playerEntities)
        {
            if (playerType.isAssignableFrom(实体.getClass()) && filter.apply((T) 实体))
            {
                list.add((T) 实体);
            }
        }

        return list;
    }

    public <T extends 实体> List<T> getEntitiesWithinAABB(Class <? extends T > classEntity, AxisAlignedBB bb)
    {
        return this.<T>getEntitiesWithinAABB(classEntity, bb, EntitySelectors.NOT_SPECTATING);
    }

    public <T extends 实体> List<T> getEntitiesWithinAABB(Class <? extends T > clazz, AxisAlignedBB aabb, Predicate <? super T > filter)
    {
        int i = MathHelper.floor_double((aabb.minX - 2.0D) / 16.0D);
        int j = MathHelper.floor_double((aabb.maxX + 2.0D) / 16.0D);
        int k = MathHelper.floor_double((aabb.minZ - 2.0D) / 16.0D);
        int l = MathHelper.floor_double((aabb.maxZ + 2.0D) / 16.0D);
        List<T> list = Lists.<T>newArrayList();

        for (int i1 = i; i1 <= j; ++i1)
        {
            for (int j1 = k; j1 <= l; ++j1)
            {
                if (this.isChunkLoaded(i1, j1, true))
                {
                    this.getChunkFromChunkCoords(i1, j1).getEntitiesOfTypeWithinAAAB(clazz, aabb, list, filter);
                }
            }
        }

        return list;
    }

    public <T extends 实体> T findNearestEntityWithinAABB(Class <? extends T > entityType, AxisAlignedBB aabb, T closestTo)
    {
        List<T> list = this.<T>getEntitiesWithinAABB(entityType, aabb);
        T t = null;
        double d0 = Double.MAX_VALUE;

        for (int i = 0; i < list.size(); ++i)
        {
            T t1 = list.get(i);

            if (t1 != closestTo && EntitySelectors.NOT_SPECTATING.apply(t1))
            {
                double d1 = closestTo.getDistanceSqToEntity(t1);

                if (d1 <= d0)
                {
                    t = t1;
                    d0 = d1;
                }
            }
        }

        return t;
    }

    public 实体 getEntityByID(int id)
    {
        return (实体)this.entitiesById.lookup(id);
    }

    public List<实体> getLoadedEntityList()
    {
        return this.loaded实体List;
    }

    public void markChunkDirty(阻止位置 pos, TileEntity unusedTileEntity)
    {
        if (this.isBlockLoaded(pos))
        {
            this.getChunkFromBlockCoords(pos).setChunkModified();
        }
    }

    public int countEntities(Class<?> entityType)
    {
        int i = 0;

        for (实体 实体 : this.loaded实体List)
        {
            if ((!(实体 instanceof 实体Living) || !((实体Living) 实体).isNoDespawnRequired()) && entityType.isAssignableFrom(实体.getClass()))
            {
                ++i;
            }
        }

        return i;
    }

    public void loadEntities(Collection<实体> 实体Collection)
    {
        this.loaded实体List.addAll(实体Collection);

        for (实体 实体 : 实体Collection)
        {
            this.onEntityAdded(实体);
        }
    }

    public void unloadEntities(Collection<实体> 实体Collection)
    {
        this.unloaded实体List.addAll(实体Collection);
    }

    public boolean canBlockBePlaced(Block blockIn, 阻止位置 pos, boolean p_175716_3_, EnumFacing side, 实体 实体In, ItemStack itemStackIn)
    {
        Block block = this.getBlockState(pos).getBlock();
        AxisAlignedBB axisalignedbb = p_175716_3_ ? null : blockIn.getCollisionBoundingBox(this, pos, blockIn.getDefaultState());
        return axisalignedbb != null && !this.checkNoEntityCollision(axisalignedbb, 实体In) ? false : (block.getMaterial() == Material.circuits && blockIn == Blocks.anvil ? true : block.getMaterial().isReplaceable() && blockIn.canReplace(this, pos, side, itemStackIn));
    }

    public int getSeaLevel()
    {
        return this.seaLevel;
    }

    public void setSeaLevel(int p_181544_1_)
    {
        this.seaLevel = p_181544_1_;
    }

    public int getStrongPower(阻止位置 pos, EnumFacing direction)
    {
        IBlockState iblockstate = this.getBlockState(pos);
        return iblockstate.getBlock().getStrongPower(this, pos, iblockstate, direction);
    }

    public WorldType getWorldType()
    {
        return this.worldInfo.getTerrainType();
    }

    public int getStrongPower(阻止位置 pos)
    {
        int i = 0;
        i = Math.max(i, this.getStrongPower(pos.down(), EnumFacing.DOWN));

        if (i >= 15)
        {
            return i;
        }
        else
        {
            i = Math.max(i, this.getStrongPower(pos.up(), EnumFacing.UP));

            if (i >= 15)
            {
                return i;
            }
            else
            {
                i = Math.max(i, this.getStrongPower(pos.north(), EnumFacing.NORTH));

                if (i >= 15)
                {
                    return i;
                }
                else
                {
                    i = Math.max(i, this.getStrongPower(pos.south(), EnumFacing.SOUTH));

                    if (i >= 15)
                    {
                        return i;
                    }
                    else
                    {
                        i = Math.max(i, this.getStrongPower(pos.west(), EnumFacing.WEST));

                        if (i >= 15)
                        {
                            return i;
                        }
                        else
                        {
                            i = Math.max(i, this.getStrongPower(pos.east(), EnumFacing.EAST));
                            return i >= 15 ? i : i;
                        }
                    }
                }
            }
        }
    }

    public boolean isSidePowered(阻止位置 pos, EnumFacing side)
    {
        return this.getRedstonePower(pos, side) > 0;
    }

    public int getRedstonePower(阻止位置 pos, EnumFacing facing)
    {
        IBlockState iblockstate = this.getBlockState(pos);
        Block block = iblockstate.getBlock();
        return block.isNormalCube() ? this.getStrongPower(pos) : block.getWeakPower(this, pos, iblockstate, facing);
    }

    public boolean isBlockPowered(阻止位置 pos)
    {
        return this.getRedstonePower(pos.down(), EnumFacing.DOWN) > 0 ? true : (this.getRedstonePower(pos.up(), EnumFacing.UP) > 0 ? true : (this.getRedstonePower(pos.north(), EnumFacing.NORTH) > 0 ? true : (this.getRedstonePower(pos.south(), EnumFacing.SOUTH) > 0 ? true : (this.getRedstonePower(pos.west(), EnumFacing.WEST) > 0 ? true : this.getRedstonePower(pos.east(), EnumFacing.EAST) > 0))));
    }

    public int isBlockIndirectlyGettingPowered(阻止位置 pos)
    {
        int i = 0;

        for (EnumFacing enumfacing : EnumFacing.values())
        {
            int j = this.getRedstonePower(pos.offset(enumfacing), enumfacing);

            if (j >= 15)
            {
                return 15;
            }

            if (j > i)
            {
                i = j;
            }
        }

        return i;
    }

    public 实体Player getClosestPlayerToEntity(实体 实体In, double distance)
    {
        return this.getClosestPlayer(实体In.X坐标, 实体In.Y坐标, 实体In.Z坐标, distance);
    }

    public 实体Player getClosestPlayer(double x, double y, double z, double distance)
    {
        double d0 = -1.0D;
        实体Player entityplayer = null;

        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            实体Player entityplayer1 = (实体Player)this.playerEntities.get(i);

            if (EntitySelectors.NOT_SPECTATING.apply(entityplayer1))
            {
                double d1 = entityplayer1.getDistanceSq(x, y, z);

                if ((distance < 0.0D || d1 < distance * distance) && (d0 == -1.0D || d1 < d0))
                {
                    d0 = d1;
                    entityplayer = entityplayer1;
                }
            }
        }

        return entityplayer;
    }

    public boolean isAnyPlayerWithinRangeAt(double x, double y, double z, double range)
    {
        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            实体Player entityplayer = (实体Player)this.playerEntities.get(i);

            if (EntitySelectors.NOT_SPECTATING.apply(entityplayer))
            {
                double d0 = entityplayer.getDistanceSq(x, y, z);

                if (range < 0.0D || d0 < range * range)
                {
                    return true;
                }
            }
        }

        return false;
    }

    public 实体Player getPlayerEntityByName(String name)
    {
        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            实体Player entityplayer = (实体Player)this.playerEntities.get(i);

            if (name.equals(entityplayer.getName()))
            {
                return entityplayer;
            }
        }

        return null;
    }

    public 实体Player getPlayerEntityByUUID(UUID uuid)
    {
        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            实体Player entityplayer = (实体Player)this.playerEntities.get(i);

            if (uuid.equals(entityplayer.getUniqueID()))
            {
                return entityplayer;
            }
        }

        return null;
    }

    public void sendQuittingDisconnectingPacket()
    {
    }

    public void checkSessionLock() throws MinecraftException
    {
        this.saveHandler.checkSessionLock();
    }

    public void setTotalWorldTime(long worldTime)
    {
        this.worldInfo.setWorldTotalTime(worldTime);
    }

    public long getSeed()
    {
        return this.worldInfo.getSeed();
    }

    public long getTotalWorldTime()
    {
        return this.worldInfo.getWorldTotalTime();
    }

    public long getWorldTime()
    {
        return this.worldInfo.getWorldTime();
    }

    public void setWorldTime(long time)
    {
        this.worldInfo.setWorldTime(time);
    }

    public 阻止位置 getSpawnPoint()
    {
        阻止位置 blockpos = new 阻止位置(this.worldInfo.getSpawnX(), this.worldInfo.getSpawnY(), this.worldInfo.getSpawnZ());

        if (!this.getWorldBorder().contains(blockpos))
        {
            blockpos = this.getHeight(new 阻止位置(this.getWorldBorder().getCenterX(), 0.0D, this.getWorldBorder().getCenterZ()));
        }

        return blockpos;
    }

    public void setSpawnPoint(阻止位置 pos)
    {
        this.worldInfo.setSpawn(pos);
    }

    public void joinEntityInSurroundings(实体 实体In)
    {
        int i = MathHelper.floor_double(实体In.X坐标 / 16.0D);
        int j = MathHelper.floor_double(实体In.Z坐标 / 16.0D);
        int k = 2;

        for (int l = i - k; l <= i + k; ++l)
        {
            for (int i1 = j - k; i1 <= j + k; ++i1)
            {
                this.getChunkFromChunkCoords(l, i1);
            }
        }

        if (!this.loaded实体List.contains(实体In))
        {
            this.loaded实体List.add(实体In);
        }
    }

    public boolean isBlockModifiable(实体Player player, 阻止位置 pos)
    {
        return true;
    }

    public void setEntityState(实体 实体In, byte state)
    {
    }

    public IChunkProvider getChunkProvider()
    {
        return this.chunkProvider;
    }

    public void addBlockEvent(阻止位置 pos, Block blockIn, int eventID, int eventParam)
    {
        blockIn.onBlockEventReceived(this, pos, this.getBlockState(pos), eventID, eventParam);
    }

    public ISaveHandler getSaveHandler()
    {
        return this.saveHandler;
    }

    public WorldInfo getWorldInfo()
    {
        return this.worldInfo;
    }

    public GameRules getGameRules()
    {
        return this.worldInfo.getGameRulesInstance();
    }

    public void updateAllPlayersSleepingFlag()
    {
    }

    public float getThunderStrength(float delta)
    {
        return (this.prevThunderingStrength + (this.thunderingStrength - this.prevThunderingStrength) * delta) * this.getRainStrength(delta);
    }

    public void setThunderStrength(float strength)
    {
        this.prevThunderingStrength = strength;
        this.thunderingStrength = strength;
    }

    public float getRainStrength(float delta)
    {
        return this.prevRainingStrength + (this.rainingStrength - this.prevRainingStrength) * delta;
    }

    public void setRainStrength(float strength)
    {
        this.prevRainingStrength = strength;
        this.rainingStrength = strength;
    }

    public boolean isThundering()
    {
        return (double)this.getThunderStrength(1.0F) > 0.9D;
    }

    public boolean isRaining()
    {
        return (double)this.getRainStrength(1.0F) > 0.2D;
    }

    public boolean isRainingAt(阻止位置 strikePosition)
    {
        if (!this.isRaining())
        {
            return false;
        }
        else if (!this.canSeeSky(strikePosition))
        {
            return false;
        }
        else if (this.getPrecipitationHeight(strikePosition).getY() > strikePosition.getY())
        {
            return false;
        }
        else
        {
            BiomeGenBase biomegenbase = this.getBiomeGenForCoords(strikePosition);
            return biomegenbase.getEnableSnow() ? false : (this.canSnowAt(strikePosition, false) ? false : biomegenbase.canRain());
        }
    }

    public boolean isBlockinHighHumidity(阻止位置 pos)
    {
        BiomeGenBase biomegenbase = this.getBiomeGenForCoords(pos);
        return biomegenbase.isHighHumidity();
    }

    public MapStorage getMapStorage()
    {
        return this.mapStorage;
    }

    public void setItemData(String dataID, WorldSavedData worldSavedDataIn)
    {
        this.mapStorage.setData(dataID, worldSavedDataIn);
    }

    public WorldSavedData loadItemData(Class <? extends WorldSavedData > clazz, String dataID)
    {
        return this.mapStorage.loadData(clazz, dataID);
    }

    public int getUniqueDataId(String key)
    {
        return this.mapStorage.getUniqueDataId(key);
    }

    public void playBroadcastSound(int p_175669_1_, 阻止位置 pos, int p_175669_3_)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            ((IWorldAccess)this.worldAccesses.get(i)).broadcastSound(p_175669_1_, pos, p_175669_3_);
        }
    }

    public void playAuxSFX(int p_175718_1_, 阻止位置 pos, int p_175718_3_)
    {
        this.playAuxSFXAtEntity((实体Player)null, p_175718_1_, pos, p_175718_3_);
    }

    public void playAuxSFXAtEntity(实体Player player, int sfxType, 阻止位置 pos, int p_180498_4_)
    {
        try
        {
            for (int i = 0; i < this.worldAccesses.size(); ++i)
            {
                ((IWorldAccess)this.worldAccesses.get(i)).playAuxSFX(player, sfxType, pos, p_180498_4_);
            }
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Playing level event");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Level event being played");
            crashreportcategory.addCrashSection("Block coordinates", CrashReportCategory.getCoordinateInfo(pos));
            crashreportcategory.addCrashSection("Event source", player);
            crashreportcategory.addCrashSection("Event type", Integer.valueOf(sfxType));
            crashreportcategory.addCrashSection("Event data", Integer.valueOf(p_180498_4_));
            throw new ReportedException(crashreport);
        }
    }

    public int getHeight()
    {
        return 256;
    }

    public int getActualHeight()
    {
        return this.provider.getHasNoSky() ? 128 : 256;
    }

    public Random setRandomSeed(int p_72843_1_, int p_72843_2_, int p_72843_3_)
    {
        long i = (long)p_72843_1_ * 341873128712L + (long)p_72843_2_ * 132897987541L + this.getWorldInfo().getSeed() + (long)p_72843_3_;
        this.rand.setSeed(i);
        return this.rand;
    }

    public 阻止位置 getStrongholdPos(String name, 阻止位置 pos)
    {
        return this.getChunkProvider().getStrongholdGen(this, name, pos);
    }

    public boolean extendedLevelsInChunkCache()
    {
        return false;
    }

    public double getHorizon()
    {
        return this.worldInfo.getTerrainType() == WorldType.FLAT ? 0.0D : 63.0D;
    }

    public CrashReportCategory addWorldInfoToCrashReport(CrashReport report)
    {
        CrashReportCategory crashreportcategory = report.makeCategoryDepth("Affected level", 1);
        crashreportcategory.addCrashSection("Level name", this.worldInfo == null ? "????" : this.worldInfo.getWorldName());
        crashreportcategory.addCrashSectionCallable("All players", new Callable<String>()
        {
            public String call()
            {
                return World.this.playerEntities.size() + " total; " + World.this.playerEntities.toString();
            }
        });
        crashreportcategory.addCrashSectionCallable("Chunk stats", new Callable<String>()
        {
            public String call()
            {
                return World.this.chunkProvider.makeString();
            }
        });

        try
        {
            this.worldInfo.addToCrashReport(crashreportcategory);
        }
        catch (Throwable throwable)
        {
            crashreportcategory.addCrashSectionThrowable("Level Data Unobtainable", throwable);
        }

        return crashreportcategory;
    }

    public void sendBlockBreakProgress(int breakerId, 阻止位置 pos, int progress)
    {
        for (int i = 0; i < this.worldAccesses.size(); ++i)
        {
            IWorldAccess iworldaccess = (IWorldAccess)this.worldAccesses.get(i);
            iworldaccess.sendBlockBreakProgress(breakerId, pos, progress);
        }
    }

    public Calendar getCurrentDate()
    {
        if (this.getTotalWorldTime() % 600L == 0L)
        {
            this.theCalendar.setTimeInMillis(MinecraftServer.getCurrentTimeMillis());
        }

        return this.theCalendar;
    }

    public void makeFireworks(double x, double y, double z, double motionX, double motionY, double motionZ, NBTTagCompound compund)
    {
    }

    public Scoreboard getScoreboard()
    {
        return this.worldScoreboard;
    }

    public void updateComparatorOutputLevel(阻止位置 pos, Block blockIn)
    {
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
        {
            阻止位置 blockpos = pos.offset(enumfacing);

            if (this.isBlockLoaded(blockpos))
            {
                IBlockState iblockstate = this.getBlockState(blockpos);

                if (Blocks.unpowered_comparator.isAssociated(iblockstate.getBlock()))
                {
                    iblockstate.getBlock().onNeighborBlockChange(this, blockpos, iblockstate, blockIn);
                }
                else if (iblockstate.getBlock().isNormalCube())
                {
                    blockpos = blockpos.offset(enumfacing);
                    iblockstate = this.getBlockState(blockpos);

                    if (Blocks.unpowered_comparator.isAssociated(iblockstate.getBlock()))
                    {
                        iblockstate.getBlock().onNeighborBlockChange(this, blockpos, iblockstate, blockIn);
                    }
                }
            }
        }
    }

    public DifficultyInstance getDifficultyForLocation(阻止位置 pos)
    {
        long i = 0L;
        float f = 0.0F;

        if (this.isBlockLoaded(pos))
        {
            f = this.getCurrentMoonPhaseFactor();
            i = this.getChunkFromBlockCoords(pos).getInhabitedTime();
        }

        return new DifficultyInstance(this.getDifficulty(), this.getWorldTime(), i, f);
    }

    public EnumDifficulty getDifficulty()
    {
        return this.getWorldInfo().getDifficulty();
    }

    public int getSkylightSubtracted()
    {
        return this.skylightSubtracted;
    }

    public void setSkylightSubtracted(int newSkylightSubtracted)
    {
        this.skylightSubtracted = newSkylightSubtracted;
    }

    public int getLastLightningBolt()
    {
        return this.lastLightningBolt;
    }

    public void setLastLightningBolt(int lastLightningBoltIn)
    {
        this.lastLightningBolt = lastLightningBoltIn;
    }

    public boolean isFindingSpawnPoint()
    {
        return this.findingSpawnPoint;
    }

    public VillageCollection getVillageCollection()
    {
        return this.villageCollectionObj;
    }

    public WorldBorder getWorldBorder()
    {
        return this.worldBorder;
    }

    public boolean isSpawnChunk(int x, int z)
    {
        阻止位置 blockpos = this.getSpawnPoint();
        int i = x * 16 + 8 - blockpos.getX();
        int j = z * 16 + 8 - blockpos.getZ();
        int k = 128;
        return i >= -k && i <= k && j >= -k && j <= k;
    }
}
