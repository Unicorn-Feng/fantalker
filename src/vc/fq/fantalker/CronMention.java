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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

/**
 * 执行计划任务查看mention
 * @author 烽麒 Unicorn-Feng
 * @link http://fq.vc
 */
@SuppressWarnings("serial")
public class CronMention extends HttpServlet 
{
	/**
	 * 用于处理GET请求完成计划任务
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException 
	{
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("setting");
		q.addFilter("mention", Query.FilterOperator.EQUAL, true);
		PreparedQuery pq = datastore.prepare(q);
		for (Entity result : pq.asIterable()) 
		{
			Key key = result.getKey();
			JID fromJID = new JID(key.getName());
			XMPPService xmpp = XMPPServiceFactory.getXMPPService();
			if(xmpp.getPresence(fromJID).isAvailable())
			{
				doCheckMention(fromJID);
			}
		}
	}

	
	/**
	 * 检查是否有新的@提到我的 消息
	 * @param fromJID
	 * @throws IOException
	 */
	public void doCheckMention(JID fromJID) throws IOException
	{
		String last_mention_id = Common.getData(fromJID, "mention","last_id");
		API api = Common.getAPI(fromJID);
		HTTPResponse response = api.statuses_mentions(fromJID, null, last_mention_id);
		Common.StatusShowResp(fromJID, response,3);
	}
	
}
