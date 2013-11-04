package com.elmakers.mine.bukkit.plugins.magic.spells;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import com.elmakers.mine.bukkit.dao.BlockList;
import com.elmakers.mine.bukkit.plugins.magic.Spell;
import com.elmakers.mine.bukkit.plugins.magic.SpellResult;
import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;

public class ConstructSpell extends Spell
{
	static final String		DEFAULT_DESTRUCTIBLES	= "0,1,2,3,4,8,9,10,11,12,13,87,88";

	private List<Material>	destructibleMaterials	= null;
	private ConstructionType defaultConstructionType = ConstructionType.SPHERE;
	private int				defaultRadius			= 2;
	private int             timeToLive              = 0;

	public enum ConstructionType
	{
		SPHERE,
		CUBOID,
		UNKNOWN;

		public static ConstructionType parseString(String s, ConstructionType defaultType)
		{
			ConstructionType construct = defaultType;
			for (ConstructionType t : ConstructionType.values())
			{
				if (t.name().equalsIgnoreCase(s))
				{
					construct = t;
				}
			}
			return construct;
		}
	};

	@SuppressWarnings("deprecation")
	@Override
	public SpellResult onCast(ConfigurationNode parameters) 
	{
		targetThrough(Material.GLASS);
		Block target = getTarget().getBlock();

		if (target == null)
		{
			initializeTargeting(player);
			noTargetThrough(Material.GLASS);
			target = getTarget().getBlock();
		}

		if (target == null)
		{
			castMessage("No target");
			return SpellResult.NO_TARGET;
		}
		if (!hasBuildPermission(target)) {
			castMessage("You don't have permission to build here.");
			return SpellResult.INSUFFICIENT_PERMISSION;
		}

		Material material = target.getType();
		byte data = target.getData();

		ItemStack buildWith = getBuildingMaterial();
		if (buildWith != null)
		{
			material = buildWith.getType();
			data = getItemData(buildWith);
		}

		ConstructionType conType = defaultConstructionType;

		boolean hollow = false;
		String fillType = (String)parameters.getString("fill", "");
		hollow = fillType.equals("hollow");

		Material materialOverride = parameters.getMaterial("material");
		if (materialOverride != null)
		{
			material = materialOverride;
			data = 0;
		}

		int radius = parameters.getInt("radius", defaultRadius);		
		String typeString = parameters.getString("type", "");

		ConstructionType testType = ConstructionType.parseString(typeString, ConstructionType.UNKNOWN);
		if (testType != ConstructionType.UNKNOWN)
		{
			conType = testType;
		}

		switch (conType)
		{
		case SPHERE: constructSphere(target, radius, material, data, !hollow); break;
		case CUBOID: constructCuboid(target, radius, material, data, !hollow); break;
		default : return SpellResult.FAILURE;
		}

		return SpellResult.SUCCESS;
	}

	public void constructCuboid(Block target, int radius, Material material, byte data, boolean fill)
	{
		fillArea(target, radius, material, data, fill, false);
	}

	public void constructSphere(Block target, int radius, Material material, byte data, boolean fill)
	{
		fillArea(target, radius, material, data, fill, true);
	}

	public void fillArea(Block target, int radius, Material material, byte data, boolean fill, boolean sphere)
	{
		BlockList constructedBlocks = new BlockList();
		int diameter = radius * 2;
		int midX = (diameter - 1) / 2;
		int midY = (diameter - 1) / 2;
		int midZ = (diameter - 1) / 2;
		int diameterOffset = diameter - 1;
		int radiusSquared = (radius - 1) * (radius - 1);

		for (int x = 0; x < radius; ++x)
		{
			for (int y = 0; y < radius; ++y)
			{
				for (int z = 0; z < radius; ++z)
				{
					boolean fillBlock = false;

					if (sphere)
					{
						int distanceSquared = getDistanceSquared(x - midX, y - midY, z - midZ);
						fillBlock = distanceSquared <= radiusSquared;
						if (!fill)
						{
							fillBlock = fillBlock && distanceSquared >= radiusSquared - 16;
						}
					}
					else
					{
						fillBlock = fill ? true : (x == 0 || y == 0 || z == 0);
					}
					if (fillBlock)
					{
						constructBlock(x, y, z, target, radius, material, data, constructedBlocks);
						constructBlock(diameterOffset - x, y, z, target, radius, material, data, constructedBlocks);
						constructBlock(x, diameterOffset - y, z, target, radius, material, data, constructedBlocks);
						constructBlock(x, y, diameterOffset - z, target, radius, material, data, constructedBlocks);
						constructBlock(diameterOffset - x, diameterOffset - y, z, target, radius, material, data, constructedBlocks);
						constructBlock(x, diameterOffset - y, diameterOffset - z, target, radius, material, data, constructedBlocks);
						constructBlock(diameterOffset - x, y, diameterOffset - z, target, radius, material, data, constructedBlocks);
						constructBlock(diameterOffset - x, diameterOffset - y, diameterOffset - z, target, radius, material, data, constructedBlocks);
					}
				}
			}
		}

		if (timeToLive == 0)
		{
			spells.addToUndoQueue(player, constructedBlocks);
		}
		else
		{
			constructedBlocks.setTimeToLive(timeToLive);
			spells.scheduleCleanup(constructedBlocks);
		}
		castMessage("Constructed " + constructedBlocks.size() + "blocks");
	}

	public int getDistanceSquared(int x, int y, int z)
	{
		return x * x + y * y + z * z;
	}

	@SuppressWarnings("deprecation")
	public void constructBlock(int dx, int dy, int dz, Block centerPoint, int radius, Material material, byte data, BlockList constructedBlocks)
	{
		int x = centerPoint.getX() + dx - radius;
		int y = centerPoint.getY() + dy - radius;
		int z = centerPoint.getZ() + dz - radius;
		Block block = player.getWorld().getBlockAt(x, y, z);
		if (!isDestructible(block))
		{
			return;
		}
		if (!hasBuildPermission(block)) 
		{
			return;
		}
		constructedBlocks.add(block);
		block.setType(material);
		block.setData(data);
	}

	public boolean isDestructible(Block block)
	{
		return destructibleMaterials.contains(block.getType());
	}

	@Override
	public void onLoad(ConfigurationNode properties)  
	{
		destructibleMaterials = destructibleMaterials != null ? destructibleMaterials : csv.parseMaterials(DEFAULT_DESTRUCTIBLES);
		destructibleMaterials = properties.getMaterials("destructible", destructibleMaterials);
		timeToLive = properties.getInt("undo", timeToLive);
	}
	
	@Override
	public boolean usesMaterial() {
		return true;
	}
}
