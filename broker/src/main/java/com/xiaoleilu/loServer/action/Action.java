/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action;

import com.xiaoleilu.loServer.ErrorCode;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.RequireAuthentication;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.server.config.IConfig;
import io.moquette.spi.impl.Utils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;

import static com.xiaoleilu.loServer.ErrorCode.ERROR_CODE_SUCCESS;

/**
 * 请求处理接口<br>
 * 当用户请求某个Path，则调用相应Action的doAction方法
 * @author Looly
 *
 */

abstract public class Action {
    public ChannelHandlerContext ctx;

    public ErrorCode preAction(Request request, Response response) {
        if (getClass().getAnnotation(RequireAuthentication.class) != null) {
            //do authentication
        }

        return ERROR_CODE_SUCCESS;
    }

	public boolean doAction(Request request, Response response) {
        ErrorCode errorCode = preAction(request, response);
        boolean isSync = true;
        if (errorCode == ERROR_CODE_SUCCESS) {
            isSync = action(request, response);
        } else {
            response.setStatus(HttpResponseStatus.OK);
            if (errorCode == null) {
                errorCode = ERROR_CODE_SUCCESS;
            }

            RestResult result = RestResult.resultOf(errorCode, errorCode.getMsg(), RestResult.resultOf(errorCode));
            response.setContent(new Gson().toJson(result));
            response.send();
        }

        return isSync;
    }

    abstract public boolean action(Request request, Response response);

    protected <T> T getRequestBody(HttpRequest request, Class<T> cls) {
        if (request instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            byte[] bytes = Utils.readBytesAndRewind(fullHttpRequest.content());
            String content = new String(bytes, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            T t = gson.fromJson(content, cls);
            return t;
        }
        return null;
    }

    protected void setResponseContent(RestResult result, Response response) {
        response.setStatus(HttpResponseStatus.OK);
        response.setContent(new Gson().toJson(result));
    }
}
