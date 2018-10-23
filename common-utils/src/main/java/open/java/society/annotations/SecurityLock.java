package open.java.society.annotations;

import java.lang.annotation.*;

/**
 * @Description:
 * @Author: hanyun
 * @Date: 2018/10/23
 * @Modified By: hanyun
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SecurityLock {
    String key() default "";
}
