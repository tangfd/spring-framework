package com.tfd.study.aop;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author TangFD
 * @since 2019/8/14.
 */
@EnableAspectJAutoProxy
@ComponentScan("com.tfd.study.aop")
public class TestAop {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(TestAop.class);
		TestService service = context.getBean(TestService.class);
		System.out.println(service.say("aop"));
		IService iService = context.getBean(IService.class);
		System.out.println(iService.say("i-aop"));
//		System.out.println(service.get(10));
		System.out.println(iService.get(10));
	}
}
