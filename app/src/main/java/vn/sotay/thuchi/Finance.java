package vn.sotay.thuchi;

import java.time.LocalDate;
import java.time.YearMonth;

public final class Finance {
    public static final String[] CATEGORIES = {"Ăn uống", "Di chuyển", "Mua sắm", "Nhà ở", "Hóa đơn", "Sức khỏe", "Giáo dục", "Giải trí", "Khác"};
    public static String groupedDigits(String digits) {
        StringBuilder grouped = new StringBuilder();
        for (int i=0;i<digits.length();i++) {
            if(i>0 && (digits.length()-i)%3==0)grouped.append(' ');
            grouped.append(digits.charAt(i));
        }
        return grouped.toString();
    }
    public static String formatMoney(long amount) {
        String raw=Long.toString(amount);
        return amount<0 ? "−"+groupedDigits(raw.substring(1)) : groupedDigits(raw);
    }
    public static long money(String input) {
        String s = input.trim().replaceAll("[\\s₫đ]", "");
        if(!s.matches("[0-9]+")&&!s.matches("[0-9]{1,3}(?:(?:\\.[0-9]{3})+|(?:,[0-9]{3})+)"))
            throw new IllegalArgumentException("Nhập số tiền nguyên VNĐ; dấu chấm hoặc phẩy chỉ dùng phân cách hàng nghìn.");
        s=s.replace(".","").replace(",","");
        if (!s.matches("[0-9]{1,13}")) throw new IllegalArgumentException("Nhập số tiền nguyên dương, tối đa 1.000 tỷ đồng.");
        long amount = Long.parseLong(s);
        if (amount <= 0 || amount > 1_000_000_000_000L) throw new IllegalArgumentException("Số tiền phải từ 1 đến 1.000 tỷ đồng.");
        return amount;
    }
    public static LocalDate nextDue(LocalDate due, int anchorDay) {
        YearMonth next=YearMonth.from(due).plusMonths(1);
        return next.atDay(Math.min(anchorDay,next.lengthOfMonth()));
    }
}
