/*
 * Copyright 2012, Unicorn-Feng
 * All rights reserved.
 * 
 * This file is part of Fantalker.
 * Fantalker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * Fantalker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Fantalker.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 *饭否GTalk机器人 Fantalker
 *Fanfou Chat Robot for Google Talk
 *Author: 烽麒 Unicorn-Feng
 *Website: http://fq.vc 
 */

package vc.fq.fantalker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.cache.CacheManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.stdimpl.GCache;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;


/**
 * 公共函数类
 * @author 烽麒 Unicorn-Feng
 * @link http://fq.vc
 */
public final class Common
{
	public static final Logger log = Logger.getLogger("Fantalker");
	
	
	/**
	 * 分配短ID
	 * @param id 消息ID
	 * @param nextID 下一个欲分配短ID位置
	 * @param ltShortID 短ID对应表List
	 * @return
	 */
	public static List<String> allocShortID(String id, int nextID,List<String> ltShortID)
	{
		/* 分配短ID */
		nextID = nextID % 100 ;
		if(nextID == 0)
		{
			nextID = 1;
		}
		if(ltShortID.size() <= nextID)
		{
			ltShortID.add(id);
			nextID++;
			ltShortID.set(0,String.valueOf(nextID));
		}
		else
		{
			ltShortID.set(nextID, id);
			nextID++;
			ltShortID.set(0, String.valueOf(nextID));
		}
		return ltShortID;
	}
	
	
	/**
	 * 获取API对象
	 * @param fromJID
	 * @return API对象,若未绑定返回null
	 */
	public static API getAPI(JID fromJID) throws NullPointerException
	{
		String oauth_key;
		String oauth_key_secret;
		oauth_key = getData(fromJID,"access_token");
		oauth_key_secret = getData(fromJID,"access_token_secret");
		try{
			if(oauth_key.isEmpty() || oauth_key_secret.isEmpty() || oauth_key == null || oauth_key_secret == null)
			{
				return null;
			}
			API api = new API(oauth_key,oauth_key_secret);
			return api;
		} catch (NullPointerException e){
			return null;
		}
	}

	
	/**
	 * 从数据库中读取一个数据
	 * @param fromJID 来源JID
	 * @param strProperty 要读取数据的属性名
	 * @return 读取结果，无结果返回null
	 */
	public static String getData(JID fromJID, String EntityName, String strProperty)
	{
		String strJID = Common.getStrJID(fromJID);
		
		/* 从MemCache中读取数据 */
		String memData = getMemData(strJID,strProperty);
		if(memData != null)
		{
			return memData;
		}
		
		/* 从Datastore中读取数据 */
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key k = KeyFactory.createKey(EntityName, strJID);
		Entity account;
		try {
			account = datastore.get(k);
		} catch (EntityNotFoundException e) {
			log.info(strJID + ":Entity " + e.getMessage());
			return null;
		}
	
		String strData;
		try {
			strData = account.getProperty(strProperty).toString();
		}
		catch (NullPointerException e)
		{
			log.info(strJID + ": " + strProperty + " " + e.toString());
			return null;
		}
		
		/* 将数据写入MemCache中 */
		setMemData(strJID,strProperty,strData);
		
		return strData;
	}
	
	
	/**
	 * 从数据库Account实体中读取一个数据
	 * @param fromJID 来源JID
	 * @param strProperty 要读取数据的属性名
	 * @return 读取结果，无结果返回null
	 */
	public static String getData(JID fromJID, String strProperty)
	{
		return getData(fromJID,"Account",strProperty);
	}


	/**
	 * 从错误json中获取错误原因
	 * @param strerr
	 * @return
	 */
	public static String getError(String strerr)
	{
		String error;
		try {
			JSONObject json = new JSONObject(strerr);
			error = json.getString("error");
			return error;
			
		} catch (JSONException e) {
			log.info("error.JSON " + e.getMessage());
			return "未知错误";
		}
	}
	
	
	/**
	 * 获取绑定的饭否ID
	 * @param fromJID 来源JID
	 * @return 字符串型饭否ID,未绑定返回null
	 */
	public static String getID(JID fromJID)
	{
		return getData(fromJID,"id");
	}


