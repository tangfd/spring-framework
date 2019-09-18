package com.tfd.study.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @since TangFD@HF 2019/9/18.
 */
@Service
public class TestIServiceImpl implements IService {
	@Resource
	private JdbcTemplate jdbcTemplate;

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
	public void save() {
		System.out.println("----testIServiceImpl-save---");
		jdbcTemplate.execute("INSERT into t_test(name) VALUES('" + UUID.randomUUID() + "');");
	}

	@Override
	@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
	public String get() {
		return null;
	}
}
