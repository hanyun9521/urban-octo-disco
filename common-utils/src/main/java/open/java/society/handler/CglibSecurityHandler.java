package open.java.society.handler;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.lang.reflect.Method;

/**
 * @Description:
 * @Author: hanyun
 * @Date: 2018/10/23
 * @Modified By: hanyun
 */
public class CglibSecurityHandler implements MethodInterceptor {

    private Object target;

    private JedisConnectionFactory jedisConnectionFactory;

    private String securityKey = "SecurityHandler";

    public CglibSecurityHandler(Object target, JedisConnectionFactory jedisConnectionFactory) {
        this.target = target;
        this.jedisConnectionFactory = jedisConnectionFactory;
    }

    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        RedisConnection redisConnection = jedisConnectionFactory.getConnection();
        String securityKey = createSecurityKey(proxy, method, args);
        try {
            if (redisConnection.setNX(securityKey.getBytes(), securityKey.getBytes())) {
                Object value = methodProxy.invoke(target, args);
                redisConnection.del(securityKey.getBytes());
                return value;
            }
        } finally {
            redisConnection.del(securityKey.getBytes());
        }


        return null;
    }

    private String createSecurityKey(Object proxy, Method method, Object[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(securityKey).append(method.getName());
        if (args != null && args.length >= 1) {
            for (Object arg : args) {
                stringBuilder.append(arg);
            }
        }

        return stringBuilder.toString();
    }
}
