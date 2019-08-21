package com.tfd.study.aop;

import org.springframework.stereotype.Service;

/**
 * @since TangFD@HF 2019/8/21.
 */
@Service
public class IServiceImpl implements IService {
	@Override
	public String say(String name) {
		return "IService hello " + name;
	}

	public double get(int num) {
		int i = 1 / 0;
		return Math.log(num);
	}
}
