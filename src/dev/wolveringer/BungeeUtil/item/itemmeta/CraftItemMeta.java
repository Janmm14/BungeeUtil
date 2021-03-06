package dev.wolveringer.BungeeUtil.item.itemmeta;

import java.util.ArrayList;
import java.util.List;

import dev.wolveringer.BungeeUtil.item.Item;
import dev.wolveringer.api.inventory.Inventory;
import dev.wolveringer.nbt.NBTTagCompound;
import dev.wolveringer.nbt.NBTTagList;
import dev.wolveringer.nbt.NBTTagString;

public class CraftItemMeta implements ItemMeta {
	protected Item item;
	protected ArrayList<MetaListener> listener = new ArrayList<MetaListener>();

	public CraftItemMeta(Item i) {
		this.item = i;
	}

	@Override
	public String getDisplayName() {
		if (!getTag().hasKey("display"))
			getTag().set("display", new NBTTagCompound());
		return hasDisplayName() ? getTag().getCompound("display").getString("Name") : "";
	}

	@Override
	public List<String> getLore() {
		if (!hasLore())
			return new ArrayList<String>();
		List<String> l = new ArrayList<>();
		NBTTagList x = getTag().getCompound("display").getList("Lore");
		for (int y = 0; y < x.size(); y++)
			l.add(x.getString(y));
		return l;
	}

	@Override
	public boolean hasDisplayName() {
		return getTag() != null && getTag().hasKey("display") && getTag().getCompound("display").hasKey("Name");
	}

	@Override
	public boolean hasLore() {
		return getTag() != null && getTag().hasKey("display") && getTag().getCompound("display").hasKey("Lore");
	}

	@Override
	public void setDisplayName(String name) {
		if (!getTag().hasKey("display"))
			getTag().set("display", new NBTTagCompound());
		getTag().getCompound("display").set("Name", new NBTTagString(name));
		fireUpdate();
	}

	@Override
	public void setGlow(boolean b) {
		if (b) {
			if (!getTag().hasKey("ench"))
				getTag().set("ench", new NBTTagList());
		} else if (!b) {
			if (getTag().hasKey("ench"))
				getTag().remove("ench");
		}
		fireUpdate();
	}

	@Override
	public boolean hasGlow() {
		return getTag() != null && getTag().hasKey("ench");
	}

	@Override
	public void setLore(List<String> lore) {
		if (!getTag().hasKey("display"))
			getTag().set("display", new NBTTagCompound());
		NBTTagList l = new NBTTagList();
		for (String s : lore)
			l.add(new NBTTagString(s));
		getTag().getCompound("display").set("Lore", l);
		fireUpdate();
	}

	@Override
	public boolean hasTag() {
		return getTag() != null && !getTag().isEmpty();
	}

	public NBTTagCompound getTag() {
		if (item.getTag() == null)
			item.setTag(new NBTTagCompound());
		return item.getTag();
	}

	protected void fireUpdate() {
		for (StackTraceElement e : Thread.currentThread().getStackTrace())
			if (e.getClassName().contains(Inventory.class.getCanonicalName() + "$2")
					&& e.getMethodName().equalsIgnoreCase("onUpdate")) // recall
																		// in
																		// update
				return;
		if (listener.size() != 0){
			try{
			for (MetaListener l : new ArrayList<>(listener))
				if (l != null)
					l.onUpdate(item);
			}catch(NegativeArraySizeException e){
				System.out.println("NegativeArraySizeException from ArrayList?! I�m craz! sie()->"+listener.size());
			}
		}
	}

	public void addMetaListener(MetaListener listener) {
		this.listener.add(listener);
	}

	public void removeMetaListener(MetaListener listener) {
		this.listener.remove(listener);
	}

	@Override
	public String toString() {
		return "CraftItemMeta@" + System.identityHashCode(this) + "[listener=" + listener + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((item == null) ? 0 : item.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CraftItemMeta other = (CraftItemMeta) obj;
		if (item == null) {
			if (other.item != null)
				return false;
		} else if (!item.getTag().equals(other.item.getTag()))
			return false;
		return true;
	}

	protected void build() {}
}
