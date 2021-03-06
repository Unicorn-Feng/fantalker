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
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.xmpp.JID;

/**
 * 连接饭否API
 * @author 烽麒 Unicorn-Feng
 * @link http://fq.vc
 */
public class API 
{
	public static final String consumer_key = "4b6d4d676807ddb134b03e635e832baf";
	public static final String consumer_secret = "83e014d54b2923b9d2f4440d18f226bf";
	public static final String HMAC_SHA1 = "HmacSHA1";
	
	private String oauth_token;
	private String oauth_token_secret;
	
	/**
	 * 构造函数 
	 * @param oauthtoken 用户Access Token
	 * @param oauthtokensecret 用户Access Token Secret
	 */
	public API(String oauthtoken, String oauthtokensecret)
	{
		oauth_token = oauthtoken;
		oauth_token_secret = oauthtokensecret;
	}
	
	
	/**
	 * 调用 GET/POST /account/verify_credentials 检查用户名密码是否正确
	 * @param fromJID 来源JID
	 * @return HTTPResponse，包含json
	 * @throws IOException 
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/account.verify-credentials
	 */
	public HTTPResponse account_verify_credentials(JID fromJID) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/account/verify_credentials.json");

		String params = generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		return response;
	}
	
	
	/**
	 * 调用 GET /favorites 浏览收藏消息
	 * @param fromJID 来源JID
	 * @param pageID 指定返回结果的页码
	 * @return HTTPResponse 包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/favorites
	 */
	public HTTPResponse favorites(JID fromJID, String pageID) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/favorites/id.json");
	
		String params = generateParams(timestamp,nonce);
		if(pageID != null)
		{
			params = params + "&page=" + pageID;
		}
		
		params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request;
		if(pageID == null)
		{
			request = new HTTPRequest(url,HTTPMethod.GET);
		}
		else
		{
			request = new HTTPRequest(new URL(url.toString() + "?page=" + pageID),HTTPMethod.GET);
		}
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		return response;
	}
	
	
	/**
	 * 调用 GET /favorites 浏览收藏消息
	 * @param fromJID 来源JID
	 * @return 包含json
	 * @throws IOException
	 * @throws SocketTimeoutException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/favorites
	 */
	public HTTPResponse favorites(JID fromJID) throws IOException,SocketTimeoutException 
	{
		return favorites(fromJID,null);
	}
	
	
	/**
	 * 调用POST /favorites/create(destroy) 添加/删除收藏
	 * @param fromJID
	 * @param id 指定需要添加/删除收藏的消息id
	 * @param fav 收藏或取消收藏
	 * @return HTTPResponse，包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/favorites.create
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/favorites.destroy
	 */
	public HTTPResponse favorites_create_destroy(JID fromJID, String id, boolean fav) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		
		URL url;
		if(fav)
		{
			url = new URL("http://api.fanfou.com/statuses/favorites/create/" + id + ".json");
		}
		else
		{
			url = new URL("http://api.fanfou.com/statuses/favorites/destroy/id.json");
		}
		
		String params;
		params = "id=" + id + "&" + generateParams(timestamp,nonce);
		//params = generateParams(timestamp,nonce);
		params = "POST&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		System.out.println(params);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.POST);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		request.addHeader(new HTTPHeader("Content-Type","application/x-www-form-urlencoded"));

		String strPayload = "id=" + id;
		request.setPayload(strPayload.getBytes());
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		
		return response;
	}
	
	
	/**
	 * 调用POST /friendships/create(destroy) 添加/删除用户为好友
	 * @param fromJID
	 * @param id 指定需要添加/删除的好友的user_id，或者loginname
	 * @return HTTPResponse，包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/friendships.create
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/friendships.destroy
	 */
	public HTTPResponse friendships_create_destroy(JID fromJID, String id, boolean fo) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		
		URL url;
		if(fo)
		{
			url = new URL("http://api.fanfou.com/friendships/create.json");
		}
		else
		{
			url = new URL("http://api.fanfou.com/friendships/destroy.json");
		}
		
		String params;
		params = "id=" + id + "&" + generateParams(timestamp,nonce);
		params = "POST&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.POST);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		request.addHeader(new HTTPHeader("Content-Type","application/x-www-form-urlencoded"));
		
		String strPayload = "id=" + id;
		request.setPayload(strPayload.getBytes());
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		
		return response;
	}


	/**
	 * 生成OAuth请求头字符串
	 * @param timestamp 时间戳，取当前时间
	 * @param nonce 单次值，随机的字符串，防止重复请求
	 * @param signature 签名值
	 * @return OAuth头字符串
	 */
	public String generateAuthString(long timestamp, long nonce, String signature)
	{
		StringBuffer strBuf = new StringBuffer(280); 
		strBuf.append("OAuth realm=\"Fantalker\",oauth_consumer_key=\"");
		strBuf.append(consumer_key);
		strBuf.append("\",oauth_signature_method=\"HMAC-SHA1\"");
		strBuf.append(",oauth_timestamp=\"").append(timestamp).append("\"");
		strBuf.append(",oauth_nonce=\"").append(nonce).append("\"");
		strBuf.append(",oauth_signature=\"").append(signature).append("\"");
		strBuf.append(",oauth_token=\"").append(oauth_token).append("\"");
		return strBuf.toString();
	}


	/**
	 * 生成oauth部分的params字符串
	 * @param timestamp
	 * @param nonce
	 * @return
	 */
	public String generateParams(long timestamp, long nonce)
	{
		StringBuffer strBuf = new StringBuffer(200); 
		strBuf.append("oauth_consumer_key=").append(consumer_key);
		strBuf.append("&oauth_nonce=").append(nonce);
		strBuf.append("&oauth_signature_method=HMAC-SHA1");
		strBuf.append("&oauth_timestamp=").append(timestamp);
		strBuf.append("&oauth_token=").append(oauth_token);
		return strBuf.toString();
	}

	
	/**
	 * 调用GET /search/public_timeline 搜索全站消息
	 * @param fromJID
	 * @param query_word 搜索关键词，多个关键词用|分割
	 * @return
	 * @throws SocketTimeoutException
	 * @throws IOException
	 */
	public HTTPResponse search_public_timeline(JID fromJID, String query_word) throws SocketTimeoutException,IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		String strURL = "http://api.fanfou.com/search/public_timeline.json";

		String params = null;
		params = generateParams(timestamp,nonce) + "&q=" + URLEncoder.encode(query_word,"UTF-8");
		params = "GET&" + URLEncoder.encode(strURL,"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		params = Common.replaceEncode(params);
		
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request;
		request = new HTTPRequest(new URL(strURL + "?q=" + URLEncoder.encode(query_word,"UTF-8")),HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		return response;
	}

	
	/**
	 * 调用GET /statuses/context_timeline 按照时间先后顺序显示消息上下文
	 * @param fromJID 来源JID
	 * @param id 指定消息ID
	 * @return HTTPResponse 包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.context-timeline
	 */
	public HTTPResponse statuses_context_timeline(JID fromJID, String id) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/context_timeline.json");

		String params = null;
		params = "id=" + id	+ "&" + generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request;
		request = new HTTPRequest(new URL(url.toString() + "?id=" + id),HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		return response;
	}


	/**
	 * 调用 POST /statuses/destroy 删除指定的消息
	 * @param fromJID 来源JID
	 * @param id 指定需要删除的消息id
	 * @return HTTPResponse，包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.destroy
	 */
	public HTTPResponse statuses_destroy(JID fromJID, String id) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/destroy.json");
		
		String params = null;
		params = "id=" + id + "&" + generateParams(timestamp,nonce);
		params = "POST&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.POST);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		request.addHeader(new HTTPHeader("Content-Type","application/x-www-form-urlencoded"));
		
		String strPayload = "id=" + id;
		request.setPayload(strPayload.getBytes());
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		
		return response;
	}


	/**
	 * 调用 GET /status/home_timeline 显示指定用户及其好友的消息
	 * @param fromJID 来源JID
	 * @param pageID 指定返回结果的页码
	 * @return HTTPResponse 包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.home-timeline
	 */
	public HTTPResponse statuses_home_timeline(JID fromJID, String pageID) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/home_timeline.json");
	
		String params = generateParams(timestamp,nonce);
		if(pageID != null)
		{
			params = params + "&page=" + pageID;
		}
		
		params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request;
		if(pageID == null)
		{
			request = new HTTPRequest(url,HTTPMethod.GET);
		}
		else
		{
			request = new HTTPRequest(new URL(url.toString() + "?page=" + pageID),HTTPMethod.GET);
		}
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		return response;
	}


	/**
	 * 调用 GET /status/home_timeline 显示指定用户及其好友的消息
	 * @param fromJID 来源JID
	 * @return HTTPResponse 包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.home-timeline
	 */
	public HTTPResponse statuses_home_timeline(JID fromJID) throws IOException,SocketTimeoutException 
	{
		return statuses_home_timeline(fromJID,null);
	}


	/**
	 * 调用 GET /statuses/mentions 显示回复/提到当前用户的20条消息
	 * @param fromJID 来源JID
	 * @param pageID 返回结果的页码
	 * @param since_id 只返回消息id大于since_id的消息
	 * @return HTTPResponse，包含json
	 * @throws IOException 
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.mentions
	 */
	public HTTPResponse statuses_mentions(JID fromJID, String pageID, String since_id) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		String strurl = "http://api.fanfou.com/statuses/mentions.json";
	
		String params = generateParams(timestamp,nonce);
		if(pageID != null)
		{
			params = params + "&page=" + pageID;
		}
		if(since_id != null)
		{
			params = params + "&since_id=" + since_id;
		}
		params = "GET&" + URLEncoder.encode(strurl,"UTF-8")
				+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request;
		if(pageID != null)
		{
			strurl = strurl + "?page=" + pageID;
		}
		if(since_id != null)
		{
			strurl = strurl + "?since_id=" + since_id;
		}
		URL url = new URL(strurl);
		request = new HTTPRequest(url,HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		
		return response;
	}


	/**
	 * 调用 GET /statuses/mentions 显示回复/提到当前用户的20条消息
	 * @param fromJID 来源JID
	 * @return HTTPResponse 包含json
	 * @throws IOException 
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.mentions
	 */
	public HTTPResponse statuses_mentions(JID fromJID) throws IOException,SocketTimeoutException 
	{
		return 	statuses_mentions(fromJID,null,null);
	}
	
	
	/**
	 * 调用 GET /statuses/mentions 显示回复/提到当前用户的20条消息
	 * @param fromJID 来源JID
	 * @param pageID 返回结果的页码
	 * @return HTTPResponse，包含json
	 * @throws IOException 
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.mentions
	 */
	public HTTPResponse statuses_mentions(JID fromJID,String pageID) throws IOException,SocketTimeoutException 
	{
		return 	statuses_mentions(fromJID,pageID,null);
	}

	
	/**
	 * 调用GET /statuses/public_timeline 显示20条随便看看的消息
	 * @param fromJID
	 * @return
	 * @throws IOException
	 */
	public HTTPResponse statuses_public_timeline(JID fromJID) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/public_timeline.json");
	
		String params = generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request;
		request = new HTTPRequest(url,HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		return response;
	}
	

	/**
	 * 调用 POST /statuses/update 回复消息
	 * @param fromJID 来源JID
	 * @param strMessage 要发送的消息
	 * @param replyID in_reply_to_status_id 回复的消息id
	 * @return HTTPResponse，包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.update
	 */
	public HTTPResponse statuses_reply(JID fromJID, String strMessage, String replyID) throws IOException,SocketTimeoutException 
	{
		HTTPResponse response;
		response = statuses_show(fromJID, replyID);
		if(response.getResponseCode() == 200)
		{
			StatusJSON jsonStatus = new StatusJSON(new String(response.getContent()));
			String userid = jsonStatus.getUserJSON().getId();
			strMessage = "@" + jsonStatus.getUserJSON().getScreenName() + " " + strMessage;
			return statuses_update(fromJID, strMessage, replyID, userid, replyID);
		}
		else
		{
			Common.sendMessage(fromJID,"回复失败:" + Common.getError(new String(response.getContent())));
			Common.log.info(Common.getStrJID(fromJID) + "-r: " + Common.getError(new String(response.getContent())));
			return null;
		}
	}


	/**
	 * 调用 POST /statuses/update 转发消息
	 * @param fromJID 来源JID
	 * @param strMessage 要发送的消息
	 * @param repostID repost_status_id 转发的消息id
	 * @return HTTPResponse，包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.update
	 */
	public HTTPResponse statuses_repost(JID fromJID, String strMessage, String repostID) throws IOException,SocketTimeoutException 
	{
		HTTPResponse response;
		response = statuses_show(fromJID, repostID);
		if(response.getResponseCode() == 200)
		{
			StatusJSON jsonStatus = new StatusJSON(new String(response.getContent()));
			String userid = jsonStatus.getUserJSON().getId();
			strMessage = strMessage + " 转@" + jsonStatus.getUserJSON().getScreenName()
					+ " " + jsonStatus.getText();
			return statuses_update(fromJID, strMessage, repostID, userid, repostID);
		}
		else
		{
			Common.log.info(Common.getStrJID(fromJID) + "-rt: " + new String(response.getContent()));
			return null;
		}
	}


	/**
	 * 调用 POST /statuses/update 发送消息
	 * @param fromJID 来源JID
	 * @param strMessage 要发送的消息
	 * @return HTTPResponse，包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.update
	 */
	public HTTPResponse statuses_send(JID fromJID, String strMessage) throws IOException,SocketTimeoutException 
	{
		return statuses_update(fromJID,strMessage,null,null,null);
	}
	
	
	/**
	 * 调用GET /statuses/show 返回好友或未设置隐私用户的某条消息
	 * @param fromJID
	 * @param id 指定需要浏览的消息id
	 * @return HTTPResponse，包含json
	 * @throws IOException 
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.show
	 */
	public HTTPResponse statuses_show(JID fromJID, String id) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/show.json");

		String params = null;
		params = "id=" + id	+ "&" + generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request;
		request = new HTTPRequest(new URL(url.toString() + "?id=" + id),HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		return response;
	}
	
	
	/**
	 * 调用 POST /statuses/update 发送消息
	 * @param fromJID 来源JID
	 * @param strMessage 要发送的消息
	 * @param replyID in_reply_to_status_id 回复的消息id
	 * @param userID in_reply_to_user_id 回复的用户id
	 * @param repostID repost_status_id 转发的消息id
	 * @return HTTPResponse，包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.update
	 */
	public HTTPResponse statuses_update(JID fromJID, String strMessage, String replyID, String userID, String repostID) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/update.json");
		
		String params = null;
		
		if(replyID != null && userID !=null)
		{
			params = "in_reply_to_status_id=" + replyID 
					+ "&in_reply_to_user_id=" + userID
					+ "&" + generateParams(timestamp,nonce);
		}
		else
		{
			params = generateParams(timestamp,nonce);
		}
	
		if(repostID != null)
		{
			params = params + "&repost_status_id=" + repostID;
		}
		params = params + "&status=" + URLEncoder.encode(strMessage,"UTF-8").replaceAll("\\+","%20");
		
		params = "POST&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		params = Common.replaceEncode(params);
		
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.POST);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		request.addHeader(new HTTPHeader("Content-Type","application/x-www-form-urlencoded"));
		
		String strPayload;
		strPayload = "status=" + URLEncoder.encode(strMessage,"UTF-8");
		if(replyID != null && userID != null)
		{
			strPayload = strPayload + "&in_reply_to_status_id=" + replyID + "&in_reply_to_user_id=" + userID;
		}
		if(repostID != null)
		{
			strPayload = strPayload + "&repost_status_id=" + repostID;
		}
		request.setPayload(strPayload.getBytes());
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		
		return response;
	}

	
	/**
	 * 调用 GET /statuses/user_timeline 浏览指定用户已发送消息
	 * @param fromJID 来源JID
	 * @param userID 目标用户的id
	 * @param pageID 指定返回结果的页码
	 * @return
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.user-timeline
	 * @throws IOException
	 */
	public HTTPResponse statuses_user_timeline(JID fromJID, String userID, String pageID) throws IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		String strURL = "http://api.fanfou.com/statuses/user_timeline.json";
		String params = "";
		if(userID != null)
		{
			params = "id=" + userID + "&";
		}
		params = params + generateParams(timestamp,nonce);

		if(pageID != null)
		{
			params = params + "&page=" + pageID;
		}
		
		params = "GET&" + URLEncoder.encode(strURL,"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");

		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request;

		if(userID == null && pageID == null)
		{
			request = new HTTPRequest(new URL(strURL),HTTPMethod.GET);
		}
		else if(userID == null && pageID != null)
		{
			strURL = strURL + "?page=" + pageID;

		}
		else if(userID != null && pageID == null)
		{
			strURL = strURL + "?id=" + userID;
		}
		else if(userID != null && pageID != null)
		{
			strURL = strURL + "?id=" + userID + "&page=" + pageID;
		}
		request = new HTTPRequest(new URL(strURL),HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		return response;
	}

	
	/**
	 * 调用 GET /statuses/user_timeline 浏览指定用户已发送消息
	 * @param fromJID 来源JID
	 * @return
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.user-timeline
	 * @throws IOException
	 */
	public HTTPResponse statuses_user_timeline(JID fromJID) throws IOException
	{
		return statuses_user_timeline(fromJID, null, null);
	}
	
	
	/**
	 * 调用 GET /statuses/user_timeline 浏览指定用户已发送消息
	 * @param fromJID 来源JID
	 * @param pageID 指定返回结果的页码
	 * @return
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.user-timeline
	 * @throws IOException
	 */
	public HTTPResponse statuses_user_timeline(JID fromJID, String pageID) throws IOException
	{
		return statuses_user_timeline(fromJID, null, pageID);
	}
	
	
	/**
	 * 调用 GET /users/show 返回用户的信息
	 * @param fromJID 来源JID
	 * @param id 指定的用户id
	 * @return HTTPResponse，包含json
	 * @throws IOException 
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/users.show
	 */
	public HTTPResponse users_show(JID fromJID, String id) throws IOException,SocketTimeoutException 
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/users/show.json");
	
		String params = null;
		params = "id=" + id + "&" + generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request = new HTTPRequest(new URL(url.toString() + "?id=" + id),HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		URLFetchService service = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse response = service.fetch(request);
		
		return response;
	}

	
    /**
     * Computes RFC 2104-compliant HMAC signature.
	 * @author Yusuke Yamamoto - yusuke at mac.com
	 * @edit Unicorn-Feng
	 * @see <a href="http://oauth.net/core/1.0/">OAuth Core 1.0</a>
     * @param data the data to be signed
     * @param access token secret
     * @return signature
     * @throws UnsupportedEncodingException 
     * @see <a href="http://oauth.net/core/1.0/#rfc.section.9.2.1">OAuth Core - 9.2.1.  Generating Signature</a>
     */
	public static String generateSignature(String data,String token) throws UnsupportedEncodingException 
    {
        byte[] byteHMAC = null;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA1);
            SecretKeySpec spec;
            if(token == null)
            {
            	String oauthSignature = encode(consumer_secret) + "&";
            	spec = new SecretKeySpec(oauthSignature.getBytes(), HMAC_SHA1);
            }
            else
            {
	            String oauthSignature = encode(consumer_secret) + "&" + encode(token);
	            spec = new SecretKeySpec(oauthSignature.getBytes(), HMAC_SHA1);
            }
            mac.init(spec);
            byteHMAC = mac.doFinal(data.getBytes());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException ignore) {
            // should never happen
        }
        return URLEncoder.encode(BASE64Encoder.encode(byteHMAC),"UTF-8");
    }
    
    
    public static String generateSignature(String data) throws UnsupportedEncodingException
    {
    	return generateSignature(data,null);
    }
    
    
    /**
     * @author Yusuke Yamamoto - yusuke at mac.com
     * @see <a href="http://oauth.net/core/1.0/">OAuth Core 1.0</a>
     * @param value string to be encoded
     * @return encoded string
     * @see <a href="http://wiki.oauth.net/TestCases">OAuth / TestCases</a>
     * @see <a href="http://groups.google.com/group/oauth/browse_thread/thread/a8398d0521f4ae3d/9d79b698ab217df2?hl=en&lnk=gst&q=space+encoding#9d79b698ab217df2">Space encoding - OAuth | Google Groups</a>
     * @see <a href="http://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 - Uniform Resource Identifier (URI): Generic Syntax - 2.1. Percent-Encoding</a>
     */
    public static String encode(String value) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
        }
        StringBuffer buf = new StringBuffer(encoded.length());
        char focus;
        for (int i = 0; i < encoded.length(); i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                buf.append("%2A");
            } else if (focus == '+') {
                buf.append("%20");
            } else if (focus == '%' && (i + 1) < encoded.length()
                    && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(focus);
            }
        }
        return buf.toString();
    }
    
}
