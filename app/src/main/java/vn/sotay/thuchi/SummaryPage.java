package vn.sotay.thuchi;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.Locale;

final class SummaryPage {
    private static final int WHITE=0xfff2f5ff,MUTED=0xff95a6c2,GREEN=0xff68dac4,PINK=0xffff92a9;
    private static String share(SummaryData.Category category,SummaryData data){return String.format(Locale.forLanguageTag("vi-VN"),"%.2f%%",category.share(data.total));}
    static void bind(MainActivity a) {
        YearMonth month=a.summaryMonth;boolean income=a.summaryIncome;
        SummaryData data=SummaryData.from(a.store.rows("tx","day DESC,id DESC"),month,income);
        a.label(R.id.summary_period,"Tháng "+month.getMonthValue()+" / "+month.getYear()+"  ⌄");
        a.click(R.id.summary_back,()->{a.page="";a.render();});
        a.click(R.id.summary_previous,()->{a.summaryMonth=month.minusMonths(1);a.render();});
        a.click(R.id.summary_next,()->{a.summaryMonth=month.plusMonths(1);a.render();});
        a.click(R.id.summary_period,()->chooseMonth(a));
        a.label(R.id.summary_type_label,income?"TỔNG THU NHẬP":"TỔNG CHI TIÊU");
        a.label(R.id.summary_total,a.cash(data.total));
        a.label(R.id.summary_count,data.count+" giao dịch");
        TextView comparison=a.body.findViewById(R.id.summary_comparison);comparison.setTextColor(MUTED);
        if(data.previousTotal==0)comparison.setText(data.total==0?"Chưa có giao dịch":"Tháng trước: 0 ₫");
        else {
            double change=(data.total-data.previousTotal)*100.0/data.previousTotal;
            comparison.setText(change==0?"Không đổi so với tháng "+month.minusMonths(1).getMonthValue():
                    (change>0?"↑ ":"↓ ")+String.format(Locale.forLanguageTag("vi-VN"),"%.1f%%",Math.abs(change))+" so với tháng "+month.minusMonths(1).getMonthValue());
            if(change!=0)comparison.setTextColor((income?change>0:change<0)?GREEN:PINK);
        }
        toggle(a,R.id.summary_expenses,!income,()->{if(a.summaryIncome){a.summaryIncome=false;a.render();}});
        toggle(a,R.id.summary_income,income,()->{if(!a.summaryIncome){a.summaryIncome=true;a.render();}});
        DonutChart chart=a.body.findViewById(R.id.summary_chart);
        chart.setData(data,category->details(a,month,income,category,data));
        LinearLayout list=a.list(R.id.summary_categories);
        if(data.categories.isEmpty()) {
            LinearLayout empty=a.col();empty.setPadding(a.dp(18),a.dp(22),a.dp(18),a.dp(16));
            a.addText(empty,income?"Tháng này chưa có khoản thu":"Tháng này chưa có khoản chi",16,WHITE,true);
            a.addText(empty,"Thêm giao dịch để thấy tỷ lệ của từng danh mục tại đây.",13,MUTED,false);
            a.action(empty,income?"＋ Thêm khoản thu":"＋ Thêm khoản chi",()->a.editTransaction(null,income),true);
            list.addView(empty);return;
        }
        for(SummaryData.Category category:data.categories){
            LinearLayout row=a.row();row.setPadding(a.dp(10),a.dp(13),a.dp(10),a.dp(13));row.setMinimumHeight(a.dp(76));
            TextView icon=a.text(symbol(category.name,income),21,category.color,true);icon.setGravity(Gravity.CENTER);
            GradientDrawable iconBackground=a.bg((category.color&0xffffff)|0x22000000,13);iconBackground.setStroke(a.dp(1),category.color);icon.setBackground(iconBackground);
            row.addView(icon,new LinearLayout.LayoutParams(a.dp(40),a.dp(42)));
            LinearLayout name=a.col();LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(0,-2,1);nameParams.setMargins(a.dp(10),0,a.dp(6),0);
            TextView title=a.text(category.name,14,WHITE,true);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);name.addView(title);
            TextView count=a.text(category.count+" giao dịch",11,MUTED,false);name.addView(count);row.addView(name,nameParams);
            LinearLayout values=a.col();values.setGravity(Gravity.END);
            TextView amount=a.text(a.cash(category.amount),15,WHITE,true);amount.setGravity(Gravity.END);amount.setMaxLines(1);
            amount.setAutoSizeTextTypeUniformWithConfiguration(10,15,1,TypedValue.COMPLEX_UNIT_SP);
            values.addView(amount,new LinearLayout.LayoutParams(-1,a.dp(26)));
            TextView percent=a.text(share(category,data),12,category.color,true);percent.setGravity(Gravity.END);values.addView(percent);
            row.addView(values,new LinearLayout.LayoutParams(a.dp(124),-2));
            row.setTag("summary:"+category.name);row.setFocusable(true);
            row.setContentDescription(category.name+", "+a.cash(category.amount)+", "+share(category,data)+", "+category.count+" giao dịch. Chạm để xem chi tiết.");
            row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x228d83ff),a.bg(Color.TRANSPARENT,18),a.bg(Color.WHITE,18)));
            row.setOnClickListener(v->details(a,month,income,category,data));Motion.press(row);list.addView(row);
        }
    }
    private static void toggle(MainActivity a,int id,boolean selected,Runnable action){
        TextView view=a.body.findViewById(id);view.setSelected(selected);view.setTextColor(selected?WHITE:MUTED);
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x228d83ff),a.bg(selected?0xff46506e:Color.TRANSPARENT,20),a.bg(Color.WHITE,20)));
        view.setOnClickListener(v->action.run());Motion.press(view);
    }
    private static String symbol(String name,boolean income){
        String lower=name.toLowerCase(Locale.forLanguageTag("vi-VN"));
        if(lower.contains("nhà"))return "⌂";if(lower.contains("ăn"))return "🍽";
        if(lower.contains("chuyển"))return "🚗";if(lower.contains("mua"))return "🛍";
        if(lower.contains("hóa đơn"))return "💡";if(lower.contains("giáo"))return "📚";
        if(lower.contains("sức"))return "✚";if(lower.contains("giải trí"))return "♫";
        if(lower.contains("lương")||lower.contains("kinh doanh"))return "▣";
        if(lower.contains("quà")||lower.contains("thưởng"))return "🎁";
        return income?"↙":"•";
    }
    static AlertDialog details(MainActivity a,YearMonth month,boolean income,SummaryData.Category category,SummaryData data){
        LinearLayout content=a.form();a.addText(content,a.cash(category.amount),27,MainActivity.GREEN,true);
        a.addText(content,share(category,data)+" tổng "+(income?"thu nhập":"chi tiêu")+" tháng "+month.getMonthValue()+"/"+month.getYear(),15,MainActivity.INK,true);
        a.addText(content,category.count+" giao dịch · Tổng tháng: "+a.cash(data.total),12,MainActivity.MUTED,false);
        for(ContentValues tx:a.store.rows("tx","day DESC,id DESC")){
            if((a.num(tx,"income")==1)!=income||!a.str(tx,"day").startsWith(month.toString())||!a.str(tx,"category").equals(category.name))continue;
            LinearLayout item=a.card(content);a.addText(item,a.str(tx,"note"),15,MainActivity.INK,true);
            a.addText(item,(income?"+":"−")+a.cash(a.num(tx,"amount")),17,income?MainActivity.GREEN:MainActivity.RED,true);
            a.addText(item,LocalDate.parse(a.str(tx,"day")).format(MainActivity.DATE),12,MainActivity.MUTED,false);
        }
        return a.showInfo(category.name,content);
    }
    private static void chooseMonth(MainActivity a){
        LinearLayout form=a.form(),row=a.row();NumberPicker month=new NumberPicker(a),year=new NumberPicker(a);
        month.setMinValue(1);month.setMaxValue(12);month.setValue(a.summaryMonth.getMonthValue());month.setContentDescription("Tháng");
        year.setMinValue(Math.min(1970,a.summaryMonth.getYear()));year.setMaxValue(Math.max(2100,a.summaryMonth.getYear()));year.setValue(a.summaryMonth.getYear());year.setWrapSelectorWheel(false);year.setContentDescription("Năm");
        row.addView(month,new LinearLayout.LayoutParams(0,-2,1));row.addView(year,new LinearLayout.LayoutParams(0,-2,1));form.addView(row);
        a.dialog("Chọn tháng",form,"Xem",()->{month.clearFocus();year.clearFocus();a.summaryMonth=YearMonth.of(year.getValue(),month.getValue());},null);
    }
}
