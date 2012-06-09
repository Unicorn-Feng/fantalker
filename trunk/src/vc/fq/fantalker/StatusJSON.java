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
 * 饭否GTalk机器人 Fantalker
 *Fanfou Chat Robot for Google Talk
 *Author: 烽麒 Unicorn-Feng
 *Website: http://fq.vc 
 */

package vc.fq.fantalker;

import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;


/**
 * 用于处理status.show的返回json
 * @author 烽麒 Unicorn-Feng
 * @see 具体格式见https://github.com/FanfouAPI/FanFouAPIDoc/wiki/statuses.show
 */
public class StatusJSON 
{
	private String created_at;
	private String id;
	private String text;
	private String source;
	private UserJSON userJson;
	
	/**
	 * 构造函数
	 * @param strJSON JSON格式字符串
	 */
	public StatusJSON(String strJSON)
	{
		try {
			JSONObject json = new JSONObject(strJSON);
			created_at = json.getString("created_at");
			id = json.getString("id");
			text = json.getString("text");
			source = json.getString("source");
			userJson = new UserJSON(json.getString("user"));
			
		} catch (JSONException e) {
			//e.printStackTrace();
			Common.log.info("status.show.JSON " + e.getMessage());
		}
	}
	
	
	/**
	 * 构造函数
	 * @param json status.show JSON对象
	 */
	public StatusJSON(JSONObject json)
	{
		try {
			created_at = json.getString("created_at");
			id = json.getString("id");
			text = json.getString("text");
			source = json.getString("source");
			userJson = new UserJSON(json.getString("user"));
			
		} catch (JSONException e) {
			//e.printStackTrace();
			Common.log.info("status.show.JSON " + e.getMessage());
		}
	}
	
	
	/**
	 * @return 消息发送时间
	 * Wed Nov 09 07:15:21 +0000 2011
	 */
	public String getCreatedAt()
	{
		return created_at;
	}
	
	
	/**
	 * @return 消息id
	 * UcIlC04F2pQ
	 */
	public String getID()
	{
		return id;
	}
	
	
	/**
	 * 
	 * @return 消息内容
	 */
	public String getText()
	{
		return text;
	}
	
	
	/**
	 * @return 消息来源
	 */
	public String getSource()
	{
		return source;
	}
	
	
	/**
	 * @return users.show JSON
	 */
	public UserJSON getUserJSON()
	{
		return userJson;
	}
}
