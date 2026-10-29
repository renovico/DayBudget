package ru.local.daybudget;
import android.app.*;
import android.os.*;
import android.content.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import java.time.*;
import java.time.temporal.ChronoUnit;
public class MainActivity extends Activity {
 android.content.SharedPreferences p; LinearLayout body; int tab=0; TextView summary; boolean dark; int ink, accent, surface, background;
 android.content.SharedPreferences.OnSharedPreferenceChangeListener changes=(prefs,key)->{if(tab==0)runOnUiThread(()->overviewText());};
 @Override public void onCreate(Bundle b){
 p=Store.p(this);
 String mode=p.getString("theme","system");
 dark=mode.equals("dark") || (mode.equals("system") && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
 setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);
 super.onCreate(b);
 ink=dark?0xfff0edf7:0xff252032;accent=dark?0xffc4abff:0xff6440ad;
 surface=dark?0xff24212d:0xffffffff;background=dark?0xff15131b:0xfff5f4fb;
 if(b!=null)tab=b.getInt("tab",0);p.registerOnSharedPreferenceChangeListener(changes);draw();
}
 @Override protected void onDestroy(){p.unregisterOnSharedPreferenceChangeListener(changes);super.onDestroy();}
 @Override protected void onResume(){super.onResume();if(tab==0 && summary!=null)overviewText();}
 TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,12,0,12);body.addView(t);return t;}
 Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);styleButton(b,false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,dp(6),0,dp(6));body.addView(b,bp);b.setOnClickListener(v->r.run());return b;}
 EditText field(String value,boolean number){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(ink);e.setHintTextColor(dark?0xffb5adbf:0xff746c80);e.setBackgroundTintList(ColorStateList.valueOf(accent));e.setText(value);if(number)e.setInputType(2|8192|4096);body.addView(e);return e;}
 void draw(){
  LinearLayout outer=new LinearLayout(this);outer.setOrientation(1);outer.setPadding(24,40,24,24);outer.setBackgroundColor(background);setContentView(outer);
  TextView title=new TextView(this);title.setText("Дневной бюджет");title.setTextSize(26);title.setTextColor(ink);outer.addView(title);
  LinearLayout nav=new LinearLayout(this);outer.addView(nav);
  String[] labels={"Обзор","Карта","Наличные","⚙"};for(int i=0;i<4;i++){final int n=i;Button b=new Button(this);b.setText(labels[i]);b.setTextSize(12);styleButton(b,i==tab);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.setMargins(dp(2),dp(8),dp(2),dp(8));nav.addView(b,np);b.setOnClickListener(v->{tab=n;draw();});}
  ScrollView scroll=new ScrollView(this);outer.addView(scroll);body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);
  if(tab==0){summary=text("",22);overviewText();text("Лимит = (карта + наличные) ÷ дни до зарплаты. Сегодня входит в расчёт, день зарплаты — нет. При наступлении зарплаты дата переносится на следующий месяц. Сам доход автоматически не начисляется.",15);}
  if(tab==1||tab==2){boolean cash=tab==2;String key=cash?"cash":"card";text(cash?"Наличные":"Карта Т-Банка",24);text("Текущий остаток: "+Store.fmt(p.getLong(key,0)),20);EditText amount=field(Store.edit(p.getLong(key,0)),true);
   button("Сохранить остаток",()->{try{long a=Store.money(amount.getText().toString());p.edit().putLong(key,a).apply();draw();}catch(Exception e){toast("Введите сумму, например 1234,56");}});
   if(cash){EditText delta=field("",true);delta.setHint("Сумма операции");button("Потратил наличные",()->cashChange(delta,-1));button("Добавить наличные",()->cashChange(delta,1));}
   else text("Остаток заменяется суммой после «Доступно» или «Баланс» в новых уведомлениях банка. Он не уменьшается повторно на сумму покупки. Ручной остаток будет заменён следующим подходящим уведомлением.",16);
  }
  if(tab==3){text("Настройки",24);
 String mode=p.getString("theme","system");
 button("Тема: "+(mode.equals("dark")?"Тёмная":mode.equals("light")?"Светлая":"Как в системе"),()->{
  String[] names={"Как в системе","Светлая","Тёмная"};String[] keys={"system","light","dark"};
  int selected=mode.equals("light")?1:mode.equals("dark")?2:0;
  new AlertDialog.Builder(this).setTitle("Оформление").setSingleChoiceItems(names,selected,(dialog,which)->{
   p.edit().putString("theme",keys[which]).apply();dialog.dismiss();recreate();
  }).setNegativeButton("Отмена",null).show();
 });button("Разрешить доступ к уведомлениям",()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
   text("Разрешение позволяет читать уведомления. Приложение обрабатывает только выбранный пакет банка. Интернет-разрешения нет. Специальные возможности не нужны.",16);
   button("Выбрать дату зарплаты (ежемесячно)",()->{LocalDate d=Store.salary(this);new DatePickerDialog(this,(v,y,m,day)->{p.edit().putInt("day",day).apply();draw();},d.getYear(),d.getMonthValue()-1,d.getDayOfMonth()).show();});
   text("День зарплаты: "+p.getInt("day",10)+". Если такого дня в месяце нет, используется последний день месяца.",16);
   text("Пакет приложения банка",16);EditText pkg=field(p.getString("bankPackage","com.idamob.tinkoff.android"),false);
   button("Сохранить пакет",()->{String s=pkg.getText().toString().trim();if(!s.contains(".")){toast("Укажите имя пакета");return;}p.edit().putString("bankPackage",s).remove("eventTime").apply();toast("Сохранено");});
   text("Имя пакета может отличаться у разных сборок Т-Банка. Значение по умолчанию нужно проверить на телефоне. Скрытые или сокращённые банковские уведомления могут не содержать остаток.",15);
   text("Проверка распознавания (баланс не меняет)",18);EditText test=field("Перевод на 462,3 ₽, счет RUB. Баланс 62 836,96 ₽",false);button("Проверить",()->{Long n=Store.parse(test.getText().toString());toast(n==null?"Остаток не найден":Store.fmt(n));});
   text("Диагностика: "+p.getString("status","Уведомления ещё не обработаны"),15);
  }
 }
 void overviewText(){if(summary==null)return;long card=p.getLong("card",0),cash=p.getLong("cash",0),total=card+cash;LocalDate salary=Store.salary(this);long days=ChronoUnit.DAYS.between(LocalDate.now(),salary);long per=java.math.BigDecimal.valueOf(total).divide(java.math.BigDecimal.valueOf(days),0,java.math.RoundingMode.FLOOR).longValue();long updated=p.getLong("updated",0);
 summary.setText("Можно в день\n"+Store.fmt(per)+"\n\nКарта: "+Store.fmt(card)+"\nНаличные: "+Store.fmt(cash)+"\nВсего: "+Store.fmt(total)+"\n\nДо зарплаты: "+days+" дн.\nДата: "+salary.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))+"\n\nБанк: "+(updated==0?"пока нет обновлений":java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(updated))));}
 void cashChange(EditText e,int sign){try{long a=Store.money(e.getText().toString());if(a<=0)throw new Exception();p.edit().putLong("cash",Math.addExact(p.getLong("cash",0),Math.multiplyExact(a,sign))).apply();draw();}catch(Exception ex){toast("Введите положительную сумму");}}
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 void styleButton(Button b,boolean active){
  b.setAllCaps(false);b.setTextColor(active?(dark?0xff20152e:Color.WHITE):accent);
  b.setBackgroundTintList(null);
  GradientDrawable shape=new GradientDrawable();shape.setColor(active?accent:surface);
  shape.setCornerRadius(dp(14));shape.setStroke(dp(1),accent);
  b.setBackground(new RippleDrawable(ColorStateList.valueOf(dark?0x40ffffff:0x226440ad),shape,null));
  b.setPadding(dp(8),dp(10),dp(8),dp(10));b.setMinimumHeight(dp(48));
 }
 @Override public void onSaveInstanceState(Bundle b){b.putInt("tab",tab);super.onSaveInstanceState(b);}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}