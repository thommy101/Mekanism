package mekanism.common.base;

import java.util.Collection;

import mekanism.common.recipe.ShapedMekanismRecipe;

public interface IBlockType
{
	String getBlockName();
	
	Collection<ShapedMekanismRecipe> getRecipes();
	
	void addRecipes(Collection<ShapedMekanismRecipe> recipes);
	
	void addRecipe(ShapedMekanismRecipe recipe);
	
	boolean isEnabled();
}
