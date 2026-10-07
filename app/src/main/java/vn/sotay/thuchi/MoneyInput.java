package vn.sotay.thuchi;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

/** Formats the display while retaining an integer amount and the logical cursor position. */
public final class MoneyInput implements TextWatcher {
    private final EditText field;
    private boolean updating;
    private String previous="";
    private int changeStart, removed, inserted;
    private MoneyInput(EditText field) { this.field=field; }
    public static void attach(EditText field) {
        field.setKeyListener(DigitsKeyListener.getInstance("0123456789 .,\u00a0"));
        field.setRawInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInput watcher=new MoneyInput(field);
        field.addTextChangedListener(watcher);
        watcher.afterTextChanged(field.getText());
    }
    public void beforeTextChanged(CharSequence text,int start,int count,int after) {
        if(!updating)previous=text.toString();
    }
    public void onTextChanged(CharSequence text,int start,int before,int count) {
        if(!updating){changeStart=start;removed=before;inserted=count;}
    }
    public void afterTextChanged(Editable editable) {
        if(updating)return;
        String input=editable.toString();
        // Keep invalid decimals intact so validation can reject them instead of changing their value.
        if(!input.matches("[0-9 \\u00a0]*") && !input.matches("[0-9]{1,3}(?:(?:\\.[0-9]{3})+|(?:,[0-9]{3})+)"))return;
        int cursor=Math.max(0,field.getSelectionStart());
        if(removed==1 && inserted==0 && changeStart>0 && changeStart<previous.length()
                && previous.charAt(changeStart)==' ' && changeStart<=input.length()) {
            input=input.substring(0,changeStart-1)+input.substring(changeStart);
            cursor=Math.max(0,cursor-1);
        }
        int digitsBefore=0;
        for(int i=0;i<Math.min(cursor,input.length());i++)if(Character.isDigit(input.charAt(i)))digitsBefore++;
        String digits=input.replaceAll("[ .,\\u00a0]","");
        String formatted=Finance.groupedDigits(digits);
        if(formatted.equals(editable.toString()))return;
        int selection=0,seen=0;
        while(selection<formatted.length() && seen<digitsBefore) {
            if(Character.isDigit(formatted.charAt(selection)))seen++;
            selection++;
        }
        updating=true;
        field.setText(formatted);
        field.setSelection(Math.min(selection,formatted.length()));
        updating=false;
    }
}
