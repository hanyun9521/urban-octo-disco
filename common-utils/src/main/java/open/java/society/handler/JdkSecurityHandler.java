package open.java.society.handler;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Description:
 * @Author: hanyun
 * @Date: 2018/10/23
 * @Modified By: hanyun
 */
public class JdkSecurityHandler implements InvocationHandler {

    private JedisConnectionFactory jedisConnectionFactory;

    private Object target;

    private String securityKey = "SecurityHandler";

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RedisConnection redisConnection = jedisConnectionFactory.getConnection();
        String securityKey = createSecurityKey(proxy, method, args);
        try {
            if (redisConnection.setNX(securityKey.getBytes(), securityKey.getBytes())) {
                Object value = method.invoke(target, args);
                return value;
            }
        } finally {
            redisConnection.del(securityKey.getBytes());
        }


        return null;
    }

    public JdkSecurityHandler(Object target, JedisConnectionFactory jedisConnectionFactory) {
        this.target = target;
        this.jedisConnectionFactory = jedisConnectionFactory;
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
