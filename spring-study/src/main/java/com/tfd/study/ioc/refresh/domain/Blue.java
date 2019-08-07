package com.tfd.study.ioc.refresh.domain;

import javax.annotation.Resource;

/**
 * @since TangFD@HF 2019/7/25.
 */
public class Blue {
	@Resource
	private Red red;

	public Blue() {
		System.out.println("blue constructor......");
	}
}
