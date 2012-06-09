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

import java.util.logging.Logger;

import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

/**
 * 用于处理users.show的返回json
 * @author 烽麒 Unicorn-Feng
 * @see 具体格式见https://github.com/FanfouAPI/FanFouAPIDoc/wiki/users.show
 */
public class UserJSON 
{
	public static final Logger log = Logger.getLogger("Fantalker");
	private String id;
	private String screen_name;
	private String gender;
	private String description;
	private String profile_image_url;
	private String location;
	private int followers_count;
	private int friends_count;
	private int favourites_count;
	private int statuses_count;
	private boolean following;
	
	
	/**
	 * 构造函数
	 * @param strJSON JSON格式字符串
	 */
	public UserJSON(String strJSON)
	{
		try {
			JSONObject json = new JSONObject(strJSON);
			id = json.getString("id");
			screen_name = json.getString("screen_name");
			gender = json.getString("gender");
			description = json.getString("description");
			profile_image_url = json.getString("profile_image_url");
			location = json.getString("location");
			followers_count = json.getInt("followers_count");
			friends_count = json.getInt("friends_count");
			favourites_count = json.getInt("favourites_count");
			statuses_count = json.getInt("statuses_count");
			following = json.getBoolean("following");
		} catch (JSONException e) {
			//e.printStackTrace();
			log.info("users.show.JSON " + e.getMessage());
		}
	}
	
	
	/**
	 * @return 用户id
	 */
	public String getId()
	{
		return id;
	}
	
	
	/**
	 * @return 用户昵称
	 */
	public String getScreenName()
	{
		return screen_name;
	}
	
	
	/**
	 * @return 用户性别
	 * 男
	 */
	public String getGender()
	{
		return gender;
	}
	
	
	/**
	 * @return 用户自述
	 * 测试帐号
	 */
	public String getDescription()
	{
		return description;
	}
	
	
	/**
	 * @return 用户头像地址
	 * http://avatar3.fanfou.com/s0/00/5n/sk.jpg?1320913295
	 */
	public String getProfileImageUrl()
	{
		return profile_image_url;
	}
	
	
	/**
	 * @return 用户地址
	 * 北京 海淀区
	 */
	public String getLocation()
	{
		return location;
	}
	
	
	/**
	 * @return 用户关注用户数
	 */
	public int getFollowersCount()
	{
		return followers_count;
	}
	
	
	/**
	 * @return 用户好友数
	 */
	public int getFriendsCount()
	{
		return friends_count;
	}
	
	
	/**
	 * @return 用户收藏消息数
	 */
	public int getFavouritesCount()
	{
		return favourites_count;
	}
	
	
	/**
	 * @return 用户消息数
	 */
	public int getStatusesCount()
	{
		return statuses_count;
	}
	
	
	/**
	 * @return 该用户是被当前登录用户关注
	 */
	public boolean getIsFollowing()
	{
		return following;
	}
	
}
