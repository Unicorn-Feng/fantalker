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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import javax.cache.CacheManager;
import javax.servlet.http.*;
import com.google.appengine.api.memcache.stdimpl.GCache;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
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
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;


/**
 * 此类用于处理xmpp消息
 * 接收来自 /_ah/xmpp/message/chat/ 的消息
 * @author 烽麒 Unicorn-Feng
 * @link http://fq.vc
 */
@SuppressWarnings("serial")
public class FantalkerServlet extends HttpServlet 
{

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
				doOauth(fromJID,msgarr);
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
			case 10:															//-del
				doDel(fromJID,msgarr);
				break;
			case 11:															//-fo
				doFollow(fromJID,msgarr,true);
				break;
			case 12:															//-unfo
				doFollow(fromJID,msgarr,false);
				break;
			case 13:															//-on
				doOn(fromJID);
				break;
			case 14:															//-off
				doOff(fromJID);
				break;
			case 15:															//-time
				doTime(fromJID,msgarr);
				break;
			case 16:															//-public
				doPublic(fromJID);
				break;
			case 17:															//-timeline
				doTimeLine(fromJID,msgarr);
				break;
			case 18:															//-fav
				doFavorite(fromJID,msgarr,true);
				break;
			//case 19:															//-unfav
				//doFavorite(fromJID,msgarr,false);
				//break;
			case 20:															//-search
				doSearch(fromJID,msgarr);
				break;
			case -1:															//未知命令
			default:
				Common.sendMessage(fromJID,"无效命令");
				break;
			}
		}
	}
	
	
	/**
	 * 执行-bind命令绑定账号
	 * @param fromJID 来源JID
	 * @param msgarr 输入字符串数组
	 * @throws IOException 
	 */
	public void doBind(JID fromJID, String[] msgarr) throws IOException
	{
		if(isOauth(fromJID))													//判断是否已绑定
		{
			return;
		}
		
		if(msgarr.length != 2)
		{
			doHelp(fromJID,"bind");
			return;
		}
		if(msgarr[1].isEmpty())
		{
			doHelp(fromJID,"bind");
			return;
		}
		String strPIN = msgarr[1];
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		URL url = new URL("http://fanfou.com/oauth/access_token");

		String oauth_token = Common.getData(fromJID,"request_token");
		try {
			if(oauth_token == null | oauth_token.isEmpty())
			{
				Common.sendMessage(fromJID,"未找到有效的Request Token\n请先使用-oauth命令获取PIN码。");
				return;
			}
		} catch (NullPointerException e) {
			Common.sendMessage(fromJID,"未找到有效的Request Token\n请先使用-oauth命令获取PIN码。");
			return;
		}
		
		String params = null;
		params = "oauth_consumer_key=" + API.consumer_key
					+ "&oauth_nonce=" + String.valueOf(nonce)
					+ "&oauth_signature_method=HMAC-SHA1"
					+ "&oauth_timestamp=" + String.valueOf(timestamp)
					+ "&oauth_token=" + oauth_token
					+ "&oauth_verifier=" + strPIN;
		
		params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
					+ "&" + URLEncoder.encode(params,"UTF-8");
		String sig = API.generateSignature(params);
		
		String authorization = null;
		
		authorization = "OAuth realm=\"Fantalker\",oauth_consumer_key=\"" + API.consumer_key
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
		
		if(response.getResponseCode() == 200)
		{
			//继续执行
		}
		else if(response.getResponseCode() == 401)
		{
			Common.sendMessage(fromJID, "PIN码无效,请使用-oauth命令重新获取PIN码");
			Common.log.info(Common.getStrJID(fromJID) + " bind:" + new String(response.getContent()));
			return;
		}
		else
		{
			Common.sendMessage(fromJID,"未知错误,请重试");
			Common.log.info(Common.getStrJID(fromJID) + " bind:" + new String(response.getContent()));
			return;
		}
		
		/* 提取接收到的未经授权的Request Token */
		String tokenstring = new String(response.getContent());
		String[] tokenarr = tokenstring.split("&");
		String[] tokenarr2 = tokenarr[0].split("=");
		oauth_token = tokenarr2[1];
		tokenarr2 = tokenarr[1].split("=");
		String oauth_token_secret = tokenarr2[1];

		Common.setToken(fromJID, oauth_token, oauth_token_secret);
	}
	
	
	/**
	 * 执行-del删除指定消息
	 * @param fromJID
	 * @param strarr
	 * @throws IOException 
	 */
	public void doDel(JID fromJID, String[] msgarr) throws IOException
	{
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		if(msgarr.length != 2)
		{
			doHelp(fromJID,"del");
			return;
		}
		if(msgarr[1].isEmpty())
		{
			doHelp(fromJID,"del");
			return;
		}
		String id = msgarr[1];
		if(id.length()<=2)
		{
			id = Common.shortID2ID(fromJID, id);
			if(id == null)
			{
				Common.sendMessage(fromJID,"找不到该短ID对应的长ID,请重新输入或使用长ID");
				return;
			}
		}
		HTTPResponse response;
		response = api.statuses_destroy(fromJID, id);
		String strmessage;
		if(response.getResponseCode() == 200)
		{
			StatusJSON jsonstatus = new StatusJSON(new String(response.getContent()));
			strmessage = "成功删除消息: " + jsonstatus.getText();
		}
		else if(response.getResponseCode() == 404)
		{
			strmessage = "没有这条消息";
		}
		else if(response.getResponseCode() == 403)
		{
			strmessage = "这不是你的消息，不能删除";
		}
		else
		{
			strmessage = Common.getError(new String(response.getContent()));
			Common.log.info(Common.getStrJID(fromJID) + "del: " + new String(response.getContent()));
		}
		Common.sendMessage(fromJID,strmessage);
	}

	
	/**
	 * 执行-fav/-unfav 收藏消息或查看已收藏消息
	 * @param fromJID
	 * @param msgarr
	 * @throws IOException 
	 */
	public void doFavorite(JID fromJID, String[] msgarr, boolean fav)
	{
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		HTTPResponse response;
		if(msgarr.length == 1)
		{
			if(fav == true)														//-fav
			{
				try {
					response = api.favorites(fromJID);
					Common.StatusShowResp(fromJID, response,7);
				} catch (SocketTimeoutException e) {
					Common.sendMessage(fromJID, "连接饭否API超时，请重试");
					return;
				} catch (IOException e) {
					Common.sendMessage(fromJID, "连接饭否API时出错，请重试");
					return;
				}
			}
			else																//-unfav
			{
				doHelp(fromJID,"unfav");
				return;
			}
		}
		else if(msgarr.length == 2)
		{
			if(fav == true)
			{
				if(msgarr[1].charAt(0) == 'p' || msgarr[1].charAt(0) == 'P')
				{
					if(Common.isNumeric(msgarr[1].substring(1)))				//-fav p2
					{
						try {
							msgarr[1] = msgarr[1].substring(1);
							response = api.favorites(fromJID, msgarr[1]);
							Common.StatusShowResp(fromJID, response, 7, msgarr[1]);
							return;
						} catch (SocketTimeoutException e) {
							Common.sendMessage(fromJID, "连接饭否API超时，请重试");
							return;
						} catch (IOException e) {
							Common.sendMessage(fromJID, "连接饭否API时出错，请重试");
							return;
						}
					}
				}
			}
			
			doHelp(fromJID,"fav");
			return;
			
			/* -fav 消息ID 或 -unfav 消息ID */
			/*
			String id = msgarr[1];
			if(id.length()<=2)
			{
				id = Common.shortID2ID(fromJID, id);
				if(id == null)
				{
					Common.sendMessage(fromJID,"找不到该短ID对应的长ID,请重新输入或使用长ID");
					return;
				}
			}
			try {
				response = api.favorites_create_destroy(fromJID, id, fav);
				String strMessage;
				if(response.getResponseCode() == 200)
				{
					StatusJSON jsonstatus = new StatusJSON(new String(response.getContent()));
					if(fav == true)
					{
						strMessage = "成功收藏 : " + jsonstatus.getText();
					}
					else
					{
						strMessage = "成功取消收藏: " + jsonstatus.getText();
					}
				}
				else if(response.getResponseCode() == 403)
				{
					strMessage = "你没有通过这个用户的验证,无法收藏TA的消息";
				}
				else if(response.getResponseCode() == 404)
				{
					strMessage = "找不到该消息";
				}
				else
				{
					strMessage = Common.getError(new String(response.getContent()));
					System.out.println(new String(response.getContent()));
					System.out.println(response.getResponseCode());
				}
				Common.sendMessage(fromJID, strMessage);
			} catch (SocketTimeoutException e) {
				Common.sendMessage(fromJID, "连接饭否API时超时,请重试");
				return;
			} catch (IOException e) {
				Common.sendMessage(fromJID, "连接饭否API时出错，请重试");
				return;
			}
			*/
		}
		else
		{
			if(fav == true)
			{
				doHelp(fromJID,"fav");
			}
			else
			{
				doHelp(fromJID,"unfav");
			}
		}
	}

	
	/**
	 * 执行-fo/-unfo关注好友
	 * @param fromJID
	 * @param msgarr
	 * @throws IOException 
	 */
	public void doFollow(JID fromJID, String[] msgarr, boolean fo) throws IOException
	{
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		if(msgarr.length != 2)
		{
			doHelp(fromJID,"fo");
			return;
		}
		if(msgarr[1].isEmpty())
		{
			doHelp(fromJID,"fo");
			return;
		}
		HTTPResponse response;
		response = api.friendships_create_destroy(fromJID, msgarr[1],fo);
		String strmessage;
		if(response.getResponseCode() == 200)
		{
			if(fo)
			{
				strmessage = "成功关注 " + msgarr[1];
			}
			else
			{
				strmessage = "成功取消关注 " + msgarr[1];
			}
		}
		else if(response.getResponseCode() == 404)
		{
			strmessage = "找不到该用户";
		}
		else
		{
			strmessage = Common.getError(new String(response.getContent()));
		}
		Common.sendMessage(fromJID,strmessage);
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
					+ "-pub/-public： 显示20条随便看看的消息\n"
					+ "-@/-r-/-reply：查看提到我的消息及 回复消息\n"
					+ "-tl/-timeline： 查看指定用户已发送的消息\n"
					+ "-rt：转发消息\n"
					+ "-m/-msg： 查看指定消息的上下文\n"
					+ "-del/-delete： 删除指定消息\n"
					+ "-fav/-favorite： 查看已收藏消息\n"
					//+ "-unfav/-unfavorite： 取消收藏指定消息\n"
					+ "-s/-search： 搜索指定消息\n"
					+ "-u： 查看用户信息\n"
					+ "-fo/-follow： 加指定用户为好友\n"
					+ "-unfo/-unfollow： 删除指定好友\n"
					+ "-on： 开启定时提醒\n"
					+ "-off： 关闭定时提醒\n"
					//+ "-time： 设置定时提醒间隔\n"
					+ "-oauth： 开始OAuth认证或进行XAuth认证\n"
					+ "-bind： 绑定PIN码完成认证\n"
					+ "-remove： 解除关联\n"
					+ "-?/-h/-help： 显示本帮助\n"
					+ "\n消息格式:\n"
					+ "*作者*: 消息内容\n [ 消息长ID = 短ID ] 年-月-日 时:分:秒 <- 消息来源\n"
					+ "\n如果在使用中发现有BUG，请及时联系我@烽麒 谢谢～";
		}
		else if(msgarr.length == 2)
		{
			msgarr[1] = "-" + msgarr[1];
			
			switch (getCmdID(msgarr[1]))
			{
			case 1:																//-oauth
				helpMsg = "用法1: -oauth\n获取用于OAuth的链接\n"
						+ "用法2: -oauth 用户名 密码\n"
						+ "使用XAuth方式认证，免去登陆饭否网站认证的过程。\n"
						+ "您的用户名密码用于提交至饭否完成认证，本程序不保存您的密码信息\n";
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
						+ "回复消息ID所指定的消息，消息ID可为长ID也可为短ID\n"
						+ "用法2: -@/-r/-reply [p页码]\n"
						+ "显示@你的消息，页码可选\n命令如-@ p2\n";
				break;
			case 6:																//-home
				helpMsg = "用法: -home [p页码]\n显示时间线，页码可选\n";
				break;
			case 7:																//-msg
				helpMsg = "用法: -msg 消息ID\n显示消息ID所指定的消息及其上下文，消息ID可为长ID也可为短ID\n";
				break;
			case 8:																//-rt
				helpMsg = "用法: -rt 消息ID [内容]\n转发指定消息ID的消息，内容可选，消息ID可为长ID也可为短ID\n";
				break;
			case 9:																//-u
				helpMsg = "用法: -u [用户id]\n显示指定用户的详细信息，不加参数则显示已绑定用户的信息\n";
				break;
			case 10:															//-del
				helpMsg = "用法: -del 消息ID\n删除指定消息，消息ID可为长ID也可为短ID\n";
				break;
			case 11:															//-fo
				helpMsg = "用法: -fo 用户ID或loginname\n添加指定用户为好友\n";
				break;
			case 12:															//-unfo
				helpMsg = "用法: -unfo 用户ID或loginname\n删除指定好友\n";
				break;
			case 13:															//-on
				helpMsg = "用法: -on\n开启定时提醒新的@提到我的消息\n";
				break;
			case 14:															//-off
				helpMsg = "用法: -off\n关闭定时提醒新的@提到我的消息\n";
				break;
			case 15:															//-time
				helpMsg = "用法: -time 时间\n设置定时提醒时间间隔，现阶段仅可为5的倍数\n";
				break;
			case 16:															//-public
				helpMsg = "用法: -public\n显示20条随便看看的消息\n";
				break;
			case 17:															//-tl
				helpMsg = "用法1: -timeline/-tl [p页码]\n"
						+ "显示当前用户已发送的消息，页码可选\n命令如-tl p2\n"
						+ "用法2: -timeline/-tl 用户ID [p页码]\n"
						+ "显示用户ID所指定的用户已发送的消息，页码可选\n";
				break;
			case 18:															//-fav
				helpMsg = "用法: -favorite/-fav [p页码]\n"
						+ "查看已收藏的消息，页码可选\n命令如-fav p2\n";
						//+ "用法2: -favorite/-fav 消息ID\n"
						//+ "收藏指定消息，消息ID可为长ID也可为短ID\n";
				break;
			//case 19:															//-unfav
				//helpMsg = "用法: -unfavorite/-unfav 消息ID\n取消收藏指定消息，消息ID可为长ID也可为短ID\n";
				//break;
			case 20:															//-search
				helpMsg = "用法: -s/-search 关键词\n"
						+ "搜索含指定关键词的消息，多个关键词用|分隔\n";
				break;
			case -1:															//未知命令
			default:
				helpMsg = "无此命令\n";
				break;
			}
		}
		else
		{
			doHelp(fromJID,"help");
			return;
		}
		Common.sendMessage(fromJID,helpMsg);
	}

	
	/**
	 * 执行-help commond 显示指定命令的帮助信息
	 * @param fromJID
	 * @param commond 要显示帮助的命令
	 */
	public void doHelp(JID fromJID, String commond)
	{
		String[] msgarr = new String[2];
		msgarr[0] = "-help";
		msgarr[1] = commond;
		doHelp(fromJID,msgarr);
	}

	
	/**
	 * 执行-home命令
	 * @param fromJID
	 * @param msgarr
	 * @throws IOException 
	 */
	public void doHome(JID fromJID, String[] msgarr) throws IOException
	{
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		HTTPResponse response;
		int msgarr_len = msgarr.length;
		if(msgarr_len == 1)
		{
			response = api.statuses_home_timeline(fromJID);
			Common.StatusShowResp(fromJID, response,1);
		}
		else if(msgarr_len == 2)												//-ho p2
		{
			char ch;
			ch = msgarr[1].charAt(0);
			if(ch != 'p' && ch != 'P')
			{
				doHelp(fromJID,"home");
				return;
			}
			msgarr[1] = msgarr[1].substring(1);
			if(!Common.isNumeric(msgarr[1]))
			{
				doHelp(fromJID,"home");
				return;
			}
			response = api.statuses_home_timeline(fromJID,msgarr[1]);
			Common.StatusShowResp(fromJID, response,1,msgarr[1]);
		}
		else
		{
			doHelp(fromJID,"home");
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
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		if(msgarr.length == 2)													//-m 7rXy196_C3k
		{
			String id = msgarr[1];
			if(id.length()<=2)
			{
				id = Common.shortID2ID(fromJID, id);
				if(id == null)
				{
					Common.sendMessage(fromJID,"找不到该短ID对应的长ID,请重新输入或使用长ID");
					return;
				}
			}
			HTTPResponse response;
			response = api.statuses_context_timeline(fromJID, id);
			if(response.getResponseCode() == 200)
			{
				Common.StatusShowResp(fromJID, response,4);
			}
			else if(response.getResponseCode() == 403)
			{
				Common.sendMessage(fromJID,"你没有通过这个用户的验证,无法查看TA的消息");
			}
			else if(response.getResponseCode() == 404)
			{
				Common.sendMessage(fromJID,"id所指定的消息不存在");
			}
			else
			{
				Common.sendMessage(fromJID,Common.getError(new String(response.getContent())));
				Common.log.info(Common.getStrJID(fromJID) + "-msg: " + new String(response.getContent()));
			}
		}
		else
		{
			doHelp(fromJID,"m");
		}
	}


	/**
	 * 执行-oauth命令完成认证
	 * @param fromJID 来源JID
	 * @param msgarr
	 * @throws IOException
	 */
	public void doOauth(JID fromJID, String[] msgarr) throws IOException
	{
		if(isOauth(fromJID))													//判断是否已绑定
		{
			return;
		}
		
		String strJID = Common.getStrJID(fromJID);
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime();
		String params = null;
		String authorization;
		
		if(msgarr.length == 1)													//OAuth
		{
			URL url = new URL("http://fanfou.com/oauth/request_token");
		
			params = "oauth_consumer_key=" + API.consumer_key 
						+ "&oauth_nonce=" + String.valueOf(nonce)
						+ "&oauth_signature_method=HMAC-SHA1"
						+ "&oauth_timestamp=" + String.valueOf(timestamp);
		
			params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
						+ "&" + URLEncoder.encode(params,"UTF-8");
			String sig = API.generateSignature(params);
					
			StringBuffer strBuf = new StringBuffer(250); 
			strBuf.append("OAuth realm=\"Fantalker\",oauth_consumer_key=\"");
			strBuf.append(API.consumer_key);
			strBuf.append("\",oauth_signature_method=\"HMAC-SHA1\"");
			strBuf.append(",oauth_timestamp=\"").append(timestamp).append("\"");
			strBuf.append(",oauth_nonce=\"").append(nonce).append("\"");
			strBuf.append(",oauth_signature=\"").append(sig).append("\"");
			authorization = strBuf.toString();
			
			HTTPRequest request = new HTTPRequest(url,HTTPMethod.GET);
			request.addHeader(new HTTPHeader("Authorization",authorization));
			URLFetchService service = URLFetchServiceFactory.getURLFetchService();
			HTTPResponse response;
			try {
				response = service.fetch(request);
			} catch (SocketTimeoutException e) {
				Common.sendMessage(fromJID, "连接饭否超时，请重试");
				return;
			}
			if(response.getResponseCode() != 200)
			{
				String errMsg = "出现错误，请重试";
				Common.sendMessage(fromJID,errMsg);
				Common.log.warning(strJID + " :" + String.valueOf(response.getResponseCode()) + ": " + new String(response.getContent()));
				return;
			}
			
			/* 提取接收到的未经授权的Request Token */
			String tokenstring = new String(response.getContent());
			String[] tokenarr = tokenstring.split("&");
			String[] tokenarr2 = tokenarr[0].split("=");
			String oauth_token = tokenarr2[1];
			Common.setData(fromJID,"Account","request_token",oauth_token);
			
			/* 请求用户授权Request Token */
			String strMessage = "请访问以下网址获取PIN码: \n http://fanfou.com/oauth/authorize?oauth_token="
						+ oauth_token + "&oauth_callback=oob"
						+ " \n 手机用户请访问: \n http://m.fanfou.com/oauth/authorize?oauth_token="
						+ oauth_token + "&oauth_callback=oob"
						+ " \n 然后使用\"-bind PIN码\"命令绑定账号。";
			Common.sendMessage(fromJID,strMessage);
		}
		else if(msgarr.length == 3)												//XAuth
		{
			URL url = new URL("http://fanfou.com/oauth/access_token");
			String username = msgarr[1];
			String password = msgarr[2];
			params = "oauth_consumer_key=" + API.consumer_key
					+ "&oauth_nonce=" + String.valueOf(nonce)
					+ "&oauth_signature_method=HMAC-SHA1"
					+ "&oauth_timestamp=" + String.valueOf(timestamp)
					+ "&x_auth_username=" + username
					+ "&x_auth_password=" + password
					+ "&x_auth_mode=client_auth";
		
			params = "GET&" + URLEncoder.encode(url.toString(),"UTF-8")
						+ "&" + URLEncoder.encode(params,"UTF-8");
			String sig = API.generateSignature(params);
			
			authorization = "OAuth realm=\"Fantalker\",oauth_consumer_key=\"" + API.consumer_key
						+ "\",oauth_signature_method=\"HMAC-SHA1\""
						+ ",oauth_timestamp=\"" + String.valueOf(timestamp) + "\""
						+ ",oauth_nonce=\"" + String.valueOf(nonce) + "\""
						+ ",oauth_signature=\"" + sig + "\""
						+ ",x_auth_username=\"" + username + "\""
						+ ",x_auth_password=\"" + password + "\""
						+ ",x_auth_mode=\"client_auth\"";
			
			
			HTTPRequest request = new HTTPRequest(url,HTTPMethod.GET);
			request.addHeader(new HTTPHeader("Authorization",authorization));
			URLFetchService service = URLFetchServiceFactory.getURLFetchService();
			HTTPResponse response;
			try {
				response = service.fetch(request);
			} catch (SocketTimeoutException e) {
				Common.sendMessage(fromJID, "连接饭否超时，请重试");
				return;
			}
			
			if(response.getResponseCode() == 200)
			{
				//继续执行
			}
			else if(response.getResponseCode() == 401)
			{
				Common.sendMessage(fromJID, "用户名或密码错误,请重试");
				return;
			}
			else
			{
				Common.sendMessage(fromJID,"未知错误，请重试");
				Common.log.info(Common.getStrJID(fromJID) + " xauth:" + new String(response.getContent()));
				return;
			}
			try {
				/* 提取接收到的未经授权的Request Token */
				String tokenstring = new String(response.getContent());
				String[] tokenarr = tokenstring.split("&");
				String[] tokenarr2 = tokenarr[0].split("=");
				String oauth_token = tokenarr2[1];
				tokenarr2 = tokenarr[1].split("=");
				String oauth_token_secret = tokenarr2[1];
				Common.setToken(fromJID, oauth_token, oauth_token_secret);
			} catch (ArrayIndexOutOfBoundsException e) {
				Common.sendMessage(fromJID, "未知错误，请重试");
				return;
			}

		}
		else
		{
			doHelp(fromJID,"oauth");
		}
	}


	/**
	 * 执行 -on 命令开启自动提醒
	 * @param fromJID
	 * @throws IOException
	 */
	public void doOn(JID fromJID) throws IOException
	{
		if(Common.getData(fromJID,"access_token") == null)						//判断是否已绑定
		{
			Common.sendMessage(fromJID, "您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		Setting set = Common.getSetting(fromJID);
		set.setMention(true);
		Common.setSetting(fromJID, set);
		Common.sendMessage(fromJID, "成功开启定时提醒新的@提到我的消息 功能");
	}
	
	
	/**
	 * 执行-off 命令关闭自动提醒
	 * @param fromJID
	 * @throws IOException
	 */
	public void doOff(JID fromJID) throws IOException
	{
		if(Common.getData(fromJID,"access_token") == null)						//判断是否已绑定
		{
			Common.sendMessage(fromJID, "您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		Setting set = Common.getSetting(fromJID);
		set.setMention(false);
		Common.setSetting(fromJID, set);
		Common.sendMessage(fromJID, "成功关闭定时提醒新的@提到我的消息 功能");
	}
	
	
	/**
	 * 执行-public 命令随便看看
	 * @param fromJID
	 * @throws IOException
	 */
	public void doPublic(JID fromJID) throws IOException
	{
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		HTTPResponse response;
		response = api.statuses_public_timeline(fromJID);
		Common.StatusShowResp(fromJID, response,5);
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
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		HTTPResponse response;
		int msgarr_len = msgarr.length;
		if(msgarr_len == 1)
		{
			response = api.statuses_mentions(fromJID);
			Common.StatusShowResp(fromJID, response,2);
		}
		else if(msgarr_len == 2)												//-@ p2
		{
			char ch;
			ch = msgarr[1].charAt(0);
			if(ch != 'p' && ch != 'P')
			{
				Common.sendMessage(fromJID,"无效命令");
				return;
			}
			msgarr[1] = msgarr[1].substring(1);
			if(!Common.isNumeric(msgarr[1]))
			{
				Common.sendMessage(fromJID,"无效命令");
				return;
			}
			response = api.statuses_mentions(fromJID, msgarr[1]);
			Common.StatusShowResp(fromJID, response,2,msgarr[1]);
		}
		else																	//-@ -WNEO5ZQt28 test t
		{
			int intIndex = strMessage.lastIndexOf(msgarr[1]) + msgarr[1].length() + 1;
			String replyMsg = strMessage.substring(intIndex);
			String id = msgarr[1];
			if(id.length()<=2)
			{
				id = Common.shortID2ID(fromJID, id);
				if(id == null)
				{
					Common.sendMessage(fromJID,"找不到该短ID对应的长ID,请重新输入或使用长ID");
					return;
				}
			}
			response = api.statuses_reply(fromJID, replyMsg, id);
			if(response == null)
			{
				return;
			}
			if(response.getResponseCode() == 200)
			{
				StatusJSON jsonStatus = new StatusJSON(new String(response.getContent()));
				Common.sendMessage(fromJID,"成功回复\n  " + jsonStatus.getText());
			}
			else if(response.getResponseCode() == 400)
			{
				try {
					JSONObject json = new JSONObject(new String(response.getContent()));
					String error = json.getString("error");
					Common.sendMessage(fromJID,error);
					
				} catch (JSONException e) {
					//e.printStackTrace();
					Common.log.warning(Common.getStrJID(fromJID) + " @:" + new String(response.getContent()));
					Common.sendMessage(fromJID,"未知错误");
				}
			}
			else
			{
				Common.sendMessage(fromJID,"未知错误");
				Common.log.info(Common.getStrJID(fromJID) + " rt:" + new String(response.getContent()));
			}

		}
	}
	
	
	/**
	 * 执行-remove命令解除账号绑定
	 * @param fromJID
	 */
	public void doRemove(JID fromJID)
	{
		if(Common.getData(fromJID,"access_token") == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		String strJID = Common.getStrJID(fromJID);
		Key k = KeyFactory.createKey("Account", strJID);
		datastore.delete(k);
		k = KeyFactory.createKey("mention",strJID);
		datastore.delete(k);
		k = KeyFactory.createKey("setting",strJID);
		datastore.delete(k);
		
		GCache cache;
		try{
			GCacheFactory cacheFactory = (GCacheFactory) CacheManager.getInstance().getCacheFactory();
			cache = (GCache) cacheFactory.createCache(Collections.emptyMap());
			cache.remove(strJID + ",access_token");
			cache.remove(strJID + ",access_token_secret");
			cache.remove(strJID + ",last_id");
		} catch (javax.cache.CacheException e){
			Common.log.info(strJID + ":JCache " + e.getMessage());
		}
		
		Common.sendMessage(fromJID,"您已成功解除账号绑定，请使用-oauth命令再次绑定账号");
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
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		HTTPResponse response;
		int msgarr_len = msgarr.length;
		if(msgarr_len == 1)
		{
			doHelp(fromJID,"rt");
		}
		else																	//-rt -WNEO5ZQt28 test t
		{
			int intIndex;
			String replyMsg;
			try{
				intIndex = strMessage.lastIndexOf(msgarr[1]) + msgarr[1].length() + 1;
				replyMsg = strMessage.substring(intIndex);
			} catch (StringIndexOutOfBoundsException e) {
				replyMsg = "";
			}
	
			String id = msgarr[1];
			if(id.length()<=2)
			{
				id = Common.shortID2ID(fromJID, id);
				if(id == null)
				{
					Common.sendMessage(fromJID,"找不到该短ID对应的长ID,请重新输入或使用长ID");
					return;
				}
			}
			response = api.statuses_repost(fromJID, replyMsg, id);
			if(response.getResponseCode() == 200)
			{
				StatusJSON jsonStatus = new StatusJSON(new String(response.getContent()));
				Common.sendMessage(fromJID,"成功转发\n  " + jsonStatus.getText());
			}
			else
			{
				Common.sendMessage(fromJID,Common.getError(new String(response.getContent())));
				Common.log.info(Common.getStrJID(fromJID) + " rt:" + new String(response.getContent()));
			}
		}
	}


	/**
	 * 发布状态
	 * @param fromJID
	 * @param message 状态内容
	 * @throws IOException 
	 */
	public void doSend(JID fromJID, String message) throws IOException
	{
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		HTTPResponse response;
		response = api.statuses_send(fromJID,message);
		if(response.getResponseCode() == 200)
		{
			Common.sendMessage(fromJID,"成功发布状态");
		}
		else
		{
			Common.sendMessage(fromJID,Common.getError(new String(response.getContent())));
			Common.log.info(Common.getStrJID(fromJID) + "status.send: " + new String(response.getContent()));
		}
	}
	
	
	/**
	 * 执行-search搜索指定消息
	 * @param fromJID
	 * @param msgarr
	 */
	public void doSearch(JID fromJID, String[] msgarr)
	{
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		if(msgarr.length < 2)
		{
			doHelp(fromJID,"search");
			return;
		}
		
		String keyword = "";
		for(int i=1;i<msgarr.length;i++)
		{
			keyword = keyword + msgarr[i] + " ";
		}
		keyword = keyword.substring(0,keyword.length()-1);
		
		HTTPResponse response;
		try {
			response = api.search_public_timeline(fromJID, keyword);
			Common.StatusShowResp(fromJID, response,8,null,keyword);
		} catch (SocketTimeoutException e) {
			Common.sendMessage(fromJID, "连接饭否API超时，请重试");
			return;
		} catch (IOException e) {
			Common.sendMessage(fromJID, "连接饭否API时出错，请重试");
			return;
		}
	}
	
	
	/**
	 * 执行-time 命令设置定时提醒间隔
	 * @param fromJID
	 * @param msgarr
	 * @throws IOException
	 */
	public void doTime(JID fromJID, String[] msgarr) throws IOException
	{
		if(msgarr.length == 1)
		{
			doHelp(fromJID,"time");
			return;
		}
		if(!Common.isNumeric(msgarr[1]))
		{
			doHelp(fromJID,"time");
		}
		long time = Long.parseLong(msgarr[1]);
		time = time / 5;
		time = time * 5;														//将time设置为5的倍数
		if(time == 0)
		{
			time = 5;
		}
		Setting set = Common.getSetting(fromJID);
		set.setTime(time);
		Common.setSetting(fromJID, set);
		Common.sendMessage(fromJID, "成功设置间隔时间" + String.valueOf(time) + "分钟");
	}
	
	
	/**
	 * 执行-tl 显示指定用户已发送的消息
	 * @param fromJID
	 * @param msgarr
	 * @throws IOException
	 */
	public void doTimeLine(JID fromJID, String[] msgarr) throws IOException
	{
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		HTTPResponse response;
		int msgarr_len = msgarr.length;
		if(msgarr_len == 1)
		{
			response = api.statuses_user_timeline(fromJID);
			Common.StatusShowResp(fromJID, response,6);
		}
		else if(msgarr_len == 2)												//-tl p2或-tl 用户ID
		{
			char ch;
			ch = msgarr[1].charAt(0);
			if(ch == 'p' || ch == 'P')
			{
				if(Common.isNumeric(msgarr[1].substring(1)))					//-tl p2
				{
					msgarr[1] = msgarr[1].substring(1);
					response = api.statuses_user_timeline(fromJID, msgarr[1]);
					Common.StatusShowResp(fromJID, response, 6, msgarr[1]);
					return;
				}
			}
			
			response = api.statuses_user_timeline(fromJID, msgarr[1], null);
			Common.StatusShowResp(fromJID, response, 6);
		}
		else if(msgarr_len == 3)												//-tl 用户ID p2
		{
			char ch;
			ch = msgarr[2].charAt(0);
			if(ch != 'p' && ch != 'P')
			{
				doHelp(fromJID,"timeline");
			}
			msgarr[2] = msgarr[2].substring(1);
			if(!Common.isNumeric(msgarr[2]))
			{
				doHelp(fromJID,"timeline");
			}
			response = api.statuses_user_timeline(fromJID, msgarr[1], msgarr[2]);
			Common.StatusShowResp(fromJID, response, 6, msgarr[2]);
		}
		else
		{
			doHelp(fromJID,"timeline");
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
		API api = Common.getAPI(fromJID);
		if(api == null)
		{
			Common.sendMessage(fromJID,"您尚未绑定账号，请使用-oauth命令绑定");
			return;
		}
		HTTPResponse response;
		if(msgarr.length == 1)													//-u
		{
			response = api.account_verify_credentials(fromJID);
		}
		else if(msgarr.length == 2)												//-u id
		{
			response = api.users_show(fromJID, msgarr[1]);
		}
		else
		{
			doHelp(fromJID,"u");
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
			Common.sendMessage(fromJID,message);
		}
		else if(response.getResponseCode() == 403)
		{
			Common.sendMessage(fromJID,"你没有通过这个用户的验证,无法查看TA的信息");
		}
		else if(response.getResponseCode() == 404)
		{
			Common.sendMessage(fromJID,"id所指定的用户不存在");
		}
		else
		{
			Common.sendMessage(fromJID,Common.getError(new String(response.getContent())));
			Common.log.info(Common.getStrJID(fromJID) + "-u: " + new String(response.getContent()));
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
		else if(strCmd.equals("-del") || strCmd.equals("-delete"))				//删除消息
			return 10;
		else if(strCmd.equals("-fo") || strCmd.equals("-follow"))				//关注好友
			return 11;
		else if(strCmd.equals("-unfo") || strCmd.equals("-unfollow"))			//取消关注
			return 12;
		else if(strCmd.equals("-on"))											//开启自动提醒
			return 13;
		else if(strCmd.equals("-off"))											//关闭自动提醒
			return 14;
		else if(strCmd.equals("-time"))											//设置自动更新间隔
			return 15;
		else if(strCmd.equals("-public") || strCmd.equals("-pub"))				//显示随便看看
			return 16;
		else if(strCmd.equals("-timeline") || strCmd.equals("-tl"))				//显示指定用户已发送消息
			return 17;
		else if(strCmd.equals("-favorite") || strCmd.equals("-fav"))			//收藏消息
			return 18;
		else if(strCmd.equals("-unfavorite") || strCmd.equals("-unfav"))		//取消收藏消息
			return 19;
		else if(strCmd.equals("-s") || strCmd.equals("-search"))				//搜索消息
			return 20;
		else																	//未知命令
			return -1;
	}


	/**
	 * 通过是否有access_token判断是否已绑定账号
	 * @param fromJID 来源JID
	 * @return 已绑定返回true,并发送提示
	 */
	public boolean isOauth(JID fromJID)
	{
		if(Common.getData(fromJID,"access_token") == null)
		{
			return false;
		}
		else
		{
			String id;
			id = Common.getID(fromJID);
			if(id == null)
			{
				String strMessage = "您已经绑定饭否ID\n 若您希望绑定新的账号请先使用-remove命令取消现有绑定";
				Common.sendMessage(fromJID, strMessage);
			}
			else
			{
				String strMessage = "您已经绑定饭否ID:" + id +" \n 若您希望绑定新的账号请先使用-remove命令取消现有绑定";
				Common.sendMessage(fromJID, strMessage);
			}
			return true;
		}
	}
	
}
