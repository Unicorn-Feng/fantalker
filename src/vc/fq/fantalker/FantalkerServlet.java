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
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.logging.Logger;
import javax.cache.CacheManager;
import javax.servlet.http.*;

import com.google.appengine.api.memcache.stdimpl.GCache;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.appengine.repackaged.org.json.JSONArray;
import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;


/**
 * 此类用于处理xmpp消息
 * 接收来自 /_ah/xmpp/message/chat/ 的消息
 */
@SuppressWarnings("serial")
public class FantalkerServlet extends HttpServlet 
{
	public static final String consumer_key = "4b6d4d676807ddb134b03e635e832baf";
	public static final String consumer_secret = "83e014d54b2923b9d2f4440d18f226bf";
	public static final String HMAC_SHA1 = "HmacSHA1";
	public static final Logger log = Logger.getLogger("Fantalker");
	public static final String NO_OAUTH = "您尚未绑定账号，请使用-oauth命令绑定";
	public static final String WRONG_CMD = "无效命令";
	public static final String UNKNOW_ERR = "未知错误";
	
	
	/**
	 * 用于处理GET请求
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException 
	{
		resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter().println("你不该来这里的，快回去！<br>");
		resp.getWriter().println("<a href='http://fantalker.appspot.com'>回主页</a>");
	}
	
	
	/**
	 * 用于处理POST请求，即接收到的xmpp消息
	 * 机器人主函数
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/html; charset=utf-8");
		
		XMPPService xmpp = XMPPServiceFactory.getXMPPService();
		Message message = xmpp.parseMessage(req);
		JID fromJID = message.getFromJid();										//发送者JID
		String msgbody = message.getBody();										//接收到的消息
		//msgbody = new String(msgbody.getBytes("ISO-8859-1"));
		//System.out.println(msgbody);
		
		msgbody = msgbody.trim();
		if(msgbody.isEmpty())
		{
			return;
		}
		char ch;
		ch = msgbody.charAt(0);	
		if(ch != '-')															//非命令
		{
			doSend(fromJID,msgbody);
		}
		else																	//命令
		{
			String[] msgarr = msgbody.split("\\s+");
			msgarr[0] = msgarr[0].toLowerCase();
			int intCmdID = getCmdID(msgarr[0]);
			switch (intCmdID)
			{
			case 1:																//-oauth
				doOauth(fromJID);
				break;
			case 2:																//-bind
				doBind(fromJID,msgarr);
				break;
			case 3:																//-remove
				doRemove(fromJID);
				break;
			case 4:																//-help
				doHelp(fromJID,msgarr);
				break;
			case 5:																//-reply/-@
				doReply(fromJID,msgarr,msgbody);
				break;
			case 6:																//-home
				doHome(fromJID,msgarr);
				break;
			case 7:																//-msg
				doMsg(fromJID,msgarr);
				break;
			case 8:																//-rt
				doRt(fromJID,msgarr,msgbody);
				break;
			case 9:																//-u
				doUser(fromJID,msgarr,msgbody);
				break;
			case -1:															//未知命令
			default:
				sendMessage(fromJID,WRONG_CMD);
				break;
			}
		}
	}
	
	
	/**
	 * 获取命令 ID
	 * 输入: 字符串，以-开头
	 * 返回: 命令ID
	 */
	public int getCmdID(String strCmd)
	{
		if(strCmd.equals("-oauth"))												//绑定账号
			return 1;
		else if(strCmd.equals("-bind"))											//接收PIN码
			return 2;
		else if(strCmd.equals("-remove"))										//解除绑定
			return 3;
		else if(strCmd.equals("-?") || strCmd.equals("-h") || strCmd.equals("-help"))	//显示帮助
			return 4;
		else if(strCmd.equals("-@") || strCmd.equals("-r") || strCmd.equals("-reply") || strCmd.equals("-mention")) //回复,提及
			return 5;
		else if(strCmd.equals("-home") || strCmd.equals("-ho"))					//显示时间线
			return 6;
		else if(strCmd.equals("-m") || strCmd.equals("-msg"))					//查看某条消息
			return 7;
		else if(strCmd.equals("-rt"))											//转发消息
			return 8;
		else if(strCmd.equals("-u") || strCmd.equals("-user"))					//显示用户信息
			return 9;
		else																	//未知命令
			return -1;
	}
	
	
	/**
	 * 向指定JID发送xmpp消息
	 * @param fromJID 来源JID
	 * @param strMessage 要发送的消息内容
	 */
	public void sendMessage(JID fromJID, String strMessage)
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
	 * 获取绑定的饭否ID
	 * @param fromJID 来源JID
	 * @return 字符串型饭否ID,未绑定返回null
	 */
	public String getID(JID fromJID)
	{
		return getData(fromJID,"id");
	}
	
	
	/**
	 * 从数据库中读取一个数据
	 * @param fromJID 来源JID
	 * @param strProperty 要读取数据的属性名
	 * @return 读取结果，无结果返回null
	 * @throws javax.cache.CacheException 
	 */
	public String getData(JID fromJID, String strProperty)
	{
		String strJID = getStrJID(fromJID);
		
		/* 从MemCache中读取数据 */
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
		
		/* 从Datastore中读取数据 */
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key k = KeyFactory.createKey("Account", strJID);
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
		try{
			GCacheFactory cacheFactory;
			cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			cache.put(strJID + "," + strProperty,strData);
		} catch (javax.cache.CacheException e){
			log.info(strJID + ":JCache " + e.getMessage());
		}
		return strData;
	}
	
	
	/**
	 * 通过是否有access_token判断是否已绑定账号
	 * @param fromJID 来源JID
	 * @return 已绑定返回true,并发送提示
	 */
	public boolean isOauth(JID fromJID)
	{
		if(getData(fromJID,"access_token") == null)
		{
			return false;
		}
		else
		{
			String id;
			id = getID(fromJID);
			if(id == null)
			{
				String strMessage = "您已经绑定饭否ID\n 若您希望绑定新的账号请先使用-remove命令取消现有绑定";
				sendMessage(fromJID, strMessage);
			}
			else
			{
				String strMessage = "您已经绑定饭否ID:" + id +" \n 若您希望绑定新的账号请先使用-remove命令取消现有绑定";
				sendMessage(fromJID, strMessage);
			}
			return true;
		}
	}
	
	
	/**
	 * 获取API对象
	 * @param fromJID
	 * @return API对象,若未绑定返回null
	 */
	public API getAPI(JID fromJID) throws NullPointerException
	{
		String oauth_key;
		String oauth_key_secret;
		oauth_key = getData(fromJID,"access_token");
		oauth_key_secret = getData(fromJID,"access_token_secret");
		if(oauth_key.isEmpty() || oauth_key_secret.isEmpty())
		{
			return null;
		}
		API api = new API(oauth_key,oauth_key_secret);
		return api;
	}
	
	
	/**
	 * 截取JID中有效的地址不符
	 * @param fromJID
	 * @return 字符串型JID
	 */
	public String getStrJID(JID fromJID)
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
	public String getSource(String source)
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
	 * 格式化时间
	 * @param strdate "Mon Mar 26 09:28:48 +0000 2012"
	 * @return "2012-03-26 09:28:48"
	 */
	public String getStrDate(String strdate)
	{
		String year,month,day,time,formdate;
		year = strdate.substring(26,30);
		month = strdate.substring(4,7);
		
		if(month.equals("Jan"))
			month = "01";
		else if(month.equals("Feb"))
			month = "02";
		else if(month.equals("Mar"))
			month = "03";
		else if(month.equals("Apr"))
			month = "04";
		else if(month.equals("May"))
			month = "05";
		else if(month.equals("Jun"))
			month = "06";
		else if(month.equals("Jul"))
			month = "07";
		else if(month.equals("Aug"))
			month = "08";
		else if(month.equals("Sep"))
			month = "09";
		else if(month.equals("Oct"))
			month = "10";
		else if(month.equals("Nov"))
			month = "11";
		else if(month.equals("Dec"))
			month = "12";
		
		day = strdate.substring(8,10);
		time = strdate.substring(11,19);
		formdate = year + "-" + month + "-" + day + " " + time;
		return formdate;
	}
	
	
	/**
	 * 执行-oauth命令完成认证
	 * @param fromJID 来源JID
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public void doOauth(JID fromJID) throws IOException
	{
		if(isOauth(fromJID))													//判断是否已绑定
		{
			return;
		}
		
		String strJID = getStrJID(fromJID);
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://fanfou.com/oauth/request_token");

		String params = null;
		params = "oauth_consumer_key=" + consumer_key 
					+ "&oauth_nonce=" + String.valueOf(nonce)
					+ "&oauth_signature_method=HMAC-SHA1"
					+ "&oauth_timestamp=" + String.valueOf(timestamp);

		params = "GET&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
		String sig = API.generateSignature(params);
				
		String authorization = null;
		authorization = "OAuth realm=\"Fantalker\",oauth_consumer_key=\"" + consumer_key
					+ "\",oauth_signature_method=\"HMAC-SHA1\""
					+ ",oauth_timestamp=\"" + String.valueOf(timestamp) + "\""
					+ ",oauth_nonce=\"" + String.valueOf(nonce) + "\""
					+ ",oauth_signature=\"" + sig + "\"";
		
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		
		if(response.getResponseCode() != 200)
		{
			String errMsg = "出现错误，请重试";
			sendMessage(fromJID,errMsg);
			log.warning(strJID + " :" + String.valueOf(response.getResponseCode()) + ": " + new String(response.getContent()));
			return;
		}
		
		/* 提取接收到的未经授权的Request Token */
		String tokenstring = new String(response.getContent());
		String[] tokenarr = tokenstring.split("&");
		String[] tokenarr2 = tokenarr[0].split("=");
		String oauth_token = tokenarr2[1];
		//tokenarr2 = tokenarr[1].split("=");
		//String oauth_token_secret = tokenarr2[1];

