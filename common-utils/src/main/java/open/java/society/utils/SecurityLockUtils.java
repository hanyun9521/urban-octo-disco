package open.java.society.utils;

import java.lang.reflect.Method;

/**
 * @Description:
 * @Author: hanyun
 * @Date: 2018/10/23
 * @Modified By: hanyun
 */
public class SecurityLockUtils {

    public static final String KEY_PREFIX = "SecurityHandler";

    public static String createSecurityKey(Object proxy, Method method, Object[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(KEY_PREFIX).append(method.getName());
        if (args != null && args.length >= 1) {
            for (Object arg : args) {
                stringBuilder.append(arg);
            }
        }

        return stringBuilder.toString();
    }
}
