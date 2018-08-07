package org.maxgamer.quickshop.Util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.server.v1_13_R1.IMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Util.CustomPotionsName.GenericPotionData;
import org.maxgamer.quickshop.Util.CustomPotionsName.GenericPotionData.Category;

@SuppressWarnings("deprecation")
public class NMS {
	private static ArrayList<NMSDependent> nmsDependencies = new ArrayList<NMSDependent>();
	private static int nextId = 0;
	private static NMSDependent nms;
	
	static {

		nmsDependencies.add(new NMSDependent("v1_13") {
			@Override
			public void safeGuard(Item item) {
				if(QuickShop.debug)System.out.println("safeGuard");
				org.bukkit.inventory.ItemStack iStack = item.getItemStack();
				net.minecraft.server.v1_13_R1.ItemStack nmsI = org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack.asNMSCopy(iStack);
				nmsI.setCount(0);
				iStack = org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack.asBukkitCopy(nmsI);
				item.setItemStack(iStack);
			}

			@Override
			public byte[] getNBTBytes(org.bukkit.inventory.ItemStack iStack) {
				try{
					if(QuickShop.debug)System.out.println("getNBTBytes");
					net.minecraft.server.v1_13_R1.ItemStack is = org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack.asNMSCopy(iStack);
					net.minecraft.server.v1_13_R1.NBTTagCompound itemCompound = new net.minecraft.server.v1_13_R1.NBTTagCompound();
					itemCompound = is.save(itemCompound);
					ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
					DataOutputStream dataoutputstream = new DataOutputStream(new GZIPOutputStream(bytearrayoutputstream));
					try {
						net.minecraft.server.v1_13_R1.NBTCompressedStreamTools.a(itemCompound, (DataOutput) dataoutputstream);
					} finally {
						dataoutputstream.close();
					}
					return bytearrayoutputstream.toByteArray();
				}catch(Exception e){
					return new byte[0];
				}
			}

			@Override
			public org.bukkit.inventory.ItemStack getItemStack(byte[] bytes) {
				try{
					if(QuickShop.debug)System.out.println("getItemStack");
					DataInputStream datainputstream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes))));
					net.minecraft.server.v1_13_R1.NBTTagCompound nbttagcompound;
					try {
						nbttagcompound = net.minecraft.server.v1_13_R1.NBTCompressedStreamTools.a((DataInput) datainputstream, null);
					} finally {
						datainputstream.close();
					}

					//ToDo: THis may be broken.. I just added IMaterial
					net.minecraft.server.v1_13_R1.ItemStack is = new net.minecraft.server.v1_13_R1.ItemStack((IMaterial)nbttagcompound);
					return org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack.asBukkitCopy(is);
				}catch(Exception e){
					return new ItemStack(Material.AIR);
				}
			}

			@Override
			public GenericPotionData getPotionData(ItemStack potionItemStack) {
				PotionMeta potionMeta = null;

				if(potionItemStack.hasItemMeta())
					potionMeta = (PotionMeta) potionItemStack.getItemMeta();

				Category category = getPotionData(potionItemStack).getCategory();

				List<PotionEffect> effects = new ArrayList<PotionEffect>(potionMeta.getCustomEffects().size()+1);

				if (potionMeta.hasCustomEffects()) {
					effects.addAll(potionMeta.getCustomEffects());
				}

				PotionType potionType = getPotionData(potionItemStack).getType();
						//potionMeta..getBasePotionData().getType();


				//GenericPotionData(PotionType type, Collection<PotionEffect> effects, Category category, boolean isCustom, int duration, int amplifier)
				return new GenericPotionData(potionType, effects, category, getPotionData(potionItemStack).isCustom(), getPotionData(potionItemStack).getDuration(), getPotionData(potionItemStack).getAmplifier());
			}

			@Override
			public boolean isPotion(Material material) {
				switch(material) {
					case POTION:
				//	case SPLASH_POTION:
				//	case LINGERING_POTION:
						return true;
					default:
						return false;
				}
			}
		});


	}
	
	public static void safeGuard(Item item) throws ClassNotFoundException {
		if(QuickShop.debug)System.out.println("Renaming");
		rename(item.getItemStack());
		//if(QuickShop.debug)System.out.println("Protecting");
		//protect(item);
		if(QuickShop.debug)System.out.println("Seting pickup delay");
		item.setPickupDelay(2147483647);
	}

	public static void init() {
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		packageName = packageName.substring(packageName.lastIndexOf(".") + 1);
		//System.out.println("Package: " + packageName);
		for (NMSDependent dep : nmsDependencies) {
			if ((packageName.startsWith(dep.getVersion())) || ((dep.getVersion().isEmpty()) && ((packageName.equals("bukkit")) || (packageName.equals("craftbukkit"))))) {
				nms = dep;
				return;
			}
		}
		throw new UnsupportedClassVersionError("This version of QuickShop is incompatible. Internal version: "+packageName);
	}

	private static void rename(ItemStack iStack) {
		ItemMeta meta = iStack.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "QuickShop " + Util.getName(iStack) + " " + nextId++);
		iStack.setItemMeta(meta);
	}

	public static byte[] getNBTBytes(org.bukkit.inventory.ItemStack iStack) throws ClassNotFoundException {
		return nms.getNBTBytes(iStack);
	}

	public static ItemStack getItemStack(byte[] bytes) throws ClassNotFoundException {
		return nms.getItemStack(bytes);
	}
	
	public static boolean isPotion(Material material) {
		return nms.isPotion(material);
	}
	
	public static GenericPotionData getPotionData(ItemStack potion) {
		return nms.getPotionData(potion);
	}

	/*private static void protect(Item item) {
		try {
			Field itemField = item.getClass().getDeclaredField("item");
			itemField.setAccessible(true);
			Object nmsEntityItem = itemField.get(item);
			Method getItemStack;
			try {
				getItemStack = nmsEntityItem.getClass().getMethod("getItemStack", new Class[0]);
			} catch (NoSuchMethodException e) {
				try {
					getItemStack = nmsEntityItem.getClass().getMethod("d", new Class[0]);
				} catch (NoSuchMethodException e2) {
					return;
				}
			}
			Object itemStack = getItemStack.invoke(nmsEntityItem, new Object[0]);
			Field countField;
			try {
				countField = itemStack.getClass().getDeclaredField("count");
			} catch (NoSuchFieldException e) {
				countField = itemStack.getClass().getDeclaredField("a");
			}
			countField.setAccessible(true);
			countField.set(itemStack, Integer.valueOf(0));
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			System.out.println("[QuickShop] Could not protect item from pickup properly! Dupes are now possible.");
		} catch (Exception e) {
			System.out.println("Other error");
			e.printStackTrace();
		}
	}*/

	private static abstract class NMSDependent {
		private String version;

		public String getVersion() {
			return this.version;
		}

		public NMSDependent(String version) {
			this.version = version;
		}
		
		public abstract boolean isPotion(Material material);
		
		public abstract GenericPotionData getPotionData(ItemStack potionItemStack);
		
		public abstract void safeGuard(Item paramItem);

		public byte[] getNBTBytes(org.bukkit.inventory.ItemStack paramItemStack) {
			throw new UnsupportedOperationException();
		}

		public org.bukkit.inventory.ItemStack getItemStack(byte[] paramArrayOfByte) {
			throw new UnsupportedOperationException();
		}
	}
}
