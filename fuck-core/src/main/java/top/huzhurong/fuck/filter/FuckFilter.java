package top.huzhurong.fuck.filter;

import top.huzhurong.fuck.filter.annotation.FuckFIlterChain;
import top.huzhurong.fuck.transaction.support.Request;
import top.huzhurong.fuck.transaction.support.Response;

/**
 * @author luobo.cs@raycloud.com
 * @since 2018/12/4
 */
public interface FuckFilter {

    boolean filter(Request request, Response response, FuckFIlterChain fuckFIlterChain);

}