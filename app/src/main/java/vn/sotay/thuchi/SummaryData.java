package vn.sotay.thuchi;

import android.content.ContentValues;
import java.time.YearMonth;
import java.util.*;

/** The chart, legend and drill-down all use the same month and transaction type. */
final class SummaryData {
    static final int[] COLORS={0xff8d83ff,0xffff7d98,0xff65b0ff,0xfff3c46d,0xffcf88e6,
            0xfff49a72,0xff60d4bf,0xffafc577,0xff80cce0,0xffafa9e9};
    static final class Category {
        final String name;final int color;long amount;int count;
        Category(String name,boolean income){this(name,COLORS[Math.floorMod(name.hashCode()+(income?3:0),COLORS.length)]);}
        private Category(String name,int color){this.name=name;this.color=color;}
        double share(long total){return total==0?0:amount*100.0/total;}
    }
    final List<Category> categories;
    final long total,previousTotal;
    final int count;
    private SummaryData(List<Category> categories,long total,long previousTotal,int count){
        this.categories=Collections.unmodifiableList(categories);this.total=total;this.previousTotal=previousTotal;this.count=count;
    }
    static SummaryData from(List<ContentValues> rows,YearMonth month,boolean income) {
        String prefix=month.toString(),previous=month.minusMonths(1).toString();
        Map<String,Category> groups=new HashMap<>();long total=0,previousTotal=0;int count=0;
        for(ContentValues row:rows) {
            if((row.getAsInteger("income")==1)!=income)continue;
            String day=row.getAsString("day");long amount=row.getAsLong("amount");
            if(day.startsWith(previous))previousTotal+=amount;
            if(!day.startsWith(prefix))continue;
            String name=row.getAsString("category");
            Category category=groups.computeIfAbsent(name,key->new Category(key,income));
            category.amount+=amount;category.count++;total+=amount;count++;
        }
        List<Category> categories=new ArrayList<>(groups.values());
        categories.sort((a,b)->{int result=Long.compare(b.amount,a.amount);return result==0?a.name.compareTo(b.name):result;});
        // Adjacent entries should remain distinct even when names have the same color hash.
        Set<Integer> used=new HashSet<>();List<Category> distinct=new ArrayList<>();
        for(Category entry:categories) {
            int index=0;while(index<COLORS.length&&COLORS[index]!=entry.color)index++;
            if(used.size()<COLORS.length)while(used.contains(COLORS[index%COLORS.length]))index++;
            Category colored=new Category(entry.name,COLORS[index%COLORS.length]);
            colored.amount=entry.amount;colored.count=entry.count;distinct.add(colored);used.add(colored.color);
        }
        return new SummaryData(distinct,total,previousTotal,count);
    }
}