	/**
	 * 从MemCache中读取数据
	 * @param strJID 字符串形式JID
	 * @param strProperty 键值
	 * @return
	 */
	public static String getMemData(String strJID, String strProperty)
	{
		GCache cache;
		try{
			GCacheFactory cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			if(!cache.isEmpty())
			{
				if(cache.containsKey(strJID + "," + strProperty))
				{
					String value;
					value = (String) cache.get(strJID + "," + strProperty);
					if(value.length()>=1)
					{
						return value;
					}
				}
			}
		} catch (javax.cache.CacheException e) {
			log.info(strJID + ":JCache " + e.getMessage());
		}
		return null;
		
	}


	/**
	 * 获取月份
	 * @param month
	 * @return 数字月份
	 */
	public static int getMonth(String month)
	{
		if(month.equals("Jan"))
			return 1;
		else if(month.equals("Feb"))
			return 2;
		else if(month.equals("Mar"))
			return 3;
		else if(month.equals("Apr"))
			return 4;
		else if(month.equals("May"))
			return 5;
		else if(month.equals("Jun"))
			return 6;
		else if(month.equals("Jul"))
			return 7;
		else if(month.equals("Aug"))
			return 8;
		else if(month.equals("Sep"))
			return 9;
		else if(month.equals("Oct"))
			return 10;
		else if(month.equals("Nov"))
			return 11;
		else if(month.equals("Dec"))
			return 12;
		else
			return -1;
	}

	
	/**
	 * 获取用户设置
	 * @param fromJID
	 * @return Setting对象
	 */
	public static Setting getSetting(JID fromJID)
	{
		String strJID = getStrJID(fromJID);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key k = KeyFactory.createKey("setting", strJID);
		Entity account;
		try {
			account = datastore.get(k);
		} catch (EntityNotFoundException e) {
			log.info(strJID + ":Entity " + e.getMessage());
			return null;
		}
	
		boolean dm;
		boolean mention;
		long time;
		try {
			dm = (Boolean) account.getProperty("dm");
			mention = (Boolean) account.getProperty("mention");
			time = (Long) account.getProperty("time");
		}
		catch (NullPointerException e)
		{
			log.info(strJID + ":setting " + e.toString());
			return null;
		}
		Setting set = new Setting(dm,mention,time);
		return set;
	}
	
	
	/**
	 * 格式化时间
	 * @param strdate UTC时间 "Mon Mar 26 09:28:48 +0000 2012"
	 * @return 北京时间 "2012-03-26 17:28:48"
	 */
	public static String getStrDate(String strdate)
	{
		String strTmp;
		int year,month,day,hour,minute,second;
		
		strTmp = strdate.substring(26,30);
		year = Integer.parseInt(strTmp);
		strTmp = strdate.substring(4,7);
		month = getMonth(strTmp);
		strTmp = strdate.substring(8,10);
		day = Integer.parseInt(strTmp);
		strTmp = strdate.substring(11,13);
		hour = Integer.parseInt(strTmp);
		strTmp = strdate.substring(14,16);
		minute = Integer.parseInt(strTmp);
		strTmp = strdate.substring(17,19);
		second = Integer.parseInt(strTmp);
		
		Calendar calendar = new GregorianCalendar(year,month-1,day,hour,minute,second);
		calendar.add(Calendar.HOUR_OF_DAY, 8);
		SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		return format.format(calendar.getTime());
 	}
	
	
	/**
	 * 截取JID中有效的地址不符
	 * @param fromJID
	 * @return 字符串型JID
	 */
	public static String getStrJID(JID fromJID)
	{
		String strJID = fromJID.getId();
		int index = strJID.indexOf("/");
		if(index == -1)
		{
			return strJID;
		}
		strJID = strJID.substring(0,index);
		return strJID;
	}


