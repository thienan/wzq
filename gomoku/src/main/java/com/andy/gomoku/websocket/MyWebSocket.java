package com.andy.gomoku.websocket;

import java.io.IOException;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.andy.gomoku.action.IWebAction;
import com.andy.gomoku.game.GameUser;
import com.andy.gomoku.game.Global;
import com.andy.gomoku.utils.CommonUtils;
import com.andy.gomoku.utils.GmAction;
import com.andy.gomoku.utils.GoConstant;
import com.andy.gomoku.utils.JsonUtils;
import com.andy.gomoku.utils.SpringContextHolder;
import com.google.common.collect.Maps;

/**
 * @Description: websocket入口处理
 * @author cuiwm
 */
@Component
@ServerEndpoint(value = "/websocket")
public class MyWebSocket {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * 会话
	 */
	private Session session;
	
	private Map<String,Object> attrs = Maps.newConcurrentMap();
	
	public void setAttr(String name,Object value){
		attrs.put(name, value);
	}
	
	public Object getAttr(String name){
		return attrs.get(name);
	}
	
	public Object removeAttr(String name){
		return attrs.remove(name);
	}

	public GameUser getUser(){
		return (GameUser) attrs.get(GoConstant.USER_SESSION_KEY);
	}
	
	/**
	 * 建立连接
	 * 
	 * @param session
	 */
	@OnOpen
	public void onOpen(Session session) {
		
		this.session = session;
		Global.addSession(this);
	}

	/**
	 * 连接关闭
	 */
	@OnClose
	public void onClose() {
		// 先退出房间
		CommonUtils.outRoom(this.getUser());
		Global.removeSession(this);
	}

	/**
	 * 收到客户端的消息
	 * @param message
	 *            消息
	 * @param session
	 *            会话
	 */
	@OnMessage
	public void onMessage(String message, Session session) {
		if(logger.isInfoEnabled()){
			logger.info("来自客户端消息:{}", message);
		}
		Map<String, Object> reqData = JsonUtils.json2Map(message);
		Integer action = MapUtils.getInteger(reqData, "action");
		if(action == null)return;
		IWebAction webAction = SpringContextHolder.getBean(GmAction.ACTION_PREFIX+action, IWebAction.class);
		Map<String, Object> data = (Map<String, Object>) reqData.get("data");
		if(data == null){
			data = Maps.newHashMap();
		}
		webAction.doAction(this,data);
		
	}

	/**
	 * 发送消息
	 * @param message
	 *            消息
	 */
	public void sendMessage(String message) {
		if(!session.isOpen())return;
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException e) {
			logger.error("发送消息异常",e);
		}
	}
	
	
}
