package org.example.mqexample;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JmsListener Annotation 의 destination 값으로 Wildcard 문자인 Asterisk(*) 를 사용해 리스닝할 수 있는데,
 * 실제 어떤 경로로 메시지가 들어왔는지 Asterisk에 대응하는 경로를 캡쳐할 때 사용할 수 있는 Annotation 이다.
 * <p>
 * value 는 몇 번째 Asterisk 에 대응하는 경로를 캡쳐할 지 결정하며 기본값은 0이다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface DestinationCapture {
    int value() default 0;
}