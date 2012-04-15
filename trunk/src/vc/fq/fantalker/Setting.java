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

/**
 * 用于存储用户设置
 * @author 烽麒 Unicorn-Feng
 * @link http://fq.vc
 */
public class Setting 
{
	private boolean dm;
	private boolean mention;
	private long time;
	
	/**
	 * 构造函数
	 * @param boldm
	 * @param bolmention
	 * @param inttime
	 */
	public Setting(boolean boldm, boolean bolmention, long lngtime)
	{
		dm = boldm;
		mention = bolmention;
		time = lngtime;
	}
	
	
	/**
	 * @return 是否开启DM提醒
	 */
	public boolean getDm()
	{
		return dm;
	}
	
	
	/**
	 * @return 是否开启@提醒
	 */
	public boolean getMention()
	{
		return mention;
	}
	
	
	/**
	 * @return 提醒间隔
	 */
	public long getTime()
	{
		return time;
	}
	
	
	/**
	 * 设置是否开启私信提醒
	 * @param boldm
	 */
	public void setDm(boolean boldm)
	{
		dm = boldm;
	}
	
	
	/**
	 * 设置是否开启@提醒
	 * @param bolmention
	 */
	public void setMention(boolean bolmention)
	{
		mention = bolmention;
	}
	
	
	/**
	 * 设置定时提醒间隔
	 * @param inttime
	 */
	public void setTime(long lngtime)
	{
		time = lngtime;
	}
	
}
