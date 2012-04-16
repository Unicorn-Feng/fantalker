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
 * 饭否GTalk机器人API操作类
 *Fanfou Chat Robot for Google Talk
 *Author: 烽麒 Unicorn-Feng
 *Website: http://fq.vc 
 */

package vc.fq.fantalker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
public class API {

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
	@SuppressWarnings("deprecation")
	public HTTPResponse account_verify_credentials(JID fromJID) throws IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/account/verify_credentials.json");

		String params = generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
		String sig = generateSignature(params,oauth_token_secret);
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.GET);
		request.addHeader(new HTTPHeader("Authorization",authorization));
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
	@SuppressWarnings("deprecation")
	public HTTPResponse friendships_create_destroy(JID fromJID, String id, boolean fo) throws IOException
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
		params = "POST&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
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
		/*
		String authorization = "OAuth realm=\"Fantalker\",oauth_consumer_key=\"" + consumer_key
					+ "\",oauth_signature_method=\"HMAC-SHA1\""
					+ ",oauth_timestamp=\"" + String.valueOf(timestamp) + "\""
					+ ",oauth_nonce=\"" + String.valueOf(nonce) + "\""
					+ ",oauth_signature=\"" + signature + "\""
					+ ",oauth_token=\"" + oauth_token + "\"";
		return authorization;
		*/
		StringBuffer strBuf = new StringBuffer(280); 
		strBuf.append("OAuth realm=\"Fantalker\",oauth_consumer_key=\"");
		strBuf.append(consumer_key);
		strBuf.append("\",oauth_signature_method=\"HMAC-SHA1\"");
		strBuf.append(",oauth_timestamp=\"");
		strBuf.append(timestamp);
		strBuf.append("\"");
		strBuf.append(",oauth_nonce=\"");
		strBuf.append(nonce);
		strBuf.append("\"");
		strBuf.append(",oauth_signature=\"");
		strBuf.append(signature);
		strBuf.append("\"");
		strBuf.append(",oauth_token=\"");
		strBuf.append(oauth_token);
		strBuf.append("\"");
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
		String params = "oauth_consumer_key=" + consumer_key 
						+ "&oauth_nonce=" + String.valueOf(nonce)
						+ "&oauth_signature_method=HMAC-SHA1"
						+ "&oauth_timestamp=" + String.valueOf(timestamp)
						+ "&oauth_token=" + oauth_token;
		return params;
	}


	/**
	 * 调用GET /statuses/context_timeline 按照时间先后顺序显示消息上下文
	 * @param fromJID 来源JID
	 * @param id 指定消息ID
	 * @return HTTPResponse 包含json
	 * @throws IOException
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.context-timeline
	 */
	@SuppressWarnings("deprecation")
	public HTTPResponse statuses_context_timeline(JID fromJID, String id) throws IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/context_timeline.json");

		String params = null;
		params = "id=" + id	+ "&" + generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
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
	@SuppressWarnings("deprecation")
	public HTTPResponse statuses_destroy(JID fromJID, String id) throws IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/destroy.json");
		
		String params = null;
		params = "id=" + id + "&" + generateParams(timestamp,nonce);
		params = "POST&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
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
	@SuppressWarnings("deprecation")
	public HTTPResponse statuses_home_timeline(JID fromJID, String pageID) throws IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/home_timeline.json");
	
		String params = generateParams(timestamp,nonce);
		if(pageID != null)
		{
			params = params + "&page=" + pageID;
		}
		
		params = "GET&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
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
	public HTTPResponse statuses_home_timeline(JID fromJID) throws IOException
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
	@SuppressWarnings("deprecation")
	public HTTPResponse statuses_mentions(JID fromJID, String pageID, String since_id) throws IOException
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
		params = "GET&" + URLEncoder.encode(strurl)
				+ "&" + URLEncoder.encode(params);
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
	public HTTPResponse statuses_mentions(JID fromJID) throws IOException
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
	public HTTPResponse statuses_mentions(JID fromJID,String pageID) throws IOException
	{
		return 	statuses_mentions(fromJID,pageID,null);
	}

	
	/**
	 * 调用GET /statuses/public_timeline 显示20条随便看看的消息
	 * @param fromJID
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public HTTPResponse statuses_public_timeline(JID fromJID) throws IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/public_timeline.json");
	
		String params = generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
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
	public HTTPResponse statuses_reply(JID fromJID, String strMessage, String replyID) throws IOException
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
	public HTTPResponse statuses_repost(JID fromJID, String strMessage, String repostID) throws IOException
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
	public HTTPResponse statuses_send(JID fromJID, String strMessage) throws IOException
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
	@SuppressWarnings("deprecation")
	public HTTPResponse statuses_show(JID fromJID, String id) throws IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/statuses/show.json");

		String params = null;
		params = "id=" + id	+ "&" + generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
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
	@SuppressWarnings("deprecation")
	public HTTPResponse statuses_update(JID fromJID, String strMessage, String replyID, String userID, String repostID) throws IOException
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
		params = params + "&status=" + URLEncoder.encode(strMessage,"GB2312").replaceAll("\\+","%20");
		
		params = "POST&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
		params = params.replace("%257E", "~");
		String sig = generateSignature(params,oauth_token_secret);
		
		String authorization = generateAuthString(timestamp, nonce, sig);
		HTTPRequest request = new HTTPRequest(url,HTTPMethod.POST);
		request.addHeader(new HTTPHeader("Authorization",authorization));
		request.addHeader(new HTTPHeader("Content-Type","application/x-www-form-urlencoded"));
		
		String strPayload;
		strPayload = "status=" + URLEncoder.encode(strMessage,"GB2312");
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
	 * 调用 GET /users/show 返回用户的信息
	 * @param fromJID 来源JID
	 * @param id 指定的用户id
	 * @return HTTPResponse，包含json
	 * @throws IOException 
	 * @see https://github.com/FanfouAPI/FanFouAPIDoc/wiki/users.show
	 */
	@SuppressWarnings("deprecation")
	public HTTPResponse users_show(JID fromJID, String id) throws IOException
	{
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://api.fanfou.com/users/show.json");
	
		String params = null;
		params = "id=" + id + "&" + generateParams(timestamp,nonce);
		params = "GET&" + URLEncoder.encode(url.toString())
					+ "&" + URLEncoder.encode(params);
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
	 * @see <a href="http://oauth.net/core/1.0/">OAuth Core 1.0</a>
	 * @edit Unicorn-Feng
     * @param data the data to be signed
     * @param access token secret
     * @return signature
     * @see <a href="http://oauth.net/core/1.0/#rfc.section.9.2.1">OAuth Core - 9.2.1.  Generating Signature</a>
     */
    @SuppressWarnings("deprecation")
	public static String generateSignature(String data,String token) 
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
        return URLEncoder.encode(BASE64Encoder.encode(byteHMAC));
    }
    
    
    public static String generateSignature(String data)
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
