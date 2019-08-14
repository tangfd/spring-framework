package com.tfd.study.aop;

import org.springframework.stereotype.Service;

/**
 * AOP测试被代理接口
 *
 * @author TangFD
 * @since 2019/8/14.
 */
@Service
public class TestService {

	public String say(String name) {
		return "hello " + name;
	}

	public double get(int num) {
		int i=1/0;
		return Math.log(num);
	}
}
