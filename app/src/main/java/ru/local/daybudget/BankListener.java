package ru.local.daybudget;
import android.app.Notification;
import android.service.notification.*;
import android.content.SharedPreferences;
public class BankListener extends NotificationListenerService {
 @Override public void onNotificationPosted(StatusBarNotification sbn){
  SharedPreferences p=Store.p(this);
  if(!sbn.getPackageName().equals(p.getString("bankPackage","com.idamob.tinkoff.android")))return;
  Notification n=sbn.getNotification(); if((n.flags & Notification.FLAG_GROUP_SUMMARY)!=0)return;
  CharSequence body=n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
  if(body==null || body.length()==0)body=n.extras.getCharSequence(Notification.EXTRA_TEXT);
  String text=String.valueOf(n.extras.getCharSequence(Notification.EXTRA_TITLE,""))+"\n"+(body==null?"":body.toString());
  long time=sbn.getPostTime();
  if(time<p.getLong("eventTime",0))return;
  Long amount=Store.parse(text);
  if(amount==null){p.edit().putString("status","Уведомление получено, но строка «Доступно … ₽» не найдена. Баланс не изменён.").apply();return;}
  if(time==p.getLong("eventTime",0)&&amount==p.getLong("card",0))return;
  p.edit().putLong("card",amount).putLong("eventTime",time).putLong("updated",System.currentTimeMillis()).putString("status","Баланс обновлён из уведомления банка").apply();
 }
}