	/**
	 * 从含地址的来源字符串中分离出来源
	 * @param source <a href="http://fq.vc" target="_blank">一勺池</a>
	 * @return 一勺池
	 */
	public static String getSource(String source)
	{
		int indexs,indexe;
		indexs = source.indexOf("target=\"_blank\">");
		if(indexs == -1)
		{
			return source;
		}
		indexe = source.indexOf("</a>");
		source = source.substring(indexs + 16, indexe);
		return source;
	}


	/**
	 * 判断字符串是否为数字
	 * @param str
	 * @return 数字返回true
	 */
	public static boolean isNumeric(String str)
	{ 
		for (int i = str.length();--i>=0;)
		{   
			if (!Character.isDigit(str.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}

	
	/**
	 * 向指定JID发送xmpp消息
	 * @param fromJID 来源JID
	 * @param strMessage 要发送的消息内容
	 */
	public static void sendMessage(JID fromJID, String strMessage)
	{
		XMPPService xmpp = XMPPServiceFactory.getXMPPService();
		Message MsgSend = new MessageBuilder()
				.withRecipientJids(fromJID)
				.withBody(strMessage)
				.build();
		if(xmpp.getPresence(fromJID).isAvailable())
		{
			SendResponse success = xmpp.sendMessage(MsgSend);
			if(success.getStatusMap().get(fromJID) != SendResponse.Status.SUCCESS)	//发送失败，重试一次
			{
				success = xmpp.sendMessage(MsgSend);
			}
		}
	}
	
	
	/**
	 * 将数据存至datastore及memcache
	 * @param fromJID
	 * @param strProperty 属性key
	 * @param value
	 */
	public static void setData(JID fromJID, String EntityName, String strProperty, String value)
	{
		String strJID = getStrJID(fromJID);
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity entity = new Entity(EntityName,strJID);
		entity.setProperty(strProperty, value);
		datastore.put(entity);
		
		/* 将数据写入MemCache中 */
		GCache cache;
		try{
			GCacheFactory cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			cache.put(strJID + "," + strProperty,value);
		} catch (javax.cache.CacheException e){
			log.info(strJID + ":JCache " + e.getMessage());
		}
	}


	/**
	 * 将数据存至datastore及memcache
	 * @param fromJID
	 * @param strProperty[] 属性key数组
	 * @param value[]
	 */
	public static void setData(JID fromJID, String EntityName, String[] strProperty, String[] value)
	{
		String strJID = getStrJID(fromJID);
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity entity = new Entity(EntityName,strJID);
		
		for(int i=0;i<strProperty.length;i++)
		{
			entity.setProperty(strProperty[i], value[i]);
		}
		datastore.put(entity);
		
		/* 将数据写入MemCache中 */
		GCache cache;
		try{
			GCacheFactory cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			for(int i=0;i<strProperty.length;i++)
			{
				cache.put(strJID + "," + strProperty[i],value[i]);
			}
			
		} catch (javax.cache.CacheException e){
			log.info(strJID + ":JCache " + e.getMessage());
		}
	}

	
	/**
	 * 将数据存储到MemCached
	 * @param strJID 字符串形式JID
	 * @param strProperty 键值
	 * @param strData
	 */
	public static void setMemData(String strJID, String strProperty, String strData)
	{
		GCache cache;
		try{
			GCacheFactory cacheFactory;
			cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			cache.put(strJID + "," + strProperty,strData);
		} catch (javax.cache.CacheException e){
			log.info(strJID + ":JCache " + e.getMessage());
		}
	}


	/**
	 * 将用户设置存入datastore
	 * @param fromJID
	 * @param set Setting对象
	 */
	public static void setSetting(JID fromJID, Setting set)
	{
		String strJID = getStrJID(fromJID);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity entity = new Entity("setting",strJID);
		entity.setProperty("dm",set.getDm());
		entity.setProperty("mention",set.getMention());
		entity.setProperty("time",set.getTime());
		datastore.put(entity);
	}

	
	/**
	 * 将通过OAuth或XAuth获取到的Access Token存入数据库并记录用户名
	 * @param fromJID
	 * @param oauth_token
	 * @param oauth_token_secret
	 * @throws IOException 
	 */
	public static void setToken(JID fromJID, String oauth_token, String oauth_token_secret) throws IOException
	{
		String strJID = Common.getStrJID(fromJID);
		
		/* 将接收到的Request Token存入数据库 */
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity account = new Entity("Account",strJID);
		account.setProperty("access_token", oauth_token);
		account.setProperty("access_token_secret",oauth_token_secret);
		
		API api = new API(oauth_token,oauth_token_secret);
		HTTPResponse response = api.account_verify_credentials(fromJID);
		String id = null;
		if(response.getResponseCode() == 200)
		{
			try {
				JSONObject respJSON = new JSONObject(new String(response.getContent()));
				id = respJSON.getString("id");
			} catch (JSONException e) {
				Common.log.warning(strJID + ":JSONid " + e.getMessage());
			}
		}
		
		if(id == null)
		{
			datastore.put(account);
			Common.sendMessage(fromJID,"成功绑定，但在获取饭否ID时出现未知错误");
			Common.log.warning(strJID + ": " + new String(response.getContent()));
		}
		else
		{
			account.setProperty("id", id);
			datastore.put(account);
			Common.sendMessage(fromJID,"成功与饭否账号 " + id + " 绑定");
		}
		
		/* 保存设置信息到datastore */
		Entity entity = new Entity("setting",strJID);
		entity.setProperty("mention", true);
		entity.setProperty("dm",true);
		entity.setProperty("time", 5);
		datastore.put(entity);
		
		/* 将数据写入MemCache中 */
		GCache cache;
		try{
			GCacheFactory cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			cache.put(strJID + ",access_token",oauth_token);
			cache.put(strJID + ",access_token_secret",oauth_token_secret);
			cache.put(strJID + "mention", true);
			cache.put(strJID + "dm", true);
			cache.put(strJID + "time", 5);
		} catch (javax.cache.CacheException e){
			Common.log.info(strJID + ":JCache " + e.getMessage());
		}
	}
	
	
	/**
	 * 根据短ID获取ID
	 * @param fromJID
	 * @param shortID
	 * @return
	 */
	public static String shortID2ID(JID fromJID, String shortID)
	{
		String strJID = getStrJID(fromJID);
		String strArrShortID = getMemData(strJID,"shortid");
		if(!isNumeric(shortID))
		{
			return null;
		}
		if(strArrShortID == null)
		{
			return null;
		}
		int shortid = Integer.parseInt(shortID);
		List<String> ltShortID = new ArrayList<String>();
		String[] arrShortID = strArrShortID.split(",");
		for(int j=0;j<arrShortID.length;j++)
		{
			ltShortID.add(arrShortID[j]);
		}
		try {
			return ltShortID.get(shortid);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	
	/**
	 * xmpp中输出消息
	 * @param fromJID 来源JID
	 * @param jsonStatus StatusJSON对象
	 * @param intType 消息类型,1时间线,2提到我的,3mention提醒,4消息上下文,5随便看看,6已发消息
	 * @param pageID 页码
	 * @param lenght jsonStatus数组长度
	 */
	public static void showStatus(JID fromJID, StatusJSON[] jsonStatus, int intType, int length, String pageID)
	{
		String strMessage = null;
		String strJID = getStrJID(fromJID);

		/* 获取短ID数组 */
		String strArrShortID = getMemData(strJID,"shortid");
		List<String> ltShortID = new ArrayList<String>();
		String id;
		if(strArrShortID == null)
		{
			ltShortID.add("1");
		}
		else
		{
			String[] arrShortID;
			arrShortID = strArrShortID.split(",");
			for(int j=0;j<arrShortID.length;j++)
			{
				ltShortID.add(arrShortID[j]);
			}
		}
		int nextID = Integer.parseInt(ltShortID.get(0));
		
		if(intType ==4)
		{
			strMessage = "";
			for(int i=0;i<length;i++)
			{
				id = jsonStatus[i].getID();
				ltShortID = allocShortID(id,nextID,ltShortID);
				nextID = Integer.parseInt(ltShortID.get(0));
				strMessage = Common.StatusMessage(strMessage,jsonStatus[i],nextID-1);
			}
		}
		else
		{
			if(intType == 1)
			{
				strMessage = "时间线: 第" + pageID + "页\n\n";
			}
			else if(intType == 5)
			{
				strMessage = "随便看看:\n\n";
			}
			else if(intType == 6)
			{
				strMessage = "已发消息: 第" + pageID + "页\n\n";
			}
			else
			{
				strMessage = "提到我的: 第" + pageID + "页\n\n";
				if(pageID.equals("1"))
				{
					setData(fromJID,"mention","last_id",jsonStatus[0].getID());
				}
			}
			
			for(int i=(length-1);i>=0;i--)
			{
				id = jsonStatus[i].getID();
				ltShortID = allocShortID(id,nextID,ltShortID);
				nextID = Integer.parseInt(ltShortID.get(0));
				strMessage = Common.StatusMessage(strMessage,jsonStatus[i],nextID-1);
			}

			/* 存储短ID */
			Iterator<String> ir = ltShortID.iterator();
			strArrShortID = "";
			while(ir.hasNext())
			{
				strArrShortID = strArrShortID + ir.next() + ",";
			}
			strArrShortID = strArrShortID.substring(0,strArrShortID.length()-1);
			setMemData(strJID,"shortid",strArrShortID);
		}
		sendMessage(fromJID,strMessage);
	}
	
	
	/**
	 * xmpp中输出消息
	 * @param fromJID 来源JID
	 * @param jsonStatus StatusJSON对象
	 * @param intType 消息类型,1时间线,2提到我的,3mention提醒,4消息上下文
	 */
	public static void showStatus(JID fromJID, StatusJSON[] jsonStatus, int intType, int length)
	{
		showStatus(fromJID,jsonStatus,intType,length,"1");
	}
	
	
	/**
	 * 构造xmpp消息
	 * @param message 现有消息
	 * @param jsonStatus 状态JSON对象
	 * @param shortid 短ID
	 * @return
	 */
	public static String StatusMessage(String message, StatusJSON jsonStatus,int shortid)
	{
		UserJSON jsonUser = jsonStatus.getUserJSON();
		message = message + "*" + jsonUser.getScreenName() + "*: " + jsonStatus.getText()
					+ "\n [ " + jsonStatus.getID() + " = " + String.valueOf(shortid) + " ] " + getStrDate(jsonStatus.getCreatedAt())
					+ " <- " + getSource(jsonStatus.getSource()) + "\n\n";
		return message;
	}


	/**
	 * 处理显示状态的HTTPResponse对象
	 * @param fromJID
	 * @param response
	 * @param intType 消息类型,1时间线,2提到我的,3mention提醒,4消息上下文,5随便看看,6已发消息
	 * @param pageID 页码
	 */
	public static void StatusShowResp(JID fromJID, HTTPResponse response, int intType, String pageID)
	{
		if(response.getResponseCode() == 200)
		{
			try {
				JSONArray jsonarr = new JSONArray(new String(response.getContent()));
				StatusJSON[] jsonStatus = new StatusJSON[20];
				int arrlen = jsonarr.length();
				if(arrlen == 0)
				{
					if(intType != 3)
					{
						sendMessage(fromJID,"无更多消息");
					}
					return;
				}
				for(int i=0;i<arrlen;i++)
				{
					jsonStatus[i] = new StatusJSON(jsonarr.getJSONObject(i));
				}
				showStatus(fromJID,jsonStatus,intType,arrlen,pageID);
			} catch (JSONException e) {
				log.info("status.show.JSON " + e.getMessage());
			}
		}
		else
		{
			String err = getError(new String(response.getContent()));
			log.warning("status.show " + new String(response.getContent()));
			sendMessage(fromJID, err);
		}
	}
	
	
	/**
	 * 处理显示状态的HTTPResponse对象
	 */
	public static void StatusShowResp(JID fromJID, HTTPResponse response, int intType)
	{
		StatusShowResp(fromJID,response,intType,"1");
	}
	
}
