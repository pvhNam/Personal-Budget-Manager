package vn.sotay.thuchi;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import android.text.InputType;
import android.view.*;
import android.widget.*;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class MainActivity extends Activity {
    static final int GREEN=0xff247c83, INK=0xff23384c, MUTED=0xff586c7d, BG=0xffedf4f7, RED=0xffb25958;
    static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    Store store; SharedPreferences prefs; ViewGroup shell; LinearLayout body,nav; int tab=0; String page="",filter="Tất cả",query="";
    boolean allTransactions=true,trendExpanded=false,categoryIncome=false;
    String renderedRoute="";
    boolean summaryIncome=false;
    YearMonth summaryMonth=YearMonth.now();
    AvatarController avatarController;
    YearMonth month=YearMonth.now(); int reportYear=LocalDate.now().getYear();
    interface SaveAction { void run() throws Exception; }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);store=new Store(this);prefs=getSharedPreferences("settings",MODE_PRIVATE);
        avatarController=new AvatarController(this,this::render,this::styleDialog);
        prefs.edit().remove("endpoint").remove("packages").apply();
        if(state!=null){summaryIncome=state.getBoolean("summaryIncome");summaryMonth=YearMonth.parse(state.getString("summaryMonth",YearMonth.now().toString()));categoryIncome=state.getBoolean("categoryIncome");allTransactions=state.getBoolean("allTransactions",true);tab=state.getInt("tab");page=state.getString("page","");month=YearMonth.parse(state.getString("month",YearMonth.now().toString()));reportYear=state.getInt("year",reportYear);}
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::navigateBack);
        render();
    }
    @Override protected void onResume(){super.onResume();if(store!=null){store.processRecurring();render();}}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putBoolean("summaryIncome",summaryIncome);state.putString("summaryMonth",summaryMonth.toString());state.putBoolean("categoryIncome",categoryIncome);state.putBoolean("allTransactions",allTransactions);state.putInt("tab",tab);state.putString("page",page);state.putString("month",month.toString());state.putInt("year",reportYear);}
    @Override protected void onDestroy(){avatarController.close();super.onDestroy();store.close();}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        avatarController.handleActivityResult(requestCode,resultCode,data);
    }
    String displayName(){String name=prefs.getString("name","Người dùng").trim();return name.isEmpty()?"Người dùng":name;}
    void navigateBack(){if(!page.isEmpty()){page="";render();}else if(tab!=0){tab=0;render();}else finish();}
    // API 33+ uses the native OnBackInvokedDispatcher registered in onCreate.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){navigateBack();}
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    String cash(long n){return Finance.formatMoney(n)+" ₫";}
    String str(ContentValues v,String k){String s=v.getAsString(k);return s==null?"":s;}
    long num(ContentValues v,String k){Long n=v.getAsLong(k);return n==null?0:n;}
    GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setLineSpacing(dp(3),1);return t;}
    void addText(LinearLayout l,String s,int size,int color,boolean bold){TextView t=text(s,size,color,bold);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);l.addView(t,p);}
    void space(LinearLayout l,int n){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(n)));}
    LinearLayout card(LinearLayout parent){LinearLayout c=col();c.setPadding(dp(18),dp(18),dp(18),dp(14));c.setBackgroundResource(R.drawable.bg_card);c.setElevation(dp(2));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(c,p);return c;}
    Button button(String label,Runnable click,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?Color.WHITE:GREEN);b.setBackgroundResource(primary?R.drawable.bg_primary:R.drawable.bg_card);b.setStateListAnimator(null);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setMinHeight(dp(48));b.setOnClickListener(v->click.run());return b;}
    void action(LinearLayout l,String label,Runnable click,boolean primary){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);l.addView(button(label,click,primary),p);}
    void pair(LinearLayout l,String a,Runnable ar,String b,Runnable br){LinearLayout r=row();LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(0,dp(8),dp(5),dp(4));r.addView(button(a,ar,true),p);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,-2,1);q.setMargins(dp(5),dp(8),0,dp(4));r.addView(button(b,br,false),q);l.addView(r);}
    void heading(String title,String subtitle){addText(body,title,28,INK,true);addText(body,subtitle,13,MUTED,false);space(body,10);}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    void render() {
        String route=tab+":"+page+":"+(page.equals("categories")?categoryIncome:"");
        boolean changed=!renderedRoute.equals(route);
        if(shell==null) {
        setContentView(R.layout.activity_main);
        shell = findViewById(R.id.shell);
        shell.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        shell.requestApplyInsets();
        View panel=findViewById(R.id.nav_panel);
        panel.setOutlineProvider(new ViewOutlineProvider(){
            @Override public void getOutline(View view,android.graphics.Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(26));}
        });
        panel.setClipToOutline(true);
        ((MenuBlur)findViewById(R.id.menu_blur)).bind(findViewById(R.id.scene_root));
        applyGlass(panel);
        findViewById(R.id.nav_add).setOnClickListener(v -> editTransaction(null,false));
        }
        boolean summary=page.equals("summary");
        View scene=findViewById(R.id.scene_root);scene.setBackgroundResource(summary?R.drawable.bg_summary_scene:android.R.color.transparent);
        if(!summary)scene.setBackgroundColor(BG);
        findViewById(R.id.glass_backdrop).setVisibility(summary?View.GONE:View.VISIBLE);
        View decor=getWindow().getDecorView();int flags=decor.getSystemUiVisibility();
        int lightBars=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decor.setSystemUiVisibility(summary?flags&~lightBars:flags|lightBars);
        FrameLayout host = findViewById(R.id.screen_host);
        if(body!=null)body.animate().cancel();
        host.removeAllViews();
        int layout = page.equals("summary") ? R.layout.screen_summary : page.equals("reports") ? R.layout.screen_reports : page.equals("savings_history") ? R.layout.screen_savings_history : page.equals("categories") ? R.layout.screen_categories : page.equals("recurring") ? R.layout.screen_recurring
                : tab == 0 ? R.layout.screen_home : tab == 1 ? R.layout.screen_transactions
                : tab == 2 ? R.layout.screen_savings : R.layout.screen_account;
        body = (LinearLayout) getLayoutInflater().inflate(layout, host, false);
        host.addView(body);
        if (page.equals("summary")) SummaryPage.bind(this);
        else if (page.equals("reports")) reports();
        else if (page.equals("savings_history")) savingsHistory();
        else if (page.equals("categories")) categoriesScreen();
        else if (page.equals("recurring")) recurring();
        else if (tab == 0) home();
        else if (tab == 1) transactionsScreen();
        else if (tab == 2) savings();
        else account();
        applyGlass(body);
        MenuBlur blur=findViewById(R.id.menu_blur);
        if(changed)((ScrollView)findViewById(R.id.content_scroll)).scrollTo(0,0);
        renderedRoute=route;
        blur.post(blur::refresh);
        if(changed)Motion.enter(body,blur::refresh);
        int[] ids = {R.id.nav_home, R.id.nav_transactions, R.id.nav_savings, R.id.nav_account};
        for (int i = 0; i < ids.length; i++) {
            final int selected = i;
            TextView item = findViewById(ids[i]);
            int color=summary?(tab==i?0xff8ce2d2:0xffd0dbee):(tab==i?GREEN:MUTED);
            item.setTextColor(color);
            item.setSelected(tab == i);
            item.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(color));
            item.setBackground(bg(tab==i?0x3033b8ad:Color.TRANSPARENT,18));
            Motion.press(item);
            item.setOnClickListener(v -> { tab = selected; page = ""; render(); });
        }
    }

    void label(int id, String value) { ((TextView) body.findViewById(id)).setText(value); }
    void click(int id, Runnable action) {
        View view=body.findViewById(id);view.setOnClickListener(v->action.run());
        if(id==R.id.income_total || id==R.id.expense_total)
            ((View)view.getParent()).setOnClickListener(v->action.run());
    }
    LinearLayout list(int id) { LinearLayout l = body.findViewById(id); l.removeAllViews(); return l; }

    void applyGlass(View view) {
        if(view instanceof Button) {
            Button button=(Button)view;
            boolean primary=button.getCurrentTextColor()==Color.WHITE;
            FrostedDrawable glass=new FrostedDrawable(button,primary);
            GradientDrawable mask=bg(Color.WHITE,20);
            button.setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x30347c89),glass,mask));
            Motion.press(button);
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)applyGlass(((ViewGroup)view).getChildAt(i));
    }

    String percentage(long value,long total) {
        return String.format(Locale.forLanguageTag("vi-VN"),"%.1f%%",total==0?0:value*100.0/total);
    }

    void drawCategoryChart() {
        long expense=store.sum(month.toString(),false);
        label(R.id.category_month_total,"Tổng chi · "+cash(expense));
        LinearLayout host=list(R.id.category_chart);
        Map<String,Long> totals=new HashMap<>();
        for(ContentValues value:store.rows("tx","day DESC,id DESC"))
            if(num(value,"income")==0 && str(value,"day").startsWith(month.toString()))
                totals.merge(str(value,"category"),num(value,"amount"),Long::sum);
        if(totals.isEmpty()) {addText(host,"Chưa có chi tiêu trong tháng này.",14,MUTED,false);return;}
        List<Map.Entry<String,Long>> sorted=new ArrayList<>(totals.entrySet());
        sorted.sort((a,b)->{int n=Long.compare(b.getValue(),a.getValue());return n!=0?n:a.getKey().compareTo(b.getKey());});
        long maximum=sorted.get(0).getValue();
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(true);
        LinearLayout columns=row();columns.setGravity(Gravity.BOTTOM);
        int width=Math.max(dp(76),(getResources().getDisplayMetrics().widthPixels-dp(64))/Math.min(4,sorted.size()));
        int[] colors={GREEN,0xff7170a8,0xffb4785f,0xff568b71,0xff5478a7};int index=0;
        for(Map.Entry<String,Long> entry:sorted) {
            String category=entry.getKey();long amount=entry.getValue();String share=percentage(amount,expense);
            LinearLayout group=col();group.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView percent=text(share,12,INK,true);percent.setGravity(Gravity.CENTER);group.addView(percent);
            BarColumn column=new BarColumn(this,amount,maximum,colors[index++%colors.length]);
            group.addView(column,new LinearLayout.LayoutParams(dp(56),dp(114)));
            TextView name=text(category,12,INK,false);name.setGravity(Gravity.CENTER);name.setMaxLines(2);
            group.addView(name,new LinearLayout.LayoutParams(-1,dp(40)));
            group.setTag("category:"+category);group.setFocusable(true);group.setClickable(true);
            String description=category+": "+cash(amount)+", "+share+" tổng chi tháng "+month.getMonthValue()+"/"+month.getYear();
            group.setContentDescription(description);column.setContentDescription(description);
            group.setOnClickListener(v->showCategoryDetails(category));column.setOnClickListener(v->showCategoryDetails(category));
            columns.addView(group,new LinearLayout.LayoutParams(width,-2));
        }
        scroll.addView(columns);host.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
    }

    AlertDialog showCategoryDetails(String category) {
        long total=store.sum(month.toString(),false),amount=0;
        LinearLayout content=form(),entries=col();int count=0;
        for(ContentValues value:store.rows("tx","day DESC,id DESC"))
            if(num(value,"income")==0 && str(value,"day").startsWith(month.toString()) && str(value,"category").equals(category)) {
                amount+=num(value,"amount");count++;
                LinearLayout entry=card(entries);addText(entry,str(value,"note"),15,INK,true);
                addText(entry,cash(num(value,"amount"))+" · "+LocalDate.parse(str(value,"day")).format(DATE),13,MUTED,false);
            }
        addText(content,cash(amount),26,GREEN,true);
        addText(content,percentage(amount,total)+" tổng chi tháng "+month.getMonthValue()+"/"+month.getYear(),17,INK,true);
        addText(content,"Tổng chi tháng: "+cash(total)+" · "+count+" giao dịch",12,MUTED,false);
        if(count==0)addText(content,"Chưa có chi tiêu trong danh mục này.",14,MUTED,false);
        content.addView(entries);return showInfo(category,content);
    }

    void openSummary(){summaryMonth=month;open("summary");}

    void home() {
        click(R.id.open_summary,this::openSummary);
        label(R.id.home_name,displayName());
        avatarController.bind(body.findViewById(R.id.home_avatar),displayName());
        label(R.id.period, "Tháng " + month.getMonthValue() + " / " + month.getYear());
        click(R.id.previous, () -> { month = month.minusMonths(1); render(); });
        click(R.id.next, () -> { month = month.plusMonths(1); render(); });
        long income = store.sum(month.toString(), true), expense = store.sum(month.toString(), false);
        long carryover=store.balanceBefore(month.atDay(1));
        label(R.id.balance, cash(carryover + income - expense));
        label(R.id.carryover,"Từ các tháng trước: "+cash(carryover));
        label(R.id.income_total, cash(income));
        label(R.id.expense_total, cash(expense));
        click(R.id.income_total,()->showDetails("Thu tháng "+month.getMonthValue(),month.toString(),true,false));
        click(R.id.expense_total,()->showDetails("Chi tháng "+month.getMonthValue(),month.toString(),false,false));
        click(R.id.carryover,()->showDetails("Số dư từ các tháng trước",month.atDay(1).toString(),null,true));
        click(R.id.balance,()->showDetails("Số dư lũy kế",month.plusMonths(1).atDay(1).toString(),null,true));
        drawChart(R.id.home_chart,month.minusMonths(5),6,true);
        body.findViewById(R.id.home_chart_card).setVisibility(trendExpanded?View.VISIBLE:View.GONE);
        label(R.id.toggle_trend,trendExpanded?"Dòng tiền 6 tháng ↑":"Dòng tiền 6 tháng ↓");
        click(R.id.toggle_trend,()->{trendExpanded=!trendExpanded;render();});
        click(R.id.add_expense, () -> editTransaction(null, false));
        click(R.id.add_income, () -> editTransaction(null, true));
        click(R.id.open_recurring, () -> open("recurring"));
        long budget = prefs.getLong("budget", 0);
        LinearLayout budgetBox = list(R.id.budget_box);
        if (budget > 0) {
            LinearLayout c = card(budgetBox);
            addText(c, "Ngân sách tháng", 16, INK, true);
            progress(c, expense, budget);
            addText(c, cash(expense) + " / " + cash(budget), 14, expense > budget ? RED : MUTED, false);
        }
        drawCategoryChart();
        click(R.id.view_transactions,()->{tab=1;allTransactions=false;filter="Tất cả";query="";render();});
    }

    void transactionsScreen() {
        label(R.id.period,"Tháng "+month.getMonthValue()+" / "+month.getYear());
        click(R.id.previous,()->{month=month.minusMonths(1);allTransactions=false;render();});
        click(R.id.next,()->{month=month.plusMonths(1);allTransactions=false;render();});
        CheckBox all=body.findViewById(R.id.all_months);
        all.setChecked(allTransactions);
        all.setOnCheckedChangeListener((button,checked)->{allTransactions=checked;render();});
        body.findViewById(R.id.transaction_period).setVisibility(allTransactions?View.GONE:View.VISIBLE);
        List<ContentValues> transactions=store.rows("tx","day DESC,id DESC");
        EditText search = body.findViewById(R.id.search);
        search.setText(query);
        click(R.id.search_button, () -> { query = search.getText().toString().trim(); render(); });
        ArrayList<String> filters = new ArrayList<>(Arrays.asList("Tất cả", "Thu nhập"));
        filters.addAll(Arrays.asList(store.categories()));
        for(String category:store.categories(true))filters.add("Thu · "+category);
        Spinner f = body.findViewById(R.id.filter);
        f.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters));
        f.setSelection(Math.max(0, filters.indexOf(filter)));
        f.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) {}
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String next = filters.get(pos);
                if (!next.equals(filter)) { filter = next; render(); }
            }
        });
        LinearLayout entries = list(R.id.transactions);
        Map<String,List<ContentValues>> days=new LinkedHashMap<>();
        for (ContentValues v : transactions) {
            if (!allTransactions && !str(v,"day").startsWith(month.toString())) continue;
            if (!query.isEmpty() && !str(v,"note").toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            if(!filter.equals("Tất cả")) {
                boolean inc=num(v,"income")==1;
                boolean matches=filter.equals("Thu nhập")?inc:filter.startsWith("Thu · ")?inc&&filter.substring(6).equals(str(v,"category")):!inc&&filter.equals(str(v,"category"));
                if(!matches)continue;
            }
            days.computeIfAbsent(str(v,"day"),key->new ArrayList<>()).add(v);
        }
        if(days.isEmpty())addText(entries,"Chưa có giao dịch phù hợp.",14,MUTED,false);
        for(Map.Entry<String,List<ContentValues>> day:days.entrySet()) {
            LinearLayout group=card(entries);group.setPadding(dp(14),dp(14),dp(14),dp(4));
            group.setTag("day:"+day.getKey());
            long totalIn=0,totalOut=0;
            for(ContentValues value:day.getValue())if(num(value,"income")==1)totalIn+=num(value,"amount");else totalOut+=num(value,"amount");
            LocalDate date=LocalDate.parse(day.getKey());
            String title=date.equals(LocalDate.now())?"Hôm nay":date.equals(LocalDate.now().minusDays(1))?"Hôm qua":date.format(DateTimeFormatter.ofPattern("EEEE",Locale.forLanguageTag("vi-VN")));
            addText(group,title+" · "+date.format(DATE),14,INK,true);
            TextView summary=text("Thu "+cash(totalIn)+"  ·  Chi "+cash(totalOut),12,MUTED,false);
            summary.setTag("day-total:"+day.getKey());group.addView(summary);space(group,4);
            for(ContentValues value:day.getValue())transactionRow(group,value);
        }
    }
    void progress(LinearLayout l,long value,long total){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(1000);p.setProgress(total<=0?0:(int)Math.min(1000,(double)value/total*1000));p.setProgressTintList(android.content.res.ColorStateList.valueOf(value>total?RED:GREEN));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(8));lp.bottomMargin=dp(10);l.addView(p,lp);}
    void empty(String title,String detail){LinearLayout c=card(body);addText(c,title,16,INK,true);addText(c,detail,13,MUTED,false);}
    void transactionRow(LinearLayout parent,ContentValues v) {
        View divider=new View(this);divider.setBackgroundColor(0x1439586c);parent.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        LinearLayout entry=col();entry.setPadding(0,dp(12),0,dp(12));entry.setMinimumHeight(dp(64));
        entry.setTag("tx:"+num(v,"id"));boolean income=num(v,"income")==1;
        addText(entry,str(v,"note"),15,INK,true);
        ((TextView)entry.getChildAt(0)).setMaxLines(2);
        ((TextView)entry.getChildAt(0)).setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout metadata=row();TextView category=text(str(v,"category"),12,MUTED,false);
        category.setMaxLines(2);metadata.addView(category,new LinearLayout.LayoutParams(0,-2,1));
        TextView amount=text((income?"+":"−")+cash(num(v,"amount")),15,income?GREEN:RED,true);
        metadata.addView(amount);entry.addView(metadata);
        entry.setBackgroundResource(android.R.drawable.list_selector_background);
        entry.setFocusable(true);entry.setOnClickListener(view->editTransaction(v,income));parent.addView(entry);
    }
    void open(String target){page=target;render();}
    LinearLayout form(){LinearLayout f=col();f.setPadding(dp(22),dp(8),dp(22),dp(16));return f;}
    EditText field(LinearLayout l,String label,String value,boolean number){addText(l,label,12,MUTED,true);EditText e=new EditText(this);e.setText(value);e.setTextSize(16);e.setSingleLine(true);e.setInputType(number?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);l.addView(e,new LinearLayout.LayoutParams(-1,dp(52)));space(l,10);if(number)MoneyInput.attach(e);return e;}
    Spinner spinner(LinearLayout l,String label,String[] options,String selected){addText(l,label,12,MUTED,true);Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options);s.setAdapter(a);int i=Arrays.asList(options).indexOf(selected);s.setSelection(Math.max(0,i));l.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));space(l,10);return s;}
    Button date(LinearLayout l,String label,LocalDate initial){addText(l,label,12,MUTED,true);Button b=button(initial.format(DATE),()->{},false);b.setTag(initial);b.setOnClickListener(v->{LocalDate old=(LocalDate)b.getTag();new DatePickerDialog(this,(p,y,m,d)->{LocalDate chosen=LocalDate.of(y,m+1,d);b.setTag(chosen);b.setText(chosen.format(DATE));},old.getYear(),old.getMonthValue()-1,old.getDayOfMonth()).show();});l.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));space(l,12);return b;}
    AlertDialog dialog(String title, LinearLayout form, String positive, SaveAction save, Runnable delete) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title).setView(scroll).setNegativeButton("Hủy", null)
                .setPositiveButton(positive, null);
        if (delete != null) builder.setNeutralButton("Xóa", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        styleDialog(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                save.run();
                dialog.dismiss();
                render();
            } catch (Exception e) {
                toast(e.getMessage() == null ? "Không thể lưu dữ liệu." : e.getMessage());
            }
        });
        if (delete != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                new AlertDialog.Builder(this).setTitle("Xóa mục này?")
                        .setMessage("Thao tác này không thể hoàn tác.")
                        .setNegativeButton("Giữ lại", null)
                        .setPositiveButton("Xóa", (d, which) -> {
                            delete.run(); dialog.dismiss(); render();
                        }).show());
        return dialog;
    }
    String required(EditText e,String name){String s=e.getText().toString().trim();if(s.isEmpty())throw new IllegalArgumentException("Vui lòng nhập "+name+".");if(s.length()>300)throw new IllegalArgumentException(name+" tối đa 300 ký tự.");return s;}
    AlertDialog editTransaction(ContentValues existing, boolean income) {
        LinearLayout f = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_transaction, null);
        Spinner type = f.findViewById(R.id.transaction_type);
        type.setSelection(income ? 1 : 0);
        EditText amount = f.findViewById(R.id.transaction_amount);
        MoneyInput.attach(amount);
        EditText note = f.findViewById(R.id.transaction_note);
        Spinner category = f.findViewById(R.id.transaction_category);
        bindCategoryType(type,category,existing,income);
        if (existing != null) {
            amount.setText(str(existing,"amount"));
            note.setText(str(existing,"note"));

        }
        Button day = f.findViewById(R.id.transaction_date);
        LocalDate initial = existing == null ? LocalDate.now() : LocalDate.parse(str(existing,"day"));
        day.setTag(initial);
        day.setText(initial.format(DATE));
        day.setOnClickListener(v -> {
            LocalDate old = (LocalDate) day.getTag();
            new DatePickerDialog(this, (p,y,m,d) -> {
                LocalDate chosen = LocalDate.of(y,m+1,d);
                day.setTag(chosen); day.setText(chosen.format(DATE));
            },old.getYear(),old.getMonthValue()-1,old.getDayOfMonth()).show();
        });
        return dialog(existing == null ? "Thêm giao dịch" : "Sửa giao dịch", f, "Lưu", () -> {
            long value = Finance.money(amount.getText().toString());
            String memo = required(note,"ghi chú");
            boolean inc = type.getSelectedItemPosition() == 1;
            LocalDate chosen = (LocalDate) day.getTag();
            if (chosen.isAfter(LocalDate.now())) throw new IllegalArgumentException("Ngày giao dịch không được ở tương lai.");
            String cat = category.getSelectedItem().toString();
            store.transaction(existing == null ? null : num(existing,"id"),value,inc,memo,cat,chosen.toString(),"Nhập tay",null);
            toast("Đã lưu giao dịch");
        }, existing == null ? null : () -> store.delete("tx",num(existing,"id")));
    }

    void bindCategoryType(Spinner type,Spinner category,ContentValues existing,boolean initialIncome) {
        final Map<Boolean,String> selections=new HashMap<>();
        selections.put(false,"Khác");selections.put(true,"Lương");
        selections.put(initialIncome,existing==null?(initialIncome?"Lương":"Khác"):str(existing,"category"));
        final boolean[] current={initialIncome};
        Runnable populate=()->{
            String[] names=store.categories(current[0]);
            category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
            category.setSelection(Math.max(0,Arrays.asList(names).indexOf(selections.get(current[0]))));
            category.setEnabled(true);
        };
        populate.run();
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                boolean next=position==1;if(next==current[0])return;
                selections.put(current[0],category.getSelectedItem().toString());current[0]=next;populate.run();
            }
        });
    }

    void recurring() {
        click(R.id.back_home, () -> { page = ""; render(); });
        click(R.id.add_recurring, () -> editRecurring(null));
        LinearLayout entries = list(R.id.recurring_list);
        List<ContentValues> schedules = store.rows("recurring","due ASC");
        if (schedules.isEmpty()) addText(entries,"Chưa có lịch định kỳ.",14,MUTED,false);
        for (ContentValues v : schedules) {
            LinearLayout c = card(entries);
            addText(c,str(v,"note"),18,INK,true);
            addText(c,(num(v,"income")==1 ? "Thu " : "Chi ")+cash(num(v,"amount"))+" / tháng",16,GREEN,true);
            addText(c,"Lần tới: "+LocalDate.parse(str(v,"due")).format(DATE),13,MUTED,false);
            action(c,"Chỉnh sửa lịch",()->editRecurring(v),false);
        }
    }
    AlertDialog editRecurring(ContentValues v){LinearLayout f=form();EditText note=field(f,"Tên khoản định kỳ",v==null?"":str(v,"note"),false);EditText amount=field(f,"Số tiền (VNĐ)",v==null?"":str(v,"amount"),true);Spinner type=spinner(f,"Loại",new String[]{"Chi tiêu","Thu nhập"},v!=null&&num(v,"income")==1?"Thu nhập":"Chi tiêu");Spinner cat=spinner(f,"Danh mục",store.categories(v!=null&&num(v,"income")==1),v==null?"Khác":str(v,"category"));bindCategoryType(type,cat,v,v!=null&&num(v,"income")==1);type.setId(R.id.transaction_type);cat.setId(R.id.transaction_category);note.setId(R.id.transaction_note);amount.setId(R.id.transaction_amount);Button due=date(f,"Ngày bắt đầu / lần tiếp theo",v==null?LocalDate.now():LocalDate.parse(str(v,"due")));addText(f,"Ngày 29–31 sẽ chuyển về ngày cuối tháng nếu tháng đó ngắn hơn. Xóa lịch không xóa giao dịch đã ghi nhận.",12,MUTED,false);
        return dialog("Lịch hàng tháng",f,"Lưu lịch",()->{long a=Finance.money(amount.getText().toString());String n=required(note,"tên khoản");LocalDate d=(LocalDate)due.getTag();if(d.isBefore(LocalDate.now()))throw new IllegalArgumentException("Chọn hôm nay hoặc ngày trong tương lai.");ContentValues c=new ContentValues();if(v!=null)c.put("id",num(v,"id"));c.put("amount",a);c.put("note",n);c.put("income",type.getSelectedItemPosition());c.put("category",cat.getSelectedItem().toString());c.put("due",d.toString());c.put("anchor",v!=null&&d.toString().equals(str(v,"due"))?num(v,"anchor"):d.getDayOfMonth());store.save("recurring",c);store.processRecurring();},v==null?null:()->store.delete("recurring",num(v,"id")));
    }
    void savings() {
        click(R.id.open_savings_history,()->open("savings_history"));
        click(R.id.add_goal, () -> editGoal(null));
        List<ContentValues> goals = store.rows("goals","deadline ASC");
        long total = 0;
        for (ContentValues v : goals) total += num(v,"saved");
        label(R.id.saved_total,cash(total));
        label(R.id.goal_count,goals.size()+" mục tiêu");
        LinearLayout entries = list(R.id.goal_list);
        if (goals.isEmpty()) addText(entries,"Chưa có mục tiêu. Tạo mục tiêu đầu tiên của bạn.",15,MUTED,false);
        for (ContentValues v : goals) {
            LinearLayout c = card(entries);
            long target = num(v,"target"), saved = num(v,"saved");
            addText(c,str(v,"name"),20,INK,true);
            addText(c,cash(saved)+" / "+cash(target),15,GREEN,true);
            progress(c,saved,target);
            addText(c,"Hạn: "+LocalDate.parse(str(v,"deadline")).format(DATE)+(saved>=target ? " · Hoàn thành" : ""),13,MUTED,false);
            c.setOnClickListener(view->showGoalDetails(v));
            pair(c,"Góp / rút tiền",()->contribute(v),"Sửa mục tiêu",()->editGoal(v));
        }
    }
    void editGoal(ContentValues v){LinearLayout f=form();EditText name=field(f,"Tên mục tiêu",v==null?"":str(v,"name"),false);EditText target=field(f,"Số tiền mục tiêu (VNĐ)",v==null?"":str(v,"target"),true);Button end=date(f,"Ngày dự kiến hoàn thành",v==null?LocalDate.now().plusMonths(6):LocalDate.parse(str(v,"deadline")));dialog("Mục tiêu tiết kiệm",f,"Lưu",()->{String n=required(name,"tên mục tiêu");long t=Finance.money(target.getText().toString());long s=v==null?0:num(v,"saved");if(t<s)throw new IllegalArgumentException("Mục tiêu không được thấp hơn số tiền đã góp.");LocalDate d=(LocalDate)end.getTag();if(d.isBefore(LocalDate.now()))throw new IllegalArgumentException("Ngày hoàn thành phải từ hôm nay.");ContentValues c=new ContentValues();if(v!=null)c.put("id",num(v,"id"));c.put("name",n);c.put("target",t);c.put("saved",s);c.put("deadline",d.toString());store.save("goals",c);},v==null?null:()->store.delete("goals",num(v,"id")));}
    void contribute(ContentValues v){LinearLayout f=form();EditText amount=field(f,"Số tiền (VNĐ)","",true);Spinner mode=spinner(f,"Thao tác",new String[]{"Góp thêm","Rút khỏi mục tiêu"},"Góp thêm");addText(f,"Đây là phân bổ tiền vào mục tiêu, không tạo khoản chi hoặc khoản thu mới.",13,MUTED,false);dialog("Cập nhật tiết kiệm",f,"Xác nhận",()->{long a=Finance.money(amount.getText().toString());store.contribute(num(v,"id"),mode.getSelectedItemPosition()==0?a:-a);},null);}
    void savingsHistory() {
        click(R.id.history_back,()->{page="";render();});
        LinearLayout entries=list(R.id.history_list);
        List<ContentValues> history=store.rows("savings_history","id DESC");
        Map<Long,String> goals=new HashMap<>();
        for(ContentValues goal:store.rows("goals","id"))goals.put(num(goal,"id"),str(goal,"name"));
        if(history.isEmpty())addText(entries,"Chưa có lịch sử. Góp tiền vào một mục tiêu để bắt đầu.",15,MUTED,false);
        for(ContentValues v:history) {
            LinearLayout c=card(entries);
            String name=goals.get(num(v,"goal_id"));
            addText(c,name==null?str(v,"goal_name")+" · Đã xóa mục tiêu":name,18,INK,true);
            long amount=num(v,"amount");boolean opening=str(v,"kind").equals("opening");
            addText(c,(opening?"Số dư từ bản trước: ":amount>0?"Góp thêm: +":"Rút tiền: −")+cash(Math.abs(amount)),16,amount>=0?GREEN:RED,true);
            String time=LocalDateTime.parse(str(v,"created")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            addText(c,(opening?"Ghi nhận khi cập nhật · ":"")+time,13,MUTED,false);
            addText(c,"Số dư sau ghi nhận: "+cash(num(v,"balance")),13,MUTED,false);
        }
    }

    void reports() {
        click(R.id.reports_back,()->{page="";render();});
        label(R.id.period,"Năm "+reportYear);
        click(R.id.previous,()->{reportYear--;render();});
        click(R.id.next,()->{reportYear++;render();});
        long income=store.sum(""+reportYear,true), expense=store.sum(""+reportYear,false);
        long last=store.sum(""+(reportYear-1),false);
        label(R.id.year_expense,cash(expense));
        label(R.id.year_income,"Tổng thu: "+cash(income));
        label(R.id.year_balance,"Còn lại: "+cash(income-expense));
        label(R.id.year_change,last==0 ? "Chưa có chi tiêu năm trước để so sánh." : String.format(Locale.forLanguageTag("vi-VN"),"%+.1f%% so với năm trước",(expense-last)*100.0/last));
        long[] values=new long[12];
        for(int i=0;i<12;i++)values[i]=store.sum(YearMonth.of(reportYear,i+1).toString(),false);
        drawChart(R.id.year_chart,YearMonth.of(reportYear,1),12,false);
        click(R.id.year_expense,()->showDetails("Chi tiêu năm "+reportYear,""+reportYear,false,false));
        click(R.id.year_income,()->showDetails("Thu nhập năm "+reportYear,""+reportYear,true,false));
        click(R.id.year_balance,()->showDetails("Chênh lệch năm "+reportYear,""+reportYear,null,false));
        Spinner a=body.findViewById(R.id.compare_a), b=body.findViewById(R.id.compare_b);
        a.setSelection(Math.max(0,LocalDate.now().getMonthValue()-2));
        b.setSelection(LocalDate.now().getMonthValue()-1);
        AdapterView.OnItemSelectedListener listener=new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                long x=values[a.getSelectedItemPosition()],y=values[b.getSelectedItemPosition()];
                label(R.id.comparison,cash(x)+" → "+cash(y)+"\nChênh lệch: "+cash(y-x));
            }
        };
        a.setOnItemSelectedListener(listener);b.setOnItemSelectedListener(listener);
    }

    AlertDialog detailDialog;
    void styleDialog(AlertDialog dialog) {
        applyGlass(dialog.getWindow().getDecorView());
        Window window=dialog.getWindow();
        if(window!=null) {
            window.setWindowAnimations(Motion.enabled()?R.style.GlassDialogMotion:0);
            window.setBackgroundDrawable(new DialogBlurDrawable(findViewById(R.id.scene_root),window.getDecorView()));
            window.setDimAmount(.22f);
            window.setLayout(getResources().getDisplayMetrics().widthPixels-dp(32),WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    void drawChart(int hostId, YearMonth start, int count, boolean showLatest) {
        LinearLayout host=list(hostId);
        TextView legend=body.findViewById(hostId==R.id.home_chart?R.id.home_chart_legend:R.id.year_chart_legend);
        android.text.SpannableString colored=new android.text.SpannableString("● Thu nhập   ● Chi tiêu");
        colored.setSpan(new android.text.style.ForegroundColorSpan(GREEN),0,10,0);
        colored.setSpan(new android.text.style.ForegroundColorSpan(RED),13,colored.length(),0);
        legend.setText(colored);
        long[][] amounts=new long[count][2];long max=0;
        for(int i=0;i<count;i++)for(int series=0;series<2;series++) {
            amounts[i][series]=store.sum(start.plusMonths(i).toString(),series==0);
            max=Math.max(max,amounts[i][series]);
        }
        addText(host,max==0?"Chưa có dữ liệu trong giai đoạn này.":"Đỉnh thang đo: "+cash(max),11,MUTED,false);
        HorizontalScrollView scroll=new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(true);
        LinearLayout months=row();months.setGravity(Gravity.BOTTOM);
        for(int i=0;i<count;i++) {
            YearMonth selected=start.plusMonths(i);
            LinearLayout group=col(),columns=row();
            for(int series=0;series<2;series++) {
                boolean income=series==0;long amount=amounts[i][series];
                String title=(income?"Thu nhập":"Chi tiêu")+" · "+selected.getMonthValue()+"/"+selected.getYear();
                BarColumn column=new BarColumn(this,amount,max,income?GREEN:RED);
                column.setTag(selected+":"+(income?"income":"expense"));
                column.setContentDescription(title+": "+cash(amount)+". Chạm để xem giao dịch.");
                column.setOnClickListener(v->showDetails(title,selected.toString(),income,false));
                columns.addView(column,new LinearLayout.LayoutParams(dp(48),dp(136)));
            }
            group.addView(columns);
            TextView monthLabel=text(String.format(Locale.ROOT,"%02d/%02d",selected.getMonthValue(),selected.getYear()%100),11,MUTED,false);
            monthLabel.setGravity(Gravity.CENTER);group.addView(monthLabel);
            months.addView(group,new LinearLayout.LayoutParams(dp(96),-2));
        }
        scroll.addView(months);host.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        addText(host,"Vuốt ngang · chạm cột để xem chi tiết",11,MUTED,false);
        if(showLatest)scroll.post(()->scroll.fullScroll(View.FOCUS_RIGHT));
    }

    AlertDialog showDetails(String title,String prefix,Boolean income,boolean before) {
        LinearLayout content=form();long totalIn=0,totalOut=0;int count=0;
        LinearLayout entries=col();
        for(ContentValues value:store.rows("tx","day DESC,id DESC")) {
            String day=str(value,"day");boolean isIncome=num(value,"income")==1;
            if(before ? day.compareTo(prefix)>=0 : !day.startsWith(prefix))continue;
            if(income!=null && income!=isIncome)continue;
            long amount=num(value,"amount");if(isIncome)totalIn+=amount;else totalOut+=amount;count++;
            LinearLayout entry=card(entries);
            addText(entry,str(value,"note"),15,INK,true);
            addText(entry,(isIncome?"+":"−")+cash(amount),16,isIncome?GREEN:RED,true);
            addText(entry,str(value,"category")+" · "+LocalDate.parse(day).format(DATE),12,MUTED,false);
        }
        addText(content,cash(income==null?totalIn-totalOut:income?totalIn:totalOut),28,income!=null&&!income?RED:GREEN,true);
        if(income==null)addText(content,"Thu: "+cash(totalIn)+"\nChi: "+cash(totalOut),14,MUTED,false);
        addText(content,count+" giao dịch",13,MUTED,false);
        if(count==0)addText(content,"Chưa có giao dịch trong phần này.",14,MUTED,false);
        content.addView(entries);
        return showInfo(title,content);
    }

    AlertDialog showInfo(String title,LinearLayout content) {
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        detailDialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("Đóng",null).create();
        detailDialog.show();styleDialog(detailDialog);return detailDialog;
    }

    void showGoalDetails(ContentValues goal) {
        LinearLayout content=form();
        addText(content,cash(num(goal,"saved"))+" / "+cash(num(goal,"target")),22,GREEN,true);
        addText(content,"Hạn mục tiêu: "+LocalDate.parse(str(goal,"deadline")).format(DATE),13,MUTED,false);
        addText(content,"Lịch sử của mục tiêu",17,INK,true);int count=0;
        for(ContentValues item:store.rows("savings_history","id DESC"))if(num(item,"goal_id")==num(goal,"id")) {
            long amount=num(item,"amount");count++;
            String kind=str(item,"kind").equals("opening")?"Số dư từ bản trước: ":amount>0?"Góp thêm: +":"Rút tiền: −";
            LinearLayout c=card(content);addText(c,kind+cash(Math.abs(amount)),15,amount>=0?GREEN:RED,true);
            addText(c,LocalDateTime.parse(str(item,"created")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),12,MUTED,false);
            addText(c,"Số dư: "+cash(num(item,"balance")),13,MUTED,false);
        }
        if(count==0)addText(content,"Chưa có lần góp tiền nào.",14,MUTED,false);
        showInfo(str(goal,"name"),content);
    }

    void categoriesScreen() {
        label(R.id.categories_description,categoryIncome?"Phân loại nguồn thu để theo dõi tiền nhận được.":"Phân loại chi tiêu để biết tiền được dùng vào đâu.");
        Button expenses=body.findViewById(R.id.category_expenses),incomes=body.findViewById(R.id.category_incomes);
        expenses.setTextColor(categoryIncome?GREEN:Color.WHITE);incomes.setTextColor(categoryIncome?Color.WHITE:GREEN);
        expenses.setSelected(!categoryIncome);incomes.setSelected(categoryIncome);
        click(R.id.category_expenses,()->{if(categoryIncome){categoryIncome=false;render();}});
        click(R.id.category_incomes,()->{if(!categoryIncome){categoryIncome=true;render();}});
        click(R.id.categories_back, () -> { page=""; render(); });
        click(R.id.category_add, () -> editCategory(null));
        LinearLayout entries=list(R.id.category_list);
        for(String name:store.categories(categoryIncome)) {
            LinearLayout c=card(entries);
            addText(c,name,18,INK,true);
            addText(c,store.categoryUsage(name,categoryIncome)+" giao dịch / lịch định kỳ đang dùng",13,MUTED,false);
            pair(c,"Đổi tên",()->editCategory(name),"Xóa",()->deleteCategory(name));
        }
    }

    AlertDialog editCategory(String previous) {
        LinearLayout f=(LinearLayout)getLayoutInflater().inflate(R.layout.dialog_category,null);
        EditText name=f.findViewById(R.id.category_name);
        if(previous!=null)name.setText(previous);
        boolean income=categoryIncome;
        return dialog((previous==null?"Thêm danh mục ":"Đổi tên danh mục ")+(income?"thu nhập":"chi tiêu"),f,"Lưu",()->{
            store.saveCategory(previous,name.getText().toString(),income);
            filter="Tất cả";
        },null);
    }

    void deleteCategory(String name) {
        boolean income=categoryIncome;
        if(store.categories(income).length<2){toast("Cần giữ ít nhất một danh mục "+(income?"thu nhập.":"chi tiêu."));return;}
        LinearLayout f=form();
        long usage=store.categoryUsage(name,income);
        addText(f,"Xóa danh mục “"+name+"”?",17,INK,true);
        ArrayList<String> choices=new ArrayList<>(Arrays.asList(store.categories(income)));
        choices.remove(name);
        Spinner replacement;
        if(usage>0) {
            addText(f,usage+" giao dịch / lịch định kỳ sẽ được chuyển sang danh mục bạn chọn. Số tiền không thay đổi.",14,MUTED,false);
            replacement=spinner(f,"Danh mục thay thế",choices.toArray(new String[0]),choices.get(0));
        } else {
            replacement=null;
            addText(f,"Danh mục này chưa được sử dụng.",14,MUTED,false);
        }
        dialog("Xóa danh mục",f,"Xóa",()->{
            store.deleteCategory(name,replacement==null?null:replacement.getSelectedItem().toString(),income);
            filter="Tất cả";
        },null);
    }

    void account() {
        click(R.id.open_summary,this::openSummary);
        click(R.id.open_reports,()->open("reports"));
        click(R.id.manage_categories, () -> {categoryIncome=false;open("categories");});
        click(R.id.manage_income_categories, () -> {categoryIncome=true;open("categories");});
        click(R.id.account_recurring,()->open("recurring"));
        label(R.id.profile_name,displayName());
        long budget=prefs.getLong("budget",0);
        label(R.id.profile_budget,budget==0 ? "Đặt ngân sách" : cash(budget));
        long spent=store.sum(YearMonth.now().toString(),false);
        label(R.id.budget_note,budget==0?"Chạm để đặt hạn mức chi mỗi tháng": "Đã chi "+cash(spent)+" trong tháng này");
        avatarController.bind(body.findViewById(R.id.profile_avatar),displayName());
        click(R.id.change_avatar,avatarController::showChooser);
        label(R.id.expense_category_count,store.categories().length+" danh mục chi tiêu");
        label(R.id.income_category_count,store.categories(true).length+" nguồn thu nhập");
        label(R.id.data_count,store.rows("tx","id").size()+" giao dịch · "+store.rows("goals","id").size()+" mục tiêu");
        click(R.id.edit_profile,this::profileDialog);
        click(R.id.profile_budget,this::profileDialog);
        click(R.id.data_count,()->{tab=1;page="";allTransactions=true;filter="Tất cả";query="";render();});
    }
    void profileDialog(){LinearLayout f=form();EditText name=field(f,"Tên hiển thị",displayName(),false),budget=field(f,"Ngân sách chi mỗi tháng (0 để tắt)",""+prefs.getLong("budget",0),true);dialog("Hồ sơ cá nhân",f,"Lưu",()->{String n=required(name,"tên");String raw=budget.getText().toString().trim();long b=raw.matches("0+")?0:Finance.money(raw);prefs.edit().putString("name",n).putLong("budget",b).apply();},null);}
}