		/* 将接收到的Request Token存入数据库 */
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity account = new Entity("Account",strJID);
		account.setProperty("request_token", oauth_token);
		datastore.put(account);
		
		/* 将数据写入MemCache中 */
		GCache cache;
		try{
			GCacheFactory cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			cache.put(strJID + ",request_token",oauth_token);
		} catch (javax.cache.CacheException e){
			log.info(strJID + ":JCache " + e.getMessage());
		}
		
		/* 请求用户授权Request Token */
		String strMessage = "请访问以下网址获取PIN码: \n http://fanfou.com/oauth/authorize?oauth_token="
					+ oauth_token + "&oauth_callback=oob"
					+ " \n 手机用户请访问: \n http://m.fanfou.com/oauth/authorize?oauth_token="
					+ oauth_token + "&oauth_callback=oob"
					+ " \n 然后使用\"-bind PIN码\"命令绑定账号。";
		sendMessage(fromJID,strMessage);
	}
	
	
	/**
	 * 执行-bind命令绑定账号
	 * @param fromJID 来源JID
	 * @param msgarr 输入字符串数组
	 * @throws IOException 
	 */
	@SuppressWarnings("deprecation")
	public void doBind(JID fromJID, String[] msgarr) throws IOException
	{
		if(isOauth(fromJID))													//判断是否已绑定
		{
			return;
		}
		
		if(msgarr.length != 2)
		{
			sendMessage(fromJID, "用法： -bind PIN码\n提供PIN码用以完成OAuth认证。PIN码可以通过访问用-oauth命令获取到的链接后获得。");
			return;
		}
		if(msgarr[1].isEmpty())
		{
			sendMessage(fromJID, "用法： -bind PIN码\n提供PIN码用以完成OAuth认证。PIN码可以通过访问用-oauth命令获取到的链接后获得。");
			return;
		}
		String strJID = getStrJID(fromJID);
		String strPIN = msgarr[1];
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://fanfou.com/oauth/access_token");

		String oauth_token = getData(fromJID,"request_token");
		if(oauth_token == null | oauth_token.isEmpty())
		{
			sendMessage(fromJID,"未找到有效的Request Token\n请先使用-oauth命令获取PIN码。");
			return;
		}
		
		String params = null;
		params = "oauth_consumer_key=" + consumer_key
					+ "&oauth_nonce=" + String.valueOf(nonce)
					+ "&oauth_signature_method=HMAC-SHA1"
					+ "&oauth_timestamp=" + String.valueOf(timestamp)
					+ "&oauth_token=" + oauth_token
					+ "&oauth_verifier=" + strPIN;
		
		params = "GET&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
		String sig = API.generateSignature(params);
		
		String authorization = null;
		authorization = "OAuth realm=\"Fantalker\",oauth_consumer_key=\"" + consumer_key
					+ "\",oauth_signature_method=\"HMAC-SHA1\""
					+ ",oauth_timestamp=\"" + String.valueOf(timestamp) + "\""
					+ ",oauth_nonce=\"" + String.valueOf(nonce) + "\""
					+ ",oauth_signature=\"" + sig + "\""
					+ ",oauth_token=\"" + oauth_token + "\""
					+ ",oauth_verifier=\"" + strPIN + "\"";
		
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		
		if(response.getResponseCode() != 200)
		{
			String errMsg = "出现错误" + String.valueOf(response.getResponseCode()) + ": " + new String(response.getContent());
			sendMessage(fromJID,errMsg);
			log.warning(strJID + " :" + errMsg);
			return;
		}
		
		/* 提取接收到的未经授权的Request Token */
		String tokenstring = new String(response.getContent());
		String[] tokenarr = tokenstring.split("&");
		String[] tokenarr2 = tokenarr[0].split("=");
		oauth_token = tokenarr2[1];
		tokenarr2 = tokenarr[1].split("=");
		String oauth_token_secret = tokenarr2[1];

		/* 将接收到的Request Token存入数据库 */
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity account = new Entity("Account",strJID);
		account.setProperty("access_token", oauth_token);
		account.setProperty("access_token_secret",oauth_token_secret);
		
		API api = new API(oauth_token,oauth_token_secret);
		response = api.account_verify_credentials(fromJID);
		String id = null;
		if(response.getResponseCode() == 200)
		{
			try {
				JSONObject respJSON = new JSONObject(new String(response.getContent()));
				id = respJSON.getString("id");
			} catch (JSONException e) {
				log.info(strJID + ":JSONid " + e.getMessage());
			}
		}
		if(id == null)															//失败重试
		{
			response = api.account_verify_credentials(fromJID);
			if(response.getResponseCode() == 200)
			{
				try {
					JSONObject respJSON = new JSONObject(new String(response.getContent()));
					id = respJSON.getString("id");
				} catch (JSONException e) {
					log.info(strJID + ":JSONid " + e.getMessage());
				}
			}
		}
		
		if(id == null)
		{
			datastore.put(account);
			sendMessage(fromJID,"成功绑定，但在获取饭否ID时出现未知错误");
			log.warning(strJID + ": " + new String(response.getContent()));
		}
		else
		{
			account.setProperty("id", id);
			datastore.put(account);
			sendMessage(fromJID,"成功与饭否账号 " + id + " 绑定");
		}
		
		/* 将数据写入MemCache中 */
		GCache cache;
		try{
			GCacheFactory cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			cache.put(strJID + ",access_token",oauth_token);
			cache.put(strJID + ",access_token_secret",oauth_token_secret);
		} catch (javax.cache.CacheException e){
			log.info(strJID + ":JCache " + e.getMessage());
		}

	}
	
	
	/**
	 * 执行-remove命令解除账号绑定
	 * @param fromJID
	 */
	public void doRemove(JID fromJID)
	{
		if(getData(fromJID,"access_token") == null)
		{
			sendMessage(fromJID,NO_OAUTH);
			return;
		}
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		String strJID = getStrJID(fromJID);
		Key k = KeyFactory.createKey("Account", strJID);
		datastore.delete(k);
		sendMessage(fromJID,"您已成功解除账号绑定，请使用-oauth命令再次绑定账号");
	}

	
	/**
	 * 执行-@ 命令回复或获取回复
	 * @param fromJID
	 * @param msgarr 输入字符串数组
	 * @param strMessage 完整输入字符串
	 * @throws IOException
	 */
	public void doReply(JID fromJID, String[] msgarr, String strMessage) throws IOException
	{
		API api = getAPI(fromJID);
		if(api == null)
		{
			sendMessage(fromJID,NO_OAUTH);
			return;
		}
		HTTPResponse response;
		int msgarr_len = msgarr.length;
		if(msgarr_len == 1)
		{
			response = api.statuses_mentions(fromJID);
			StatusShowResp(fromJID, response,2);
		}
		else if(msgarr_len == 2)												//-@ p2
		{
			char ch;
			ch = msgarr[1].charAt(0);
			if(ch != 'p' && ch != 'P')
			{
				sendMessage(fromJID,WRONG_CMD);
				return;
			}
			msgarr[1] = msgarr[1].substring(1);
			if(!isNumeric(msgarr[1]))
			{
				sendMessage(fromJID,WRONG_CMD);
				return;
			}
			response = api.statuses_mentions(fromJID, msgarr[1]);
			StatusShowResp(fromJID, response,2,msgarr[1]);
		}
		else																	//-@ -WNEO5ZQt28 test t
		{
			int intIndex = strMessage.lastIndexOf(msgarr[1]) + msgarr[1].length() + 1;
			String replyMsg = strMessage.substring(intIndex);
			response = api.statuses_reply(fromJID, replyMsg, msgarr[1]);
			if(response.getResponseCode() == 200)
			{
				StatusJSON jsonStatus = new StatusJSON(new String(response.getContent()));
				sendMessage(fromJID,"成功转发\n  " + jsonStatus.getText());
			}
			else if(response.getResponseCode() == 400)
			{
				try {
					JSONObject json = new JSONObject(new String(response.getContent()));
					String error = json.getString("error");
					sendMessage(fromJID,error);
					
				} catch (JSONException e) {
					//e.printStackTrace();
					log.warning(getStrJID(fromJID) + " rt:" + new String(response.getContent()));
					sendMessage(fromJID,"未知错误");
				}
			}
			else
			{
				sendMessage(fromJID,"未知错误");
				log.warning(getStrJID(fromJID) + " rt:" + new String(response.getContent()));
			}
		}
	}
	
	
	/**
	 * 执行-home命令
	 * @param fromJID
	 * @param msgarr
	 * @throws IOException 
	 */
	public void doHome(JID fromJID, String[] msgarr) throws IOException
	{
		API api = getAPI(fromJID);
		if(api == null)
		{
			sendMessage(fromJID,NO_OAUTH);
			return;
		}
		HTTPResponse response;
		int msgarr_len = msgarr.length;
		if(msgarr_len == 1)
		{
			response = api.statuses_home_timeline(fromJID);
			StatusShowResp(fromJID, response,1);
		}
		else if(msgarr_len == 2)												//-ho p2
		{
			char ch;
			ch = msgarr[1].charAt(0);
			if(ch != 'p' && ch != 'P')
			{
				sendMessage(fromJID,WRONG_CMD);
				return;
			}
			msgarr[1] = msgarr[1].substring(1);
			if(!isNumeric(msgarr[1]))
			{
				sendMessage(fromJID,WRONG_CMD);
				return;
			}
			response = api.statuses_home_timeline(fromJID,msgarr[1]);
			StatusShowResp(fromJID, response,1,msgarr[1]);
		}
		else
		{
			sendMessage(fromJID,WRONG_CMD);
		}
	}
	
	
	/**
	 * 执行-help 显示帮助
	 * @param fromJID
	 * @param msgarr 命令字符串数组
	 */
	public void doHelp(JID fromJID, String[] msgarr)
	{
		String helpMsg = null;
		if(msgarr.length == 1)
		{
			helpMsg = "Fantalker目前支持以下命令:\n"
					+ "您可以通过-?/-h/-help COMMAND查询每条命令的详细用法（方括号代表参数可选）:\n"
					+ "-ho/-home： 查看首页时间线\n"
					+ "-@/-r-/-reply：查看提到我的消息及 回复消息\n"
					+ "-rt：转发消息\n"
					+ "-u： 查看用户信息\n"
					+ "-m/-msg： 查看指定消息\n"
					+ "-oauth： 开始OAuth认证\n"
					+ "-bind： 绑定PIN码完成认证\n"
					+ "-remove： 解除关联\n"
					+ "-?/-h/-help： 显示本帮助\n"
					+ "\n如果在使用中发现有BUG，请及时联系我@烽麒 谢谢～";
		}
		else if(msgarr.length == 2)
		{
			msgarr[1] = "-" + msgarr[1];
			
			switch (getCmdID(msgarr[1]))
			{
			case 1:																//-oauth
				helpMsg = "用法: -oauth\n获取用于OAuth的链接\n";
				break;
			case 2:																//-bind
				helpMsg = "用法: -bind PIN码\n提供PIN码用以完成OAuth认证。PIN码可以通过访问用-oauth命令获取到的链接后获得\n";
				break;
			case 3:																//-remove
				helpMsg = "用法: -remove\n解除现有账户绑定\n";
				break;
			case 4:																//-help
				helpMsg = "用法: -help [COMMOND]\n显示帮助\n";
				break;
			case 5:																//-reply/-@
				helpMsg = "用法1: -@/-r/-reply 消息ID 内容\n"
						+ "回复消息ID所指定的消息\n"
						+ "用法2: -@/-r/-reply [p页码]\n"
						+ "显示@你的消息，页码可选\n命令如-@ p2\n";
				break;
			case 6:																//-home
				helpMsg = "用法: -home [p页码]\n显示时间线，页码可选\n";
				break;
			case 7:																//-msg
				helpMsg = "用法: -msg 消息ID\n显示消息ID所指定的消息\n";
				break;
			case 8:																//-rt
				helpMsg = "用法: -rt 消息ID 内容\n转发指定消息ID的消息\n";
				break;
			case 9:																//-u
				helpMsg = "用法: -u [用户id]\n显示指定用户的详细信息，不加参数则显示已绑定用户的信息\n";
				break;
			case -1:															//未知命令
			default:
				helpMsg = "无此命令\n";
				break;
			}
		}
		else
		{
			sendMessage(fromJID,WRONG_CMD);
			return;
		}
		sendMessage(fromJID,helpMsg);
	}
	
	
	/**
	 * 发布状态
	 * @param fromJID
	 * @param message 状态内容
	 * @throws IOException 
	 */
	public void doSend(JID fromJID, String message) throws IOException
	{
		API api = getAPI(fromJID);
		if(api == null)
		{
			sendMessage(fromJID,NO_OAUTH);
			return;
		}
		HTTPResponse response;
		response = api.statuses_send(fromJID,message);
		if(response.getResponseCode() == 200)
		{
			sendMessage(fromJID,"成功发布状态");
		}
		else
		{
			sendMessage(fromJID,"发送失败");
			log.warning(getStrJID(fromJID) + "status.send: " + new String(response.getContent()));
		}
	}
	
	
	/**
	 * 执行-m命令显示某条消息
	 * @param fromJID
	 * @param msgarr
	 * @throws IOException
	 */
	public void doMsg(JID fromJID, String[] msgarr) throws IOException
	{
		API api = getAPI(fromJID);
		if(api == null)
		{
			sendMessage(fromJID,NO_OAUTH);
			return;
		}
		if(msgarr.length == 2)													//-m 7rXy196_C3k
		{
			HTTPResponse response;
			response = api.statuses_show(fromJID, msgarr[1]);
			if(response.getResponseCode() == 200)
			{
				StatusJSON jsonStatus = new StatusJSON(new String(response.getContent()));
				sendMessage(fromJID,StatusMessage("",jsonStatus));
			}
			else if(response.getResponseCode() == 403)
			{
				sendMessage(fromJID,"你没有通过这个用户的验证,无法查看TA的消息");
			}
			else if(response.getResponseCode() == 404)
			{
				sendMessage(fromJID,"id所指定的消息不存在");
			}
			else
			{
				sendMessage(fromJID,"未知错误");
				log.warning(getStrJID(fromJID) + "-msg: " + new String(response.getContent()));
			}
		}
		else
		{
			sendMessage(fromJID,WRONG_CMD);
		}
	}
	
	
	/**
	 * 执行-rt 命令转发消息
	 * @param fromJID
	 * @param msgarr 输入字符串数组
	 * @param strMessage 完整输入字符串
	 * @throws IOException
	 */
	public void doRt(JID fromJID, String[] msgarr, String strMessage) throws IOException
	{
		API api = getAPI(fromJID);
		if(api == null)
		{
			sendMessage(fromJID,NO_OAUTH);
			return;
		}
		HTTPResponse response;
		int msgarr_len = msgarr.length;
		if(msgarr_len == 1)
		{
			response = api.statuses_mentions(fromJID);
			StatusShowResp(fromJID, response,2);
		}
		else																	//-rt -WNEO5ZQt28 test t
		{
			int intIndex = strMessage.lastIndexOf(msgarr[1]) + msgarr[1].length() + 1;
			String replyMsg = strMessage.substring(intIndex);
			response = api.statuses_repost(fromJID, replyMsg, msgarr[1]);
			if(response.getResponseCode() == 200)
			{
				StatusJSON jsonStatus = new StatusJSON(new String(response.getContent()));
				sendMessage(fromJID,"成功转发\n  " + jsonStatus.getText());
			}
			else if(response.getResponseCode() == 400)
			{
				try {
					JSONObject json = new JSONObject(new String(response.getContent()));
					String error = json.getString("error");
					sendMessage(fromJID,error);
					
				} catch (JSONException e) {
					//e.printStackTrace();
					log.info(getStrJID(fromJID) + " rt:" + new String(response.getContent()));
					sendMessage(fromJID,"未知错误");
				}
			}
			else
			{
				sendMessage(fromJID,"未知错误");
				log.warning(getStrJID(fromJID) + " rt:" + new String(response.getContent()));
			}
		}
	}
	
	
	/**
	 * 执行-u查看用户信息
	 * @param fromJID
	 * @param msgarr 输入字符串数组
	 * @param strMessage 完整输入字符串
	 * @throws IOException
	 */
	public void doUser(JID fromJID, String[] msgarr, String strMessage) throws IOException
	{
		API api = getAPI(fromJID);
		if(api == null)
		{
			sendMessage(fromJID,NO_OAUTH);
			return;
		}
		HTTPResponse response;
		if(msgarr.length == 1)													//-u
		{
			response = api.account_verify_credentials(fromJID);
		}
		else if(msgarr.length == 2)													//-u id
		{
			response = api.users_show(fromJID, msgarr[1]);
		}
		else
		{
			sendMessage(fromJID,WRONG_CMD);
			return;
		}
		
		if(response.getResponseCode() == 200)
		{
			UserJSON jsonUser = new UserJSON(new String(response.getContent()));
			String message = null;
			message = jsonUser.getScreenName() + " (" + jsonUser.getId() + ") 的信息\n"
					+ "头像地址: " + jsonUser.getProfileImageUrl()
					+ "\n性别: " + jsonUser.getGender()
					+ "\n自述: " + jsonUser.getDescription()
					+ "\n地址: " + jsonUser.getLocation()
					+ "\nTA关注的人数: " + String.valueOf(jsonUser.getFriendsCount())
					+ "\n关注TA的人数: " + String.valueOf(jsonUser.getFollowersCount())
					+ "\n收藏消息数: " + String.valueOf(jsonUser.getFavouritesCount())
					+ "\n已发消息数: " + String.valueOf(jsonUser.getStatusesCount());
			if(jsonUser.getIsFollowing())
			{
				message = message + "\n\n您已经关注了" + jsonUser.getId();
			}
			else
			{
				message = message + "\n\n您没有关注" + jsonUser.getId();
			}
			sendMessage(fromJID,message);
		}
		else if(response.getResponseCode() == 403)
		{
			sendMessage(fromJID,"你没有通过这个用户的验证,无法查看TA的信息");
		}
		else if(response.getResponseCode() == 404)
		{
			sendMessage(fromJID,"id所指定的用户不存在");
		}
		else
		{
			sendMessage(fromJID,"未知错误");
			log.warning(getStrJID(fromJID) + "-u: " + new String(response.getContent()));
		}
	}
	
	
	/**
	 * xmpp中输出消息
	 * @param fromJID 来源JID
	 * @param jsonStatus StatusJSON对象
	 * @param intType 消息类型,1时间线,2提到我的
	 * @param pageID 页码
	 * @param lenght jsonStatus数组长度
	 */
	public void showStatus(JID fromJID, StatusJSON[] jsonStatus, int intType, int length, String pageID)
	{
		String strMessage = null;
		if(intType == 1)
		{
			strMessage = "时间线: 第" + pageID + "页\n\n";
		}
		else
		{
			strMessage = "提到我的: 第" + pageID + "页\n\n";
		}
		for(int i=(length-1);i>=0;i--)
		{
			strMessage = StatusMessage(strMessage,jsonStatus[i]);
		}
		sendMessage(fromJID,strMessage);
	}
	
	
	/**
	 * xmpp中输出消息
	 * @param fromJID 来源JID
	 * @param jsonStatus StatusJSON对象
	 * @param intType 消息类型,1时间线,2提到我的
	 */
	public void showStatus(JID fromJID, StatusJSON[] jsonStatus, int intType, int length)
	{
		showStatus(fromJID,jsonStatus,intType,length,"1");
	}
	
	
	/**
	 * 处理显示状态的HTTPResponse对象
	 * @param fromJID
	 * @param response
	 * @param intType 消息类型,1时间线,2提到我的
	 * @param pageID 页码
	 */
	public void StatusShowResp(JID fromJID, HTTPResponse response, int intType, String pageID)
	{
		if(response.getResponseCode() == 200)
		{
			try {
				JSONArray jsonarr = new JSONArray(new String(response.getContent()));
				StatusJSON[] jsonStatus = new StatusJSON[20];
				int arrlen = jsonarr.length();
				if(arrlen == 0)
				{
					sendMessage(fromJID,"无更多消息");
					return;
				}
				for(int i=0;i<arrlen;i++)
				{
					jsonStatus[i] = new StatusJSON(jsonarr.getJSONObject(i));
				}
				showStatus(fromJID,jsonStatus,intType,arrlen,pageID);
			} catch (JSONException e) {
				//e.printStackTrace();
				log.info("status.show.JSON " + e.getMessage());
			}
		}
		else
		{
			log.warning("status.show " + new String(response.getContent()));
			sendMessage(fromJID,UNKNOW_ERR);
		}
	}
	
	
	public void StatusShowResp(JID fromJID, HTTPResponse response, int intType)
	{
		StatusShowResp(fromJID,response,intType,"1");
	}
	
	
	/**
	 * 构造xmpp消息
	 * @param message 现有消息
	 * @param jsonStatus 状态JSON对象
	 * @return
	 */
	public String StatusMessage(String message, StatusJSON jsonStatus)
	{
		UserJSON jsonUser = jsonStatus.getUserJSON();
		message = message + jsonUser.getScreenName() + ": " + jsonStatus.getText()
					+ "\n [ " + jsonStatus.getID() + " ] " + getStrDate(jsonStatus.getCreatedAt())
					+ " <- " + getSource(jsonStatus.getSource()) + "\n\n";
		return message;
	}

	
	/**
	 * 通过正则表达式判断字符串是否为数字
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
	
}
