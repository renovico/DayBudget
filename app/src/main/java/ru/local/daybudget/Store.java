package ru.local.daybudget;
import android.content.*;
import java.math.*;
import java.time.*;
import java.util.regex.*;
public final class Store {
 static android.content.SharedPreferences p(Context c){ return c.getSharedPreferences("budget",0); }
 static long money(String s){return new BigDecimal(s.replaceAll("[\\s\\u00a0\\u202f]", "").replace(',', '.')).setScale(2,RoundingMode.HALF_UP).movePointRight(2).longValueExact();}
 static String fmt(long n){return String.format(java.util.Locale.forLanguageTag("ru"),"%,.2f ₽",n/100.0);}
 static String edit(long n){return BigDecimal.valueOf(n,2).toPlainString();}
 static Long parse(String text){
  Pattern re=Pattern.compile("(?iu)(?<![\\p{L}])(?:доступно|баланс)\\s*[:：]?\\s*([0-9][0-9 \\u00a0\\u202f]*(?:[,.][0-9]{1,2})?)\\s*(?:₽|руб)");
  Matcher m=re.matcher(text); if(!m.find())return null;
  try{return money(m.group(1));}catch(Exception e){return null;}
 }
 static LocalDate salary(Context c){
  int day=p(c).getInt("day",10); LocalDate today=LocalDate.now(); YearMonth mo=YearMonth.from(today);
  LocalDate date=mo.atDay(Math.min(day,mo.lengthOfMonth()));
  if(date.isBefore(today)){mo=mo.plusMonths(1);date=mo.atDay(Math.min(day,mo.lengthOfMonth()));}return date;
 }
}