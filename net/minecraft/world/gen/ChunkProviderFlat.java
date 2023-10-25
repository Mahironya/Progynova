package net.minecraft.world.gen;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.阻止位置;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureOceanMonument;

public class ChunkProviderFlat implements IChunkProvider
{
    private World worldObj;
    private Random random;
    private final IBlockState[] cachedBlockIDs = new IBlockState[256];
    private final FlatGeneratorInfo flatWorldGenInfo;
    private final List<MapGenStructure> structureGenerators = Lists.<MapGenStructure>newArrayList();
    private final boolean hasDecoration;
    private final boolean hasDungeons;
    private WorldGenLakes waterLakeGenerator;
    private WorldGenLakes lavaLakeGenerator;

    public ChunkProviderFlat(World worldIn, long seed, boolean generateStructures, String flatGeneratorSettings)
    {
        this.worldObj = worldIn;
        this.random = new Random(seed);
        this.flatWorldGenInfo = FlatGeneratorInfo.createFlatGeneratorFromString(flatGeneratorSettings);

        if (generateStructures)
        {
            Map<String, Map<String, String>> map = this.flatWorldGenInfo.getWorldFeatures();

            if (map.containsKey("village"))
            {
                Map<String, String> map1 = (Map)map.get("village");

                if (!map1.containsKey("size"))
                {
                    map1.put("size", "1");
                }

                this.structureGenerators.add(new MapGenVillage(map1));
            }

            if (map.containsKey("biome_1"))
            {
                this.structureGenerators.add(new MapGenScatteredFeature((Map)map.get("biome_1")));
            }

            if (map.containsKey("mineshaft"))
            {
                this.structureGenerators.add(new MapGenMineshaft((Map)map.get("mineshaft")));
            }

            if (map.containsKey("stronghold"))
            {
                this.structureGenerators.add(new MapGenStronghold((Map)map.get("stronghold")));
            }

            if (map.containsKey("oceanmonument"))
            {
                this.structureGenerators.add(new StructureOceanMonument((Map)map.get("oceanmonument")));
            }
        }

        if (this.flatWorldGenInfo.getWorldFeatures().containsKey("lake"))
        {
            this.waterLakeGenerator = new WorldGenLakes(Blocks.water);
        }

        if (this.flatWorldGenInfo.getWorldFeatures().containsKey("lava_lake"))
        {
            this.lavaLakeGenerator = new WorldGenLakes(Blocks.lava);
        }

        this.hasDungeons = this.flatWorldGenInfo.getWorldFeatures().containsKey("dungeon");
        int j = 0;
        int k = 0;
        boolean flag = true;

        for (FlatLayerInfo flatlayerinfo : this.flatWorldGenInfo.getFlatLayers())
        {
            for (int i = flatlayerinfo.getMinY(); i < flatlayerinfo.getMinY() + flatlayerinfo.getLayerCount(); ++i)
            {
                IBlockState iblockstate = flatlayerinfo.getLayerMaterial();

                if (iblockstate.getBlock() != Blocks.air)
                {
                    flag = false;
                    this.cachedBlockIDs[i] = iblockstate;
                }
            }

            if (flatlayerinfo.getLayerMaterial().getBlock() == Blocks.air)
            {
                k += flatlayerinfo.getLayerCount();
            }
            else
            {
                j += flatlayerinfo.getLayerCount() + k;
                k = 0;
            }
        }

        worldIn.setSeaLevel(j);
        this.hasDecoration = flag ? false : this.flatWorldGenInfo.getWorldFeatures().containsKey("decoration");
    }

    public Chunk provideChunk(int x, int z)
    {
        ChunkPrimer chunkprimer = new ChunkPrimer();

        for (int i = 0; i < this.cachedBlockIDs.length; ++i)
        {
            IBlockState iblockstate = this.cachedBlockIDs[i];

            if (iblockstate != null)
            {
                for (int j = 0; j < 16; ++j)
                {
                    for (int k = 0; k < 16; ++k)
                    {
                        chunkprimer.setBlockState(j, i, k, iblockstate);
                    }
                }
            }
        }

        for (MapGenBase mapgenbase : this.structureGenerators)
        {
            mapgenbase.generate(this, this.worldObj, x, z, chunkprimer);
        }

        Chunk chunk = new Chunk(this.worldObj, chunkprimer, x, z);
        BiomeGenBase[] abiomegenbase = this.worldObj.getWorldChunkManager().loadBlockGeneratorData((BiomeGenBase[])null, x * 16, z * 16, 16, 16);
        byte[] abyte = chunk.getBiomeArray();

        for (int l = 0; l < abyte.length; ++l)
        {
            abyte[l] = (byte)abiomegenbase[l].biomeID;
        }

        chunk.generateSkylightMap();
        return chunk;
    }

