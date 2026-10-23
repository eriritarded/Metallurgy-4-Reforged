package it.hurts.metallurgy_reforged.machine;
import com.google.gson.*;
import it.hurts.metallurgy_reforged.effect.VersionBridge;
import net.minecraft.world.item.ItemStack;
/** Per-output weighted XP, independent of recipe-map iteration order.
 * Hopper removals discard their proportional share, as source slots gave XP only
 * for player-taken items. Inventory stacks are never modified by this ledger. */
public final class MachineExperience {
    private final String[] item=new String[5];
    private final int[] count=new int[5];
    private final double[] xp=new double[5];
    private void clear(int slot) {item[slot]=null;count[slot]=0;xp[slot]=0;}
    private void reconcile(int slot,String id,int actual,float fallback) {
        actual=Math.max(0,Math.min(64,actual));
        if(!id.equals(item[slot])) {clear(slot);item[slot]=id;}
        if(count[slot]>actual) xp[slot]*=actual/(double)count[slot];
        else if(count[slot]<actual) xp[slot]+=(actual-count[slot])*rate(fallback);
        count[slot]=actual;if(actual==0) xp[slot]=0;
    }
    private static double rate(float value) {return Float.isFinite(value)?Math.max(0,Math.min(1000,value)):0;}
    public void produced(int slot,ItemStack current,ItemStack result,float rate,boolean alloy) {
        reconcile(slot,VersionBridge.itemId(result),current.getCount(),MachineRecipes.experience(current,alloy));
        int added=Math.min(result.getCount(),64-count[slot]);count[slot]+=added;xp[slot]+=added*rate(rate);
    }
    public float claim(int slot,ItemStack taken,int remaining,int removed,boolean alloy) {
        removed=Math.max(0,Math.min(64,removed));reconcile(slot,VersionBridge.itemId(taken),remaining+removed,MachineRecipes.experience(taken,alloy));
        int used=Math.min(removed,count[slot]);double amount=count[slot]==0?0:xp[slot]*used/count[slot];
        xp[slot]-=amount;count[slot]-=used;if(count[slot]==0) clear(slot);return (float)amount;
    }
    public String save() {
        JsonArray array=new JsonArray();
        for(int slot=0;slot<5;slot++) if(item[slot]!=null && count[slot]>0) {JsonObject row=new JsonObject();row.addProperty("slot",slot);row.addProperty("item",item[slot]);row.addProperty("count",count[slot]);row.addProperty("xp",xp[slot]);array.add(row);}
        return array.toString();
    }
    public void load(String value) {
        for(int slot=0;slot<5;slot++) clear(slot);if(value.isEmpty() || value.length()>65536) return;
        try {
            var rows=JsonParser.parseString(value).getAsJsonArray();if(rows.size()>5) return;
            for(var element:rows) {var row=element.getAsJsonObject();int slot=row.get("slot").getAsInt(),amount=row.get("count").getAsInt();String id=row.get("item").getAsString();double experience=row.get("xp").getAsDouble();
                if(slot<0 || slot>=5 || amount<1 || amount>64 || id.length()>256 || !id.contains(":") || !Double.isFinite(experience) || experience<0 || experience>amount*1000D) continue;
                item[slot]=id;count[slot]=amount;xp[slot]=experience;
            }
        } catch(RuntimeException invalidLedger) {for(int slot=0;slot<5;slot++) clear(slot);}
    }
}
