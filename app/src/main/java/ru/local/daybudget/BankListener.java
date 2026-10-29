package ru.local.daybudget;
import android.app.Notification;
import android.service.notification.*;
import android.content.SharedPreferences;
public class BankListener extends NotificationListenerService {
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  SharedPreferences p=Store.p(this);
  if(!sbn.getPackageName().equals(p.getString("bankPackage","com.idamob.tinkoff.android")))return;
  Notification n=sbn.getNotification(); if((n.flags & Notification.FLAG_GROUP_SUMMARY)!=0)return;
  StringBuilder all=new StringBuilder();
  String[] fields={Notification.EXTRA_BIG_TEXT,Notification.EXTRA_TEXT,Notification.EXTRA_TITLE};
  for(String field:fields){CharSequence value=n.extras.getCharSequence(field);if(value!=null)all.append(value).append('\n');}
  CharSequence[] lines=n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
  if(lines!=null)for(CharSequence line:lines)if(line!=null)all.append(line).append('\n');
  String text=all.toString();
  long time=sbn.getPostTime();
  if(time<p.getLong("eventTime",0))return;
  Long amount=Store.parse(text);
  if(amount==null){p.edit().putString("status","Уведомление получено, но строка «Доступно … ₽» или «Баланс … ₽» не найдена. Баланс не изменён.").apply();return;}
  if(time==p.getLong("eventTime",0)&&amount==p.getLong("card",0))return;
  p.edit().putLong("card",amount).putLong("eventTime",time).putLong("updated",System.currentTimeMillis()).putString("status","Баланс обновлён из уведомления банка").apply();
 }
}