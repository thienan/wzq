package com.andy.gomoku.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

public class HeartbeatHandler extends ChannelInboundHandlerAdapter{
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HB",CharsetUtil.UTF_8));
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if(evt instanceof IdleStateEvent) {
			NettySocketSession session = ctx.channel().attr(TextWebSocketFrameHandler.nssKey).get();
			if(session != null && session.getUser() != null){
				logger.info("====>Heartbeat: greater than {},outUser:{}", 1800,session.getUser().getId());
			}else{
				logger.info("====>Heartbeat: greater than {}", 1800);
			}
			ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		}else {
			super.userEventTriggered(ctx, evt);
		}
	}
}