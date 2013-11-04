package com.elmakers.mine.bukkit.plugins.magic.spells;

import org.bukkit.entity.Fireball;

import com.elmakers.mine.bukkit.plugins.magic.Spell;
import com.elmakers.mine.bukkit.plugins.magic.SpellResult;
import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;

public class FireballSpell extends Spell 
{
	int defaultSize = 1;

	@Override
	public SpellResult onCast(ConfigurationNode parameters) 
	{
		int size = parameters.getInt("size", defaultSize);
		boolean useFire = parameters.getBoolean("fire", true);
		Fireball fireball = (Fireball)player.launchProjectile(Fireball.class);
		fireball.setIsIncendiary(useFire);
		fireball.setYield(size);
		return SpellResult.SUCCESS;
	}

	@Override
	public void onLoad(ConfigurationNode node)
	{
		disableTargeting();
	}
}
