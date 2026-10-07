package vn.sotay.thuchi;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.time.*;
import java.util.*;

public final class Store extends SQLiteOpenHelper {
    public Store(Context c) { this(c,"finance.db"); }
    Store(Context c,String databaseName) { super(c,databaseName,null,5); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tx(id INTEGER PRIMARY KEY AUTOINCREMENT,amount INTEGER NOT NULL CHECK(amount>0),income INTEGER NOT NULL,note TEXT NOT NULL,category TEXT NOT NULL,day TEXT NOT NULL,source TEXT NOT NULL,unique_key TEXT UNIQUE)");
        db.execSQL("CREATE TABLE recurring(id INTEGER PRIMARY KEY AUTOINCREMENT,amount INTEGER NOT NULL,income INTEGER NOT NULL,note TEXT NOT NULL,category TEXT NOT NULL,due TEXT NOT NULL,anchor INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE goals(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,target INTEGER NOT NULL,saved INTEGER NOT NULL,deadline TEXT NOT NULL)");
        createCategories(db);
        createSavingsHistory(db);
        createIncomeCategories(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV) {
        if(oldV<2)db.execSQL("DROP TABLE IF EXISTS inbox");
        if(oldV<3)createCategories(db);
        if(oldV<4)createSavingsHistory(db);
        if(oldV<5)createIncomeCategories(db);
    }
    private void createSavingsHistory(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS savings_history(id INTEGER PRIMARY KEY AUTOINCREMENT,goal_id INTEGER NOT NULL,goal_name TEXT NOT NULL,amount INTEGER NOT NULL,balance INTEGER NOT NULL,created TEXT NOT NULL,kind TEXT NOT NULL)");
        // Existing balances are opening balances, not invented historical deposits.
        db.execSQL("INSERT INTO savings_history(goal_id,goal_name,amount,balance,created,kind) SELECT id,name,saved,saved,?,'opening' FROM goals WHERE saved>0 AND NOT EXISTS(SELECT 1 FROM savings_history h WHERE h.goal_id=goals.id)",new Object[]{LocalDateTime.now().toString()});
    }
    public long balanceBefore(LocalDate boundary) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN income=1 THEN amount ELSE -amount END),0) FROM tx WHERE day < ?",new String[]{boundary.toString()})) {
            c.moveToFirst();return c.getLong(0);
        }
    }
    public void contribute(long goalId,long delta) {
        if(delta==0||delta>1_000_000_000_000L||delta< -1_000_000_000_000L)throw new IllegalArgumentException("Số tiền không hợp lệ.");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try(Cursor c=db.rawQuery("SELECT name,target,saved FROM goals WHERE id=?",new String[]{""+goalId})) {
            if(!c.moveToFirst())throw new IllegalArgumentException("Mục tiêu không còn tồn tại.");
            long balance=c.getLong(2)+delta;
            if(balance<0||balance>c.getLong(1))throw new IllegalArgumentException("Số dư mục tiêu phải từ 0 đến số tiền mục tiêu.");
            ContentValues update=new ContentValues();update.put("saved",balance);db.update("goals",update,"id=?",new String[]{""+goalId});
            ContentValues history=new ContentValues();history.put("goal_id",goalId);history.put("goal_name",c.getString(0));history.put("amount",delta);history.put("balance",balance);history.put("created",LocalDateTime.now().toString());history.put("kind",delta>0?"deposit":"withdrawal");
            db.insertOrThrow("savings_history",null,history);db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    private void createCategories(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS categories(name TEXT PRIMARY KEY NOT NULL)");
        for(String name:Finance.CATEGORIES) {
            ContentValues v=new ContentValues();v.put("name",name);
            db.insertWithOnConflict("categories",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        }
        importUsedCategories(db);
    }
    private void createIncomeCategories(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS income_categories(name TEXT PRIMARY KEY NOT NULL)");
        for(String name:new String[]{"Lương","Thưởng","Kinh doanh","Đầu tư","Quà tặng","Thu nhập khác"}) {
            ContentValues value=new ContentValues();value.put("name",name);
            db.insertWithOnConflict("income_categories",null,value,SQLiteDatabase.CONFLICT_IGNORE);
        }
        importCategories(db,true);
    }
    private static String categoryTable(boolean income){return income?"income_categories":"categories";}
    private void importCategories(SQLiteDatabase db,boolean income) {
        for(String table:new String[]{"tx","recurring"})
            db.execSQL("INSERT OR IGNORE INTO "+categoryTable(income)+"(name) SELECT DISTINCT category FROM "+table+" WHERE income="+(income?1:0));
    }
    private void importUsedCategories(SQLiteDatabase db) {
        importCategories(db,false);
        // Expense categories are created before the income table on a fresh database.
        try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE name='income_categories'",null)) {
            if(c.moveToFirst())importCategories(db,true);
        }
    }
    public String[] categories(){return categories(false);}
    public String[] categories(boolean income) {
        List<String> names=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT name FROM "+categoryTable(income)+" ORDER BY name COLLATE NOCASE",null)) {
            while(c.moveToNext())names.add(c.getString(0));
        }
        return names.toArray(new String[0]);
    }
    public long categoryUsage(String name){return categoryUsage(name,false);}
    public long categoryUsage(String name,boolean income) {
        long count=0;
        for(String table:new String[]{"tx","recurring"})
            try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table+" WHERE income=? AND category=?",new String[]{income?"1":"0",name})) {c.moveToFirst();count+=c.getLong(0);}
        return count;
    }
    public void saveCategory(String previous,String input){saveCategory(previous,input,false);}
    public void saveCategory(String previous,String input,boolean income) {
        String name=java.text.Normalizer.normalize(input.trim().replaceAll("\\s+"," "),java.text.Normalizer.Form.NFC);
        if(name.isEmpty()||name.length()>40)throw new IllegalArgumentException("Tên danh mục phải từ 1 đến 40 ký tự.");
        if(name.equalsIgnoreCase("Tất cả")||name.equalsIgnoreCase("Thu nhập")||name.startsWith("Thu · "))throw new IllegalArgumentException("Tên này dành riêng cho bộ lọc. Hãy chọn tên khác.");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            List<String> names=Arrays.asList(categories(income));
            if(previous!=null&&!names.contains(previous))throw new IllegalArgumentException("Danh mục không còn tồn tại.");
            for(String existing:names)if(!existing.equals(previous)&&existing.equalsIgnoreCase(name))throw new IllegalArgumentException("Danh mục này đã tồn tại.");
            ContentValues v=new ContentValues();v.put("name",name);
            if(previous==null)db.insertOrThrow(categoryTable(income),null,v);
            else {db.update(categoryTable(income),v,"name=?",new String[]{previous});replaceCategory(db,previous,name,income);}
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    private void replaceCategory(SQLiteDatabase db,String from,String to,boolean income) {
        ContentValues values=new ContentValues();values.put("category",to);
        for(String table:new String[]{"tx","recurring"})db.update(table,values,"income=? AND category=?",new String[]{income?"1":"0",from});
    }
    public void deleteCategory(String name,String replacement){deleteCategory(name,replacement,false);}
    public void deleteCategory(String name,String replacement,boolean income) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            List<String> names=Arrays.asList(categories(income));
            if(!names.contains(name))throw new IllegalArgumentException("Danh mục không còn tồn tại.");
            if(names.size()<2)throw new IllegalArgumentException("Cần giữ ít nhất một danh mục "+(income?"thu nhập.":"chi tiêu."));
            if(categoryUsage(name,income)>0) {
                if(name.equals(replacement)||!names.contains(replacement))throw new IllegalArgumentException("Chọn danh mục thay thế hợp lệ.");
                replaceCategory(db,name,replacement,income);
            }
            db.delete(categoryTable(income),"name=?",new String[]{name});db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    public List<ContentValues> rows(String table,String order) {
        List<ContentValues> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query(table,null,null,null,null,null,order)) {
            while(c.moveToNext()) { ContentValues v=new ContentValues(); for(int i=0;i<c.getColumnCount();i++) { if(c.isNull(i))v.putNull(c.getColumnName(i)); else v.put(c.getColumnName(i),c.getString(i)); } result.add(v); }
        } return result;
    }
    public long save(String table,ContentValues v) {
        Long id=v.getAsLong("id");
        if(id!=null) { getWritableDatabase().update(table,v,"id=?",new String[]{id.toString()}); return id; }
        return getWritableDatabase().insertOrThrow(table,null,v);
    }
    public void delete(String table,long id) { getWritableDatabase().delete(table,"id=?",new String[]{""+id}); }
    public void transaction(Long id,long amount,boolean income,String note,String category,String day,String source,String key) {
        LocalDate.parse(day);
        ContentValues v=new ContentValues(); if(id!=null)v.put("id",id);
        v.put("amount",amount);v.put("income",income?1:0);v.put("note",note);v.put("category",category);v.put("day",day);v.put("source",source);if(key!=null)v.put("unique_key",key);
        save("tx",v);
    }
    public long sum(String prefix,boolean income) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM tx WHERE day LIKE ? AND income=?",new String[]{prefix+"%",income?"1":"0"})) { c.moveToFirst();return c.getLong(0); }
    }
    public void processRecurring() {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            for(ContentValues v:rows("recurring","id")) {
                LocalDate due=LocalDate.parse(v.getAsString("due"));
                int count=0;
                while(!due.isAfter(LocalDate.now()) && count++<1200) {
                    ContentValues t=new ContentValues(); t.put("amount",v.getAsLong("amount"));t.put("income",v.getAsInteger("income"));t.put("note",v.getAsString("note"));t.put("category",v.getAsString("category"));t.put("day",due.toString());t.put("source","Định kỳ");t.put("unique_key","recurring:"+v.getAsLong("id")+":"+due);
                    db.insertWithOnConflict("tx",null,t,SQLiteDatabase.CONFLICT_IGNORE);
                    due=Finance.nextDue(due,v.getAsInteger("anchor"));
                }
                v.put("due",due.toString());save("recurring",v);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    public String backup() throws JSONException {
        JSONObject root=new JSONObject();root.put("version",1);
        for(String table:new String[]{"tx","recurring","goals"}) {
            JSONArray list=new JSONArray();for(ContentValues v:rows(table,"id")) { JSONObject obj=new JSONObject(); for(String key:v.keySet())if(v.get(key)!=null)obj.put(key,v.get(key));list.put(obj); }root.put(table,list);
        }return root.toString(2);
    }
    public void restore(String json) throws JSONException {
        JSONObject root=new JSONObject(json);
        if(root.getInt("version")!=1)throw new JSONException("Phiên bản sao lưu không hỗ trợ");
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            for(String table:new String[]{"tx","recurring","goals"}) {
                JSONArray a=root.getJSONArray(table);if(a.length()>100000)throw new JSONException("Tệp quá lớn");
                db.delete(table,null,null);
                for(int i=0;i<a.length();i++) {
                    JSONObject o=a.getJSONObject(i);ContentValues v=new ContentValues();
                    v.put("id",o.getLong("id"));if(o.getLong("id")<=0)throw new JSONException("ID không hợp lệ");
                    if(table.equals("goals")) {
                        v.put("name",o.getString("name"));v.put("target",Finance.money(o.getString("target")));long saved=o.getLong("saved");if(saved<0||saved>v.getAsLong("target"))throw new JSONException("Tiết kiệm không hợp lệ");v.put("saved",saved);v.put("deadline",LocalDate.parse(o.getString("deadline")).toString());
                    } else {
                        v.put("amount",Finance.money(o.getString("amount")));int inc=o.getInt("income");if(inc!=0&&inc!=1)throw new JSONException("Loại giao dịch không hợp lệ");v.put("income",inc);v.put("note",o.getString("note"));v.put("category",o.getString("category"));
                        if(table.equals("tx")){v.put("day",LocalDate.parse(o.getString("day")).toString());v.put("source",o.getString("source"));if(o.has("unique_key"))v.put("unique_key",o.getString("unique_key"));}
                        else {v.put("due",LocalDate.parse(o.getString("due")).toString());int anchor=o.getInt("anchor");if(anchor<1||anchor>31)throw new JSONException("Ngày không hợp lệ");v.put("anchor",anchor);}
                    }
                    db.insertOrThrow(table,null,v);
                }
            } importUsedCategories(db);db.setTransactionSuccessful();
        }finally {db.endTransaction();}
    }
}