    public boolean chunkExists(int x, int z)
    {
        return true;
    }

    public void populate(IChunkProvider chunkProvider, int x, int z)
    {
        int i = x * 16;
        int j = z * 16;
        阻止位置 blockpos = new 阻止位置(i, 0, j);
        BiomeGenBase biomegenbase = this.worldObj.getBiomeGenForCoords(new 阻止位置(i + 16, 0, j + 16));
        boolean flag = false;
        this.random.setSeed(this.worldObj.getSeed());
        long k = this.random.nextLong() / 2L * 2L + 1L;
        long l = this.random.nextLong() / 2L * 2L + 1L;
        this.random.setSeed((long)x * k + (long)z * l ^ this.worldObj.getSeed());
        ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(x, z);

        for (MapGenStructure mapgenstructure : this.structureGenerators)
        {
            boolean flag1 = mapgenstructure.generateStructure(this.worldObj, this.random, chunkcoordintpair);

            if (mapgenstructure instanceof MapGenVillage)
            {
                flag |= flag1;
            }
        }

        if (this.waterLakeGenerator != null && !flag && this.random.nextInt(4) == 0)
        {
            this.waterLakeGenerator.generate(this.worldObj, this.random, blockpos.add(this.random.nextInt(16) + 8, this.random.nextInt(256), this.random.nextInt(16) + 8));
        }

        if (this.lavaLakeGenerator != null && !flag && this.random.nextInt(8) == 0)
        {
            阻止位置 blockpos1 = blockpos.add(this.random.nextInt(16) + 8, this.random.nextInt(this.random.nextInt(248) + 8), this.random.nextInt(16) + 8);

            if (blockpos1.getY() < this.worldObj.getSeaLevel() || this.random.nextInt(10) == 0)
            {
                this.lavaLakeGenerator.generate(this.worldObj, this.random, blockpos1);
            }
        }

        if (this.hasDungeons)
        {
            for (int i1 = 0; i1 < 8; ++i1)
            {
                (new WorldGenDungeons()).generate(this.worldObj, this.random, blockpos.add(this.random.nextInt(16) + 8, this.random.nextInt(256), this.random.nextInt(16) + 8));
            }
        }

        if (this.hasDecoration)
        {
            biomegenbase.decorate(this.worldObj, this.random, blockpos);
        }
    }

    public boolean populateChunk(IChunkProvider chunkProvider, Chunk chunkIn, int x, int z)
    {
        return false;
    }

    public boolean saveChunks(boolean saveAllChunks, IProgressUpdate progressCallback)
    {
        return true;
    }

    public void saveExtraData()
    {
    }

    public boolean unloadQueuedChunks()
    {
        return false;
    }

    public boolean canSave()
    {
        return true;
    }

    public String makeString()
    {
        return "FlatLevelSource";
    }

    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, 阻止位置 pos)
    {
        BiomeGenBase biomegenbase = this.worldObj.getBiomeGenForCoords(pos);
        return biomegenbase.getSpawnableList(creatureType);
    }

    public 阻止位置 getStrongholdGen(World worldIn, String structureName, 阻止位置 position)
    {
        if ("Stronghold".equals(structureName))
        {
            for (MapGenStructure mapgenstructure : this.structureGenerators)
            {
                if (mapgenstructure instanceof MapGenStronghold)
                {
                    return mapgenstructure.getClosestStrongholdPos(worldIn, position);
                }
            }
        }

        return null;
    }

    public int getLoadedChunkCount()
    {
        return 0;
    }

    public void recreateStructures(Chunk chunkIn, int x, int z)
    {
        for (MapGenStructure mapgenstructure : this.structureGenerators)
        {
            mapgenstructure.generate(this, this.worldObj, x, z, (ChunkPrimer)null);
        }
    }

    public Chunk provideChunk(阻止位置 阻止位置In)
    {
        return this.provideChunk(阻止位置In.getX() >> 4, 阻止位置In.getZ() >> 4);
    }
}
