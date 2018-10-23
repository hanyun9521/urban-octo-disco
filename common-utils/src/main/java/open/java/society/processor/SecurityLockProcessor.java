package open.java.society.processor;

import open.java.society.annotations.SecurityLock;
import open.java.society.handler.CglibSecurityHandler;
import open.java.society.handler.JdkSecurityHandler;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * @Description:
 * @Author: hanyun
 * @Date: 2018/10/23
 * @Modified By: hanyun
 */
public class SecurityLockProcessor implements BeanPostProcessor {

    private JedisConnectionFactory jedisConnectionFactory;

    private Set<Object> beans = new HashSet<Object>();

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beans.contains(bean)) {
            return bean;
        }

        Object target = bean;
        try {
            while (AopUtils.isAopProxy(target)) {
                target = getTarget(target);
            }
        } catch (Exception ex) {}

        Method[] methods = target.getClass().getDeclaredMethods();
        boolean needSecurityOps = false;
        for (Method method : methods) {
            if (method.isAnnotationPresent(SecurityLock.class)) {
                needSecurityOps = true;
                break;
            }
        }

        if (needSecurityOps) {
            beans.add(target);
        }

        return needSecurityOps ? getProxy(bean, beanName) : bean;
    }

    public Object getTarget(Object proxy) throws Exception {

        if(!AopUtils.isAopProxy(proxy)) {
            return proxy;
        }

        if(AopUtils.isJdkDynamicProxy(proxy)) {
            return getJdkDynamicProxyTargetObject(proxy);
        } else { //cglib
            return getCglibProxyTargetObject(proxy);
        }
    }

    private Object getProxy(Object bean, String beanName) {
        JdkSecurityHandler securityHandler = new JdkSecurityHandler(bean, jedisConnectionFactory);
        Class claz = bean.getClass();
        List<Class<?>> allInterfaces = new ArrayList<Class<?>>();
        while(!claz.equals(Object.class)) {
            Class<?>[] clazes = claz.getInterfaces();
            if (clazes != null && clazes.length > 0) {
                List<Class<?>> classes = Arrays.asList(clazes);
                allInterfaces.addAll(classes);
            }
            claz = claz.getSuperclass();
        }

        if (CollectionUtils.isEmpty(allInterfaces)) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(bean.getClass());
            CglibSecurityHandler cglibSecurityHandler = new CglibSecurityHandler(bean, jedisConnectionFactory);
            enhancer.setCallback(cglibSecurityHandler);
            return enhancer.create();
        }

        Class[] interfaces = new Class[allInterfaces.size()];
        allInterfaces.toArray(interfaces);

        return Proxy.newProxyInstance(bean.getClass().getClassLoader(), interfaces, securityHandler);
    }

    private Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);

        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);

        Object target = ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();
        return target;
    }

    private Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);

        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);

        Object target = ((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();

        return target;
    }
}
