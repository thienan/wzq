package com.andy.gomoku.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.andy.gomoku.game.GameUser;
import com.andy.gomoku.game.Global;
import com.andy.gomoku.utils.GmAction;
import com.andy.gomoku.utils.SendUtil;
import com.andy.gomoku.websocket.MyWebSocket;

/**
 * 快速开始
 * @author cuiwm
 *
 */
@Component(GmAction.ACTION_PREFIX+GmAction.ACTION_104)
public class Action104 implements IWebAction{

	@Override
	public void doAction(MyWebSocket myWebSocket, Map<String, Object> data) {
		GameUser gameUser = myWebSocket.getUser();
		if(gameUser.getRoom() != null) return;
		
		Global.addMatch(gameUser);
		
		SendUtil.send104(myWebSocket,gameUser);
		
	}

	
	
	
}