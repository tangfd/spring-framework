package com.tfd.study.tx;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @since TangFD@HF 2019/9/4.
 */
@Service
public class TestService {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
	public void save(String name) {
		System.out.println("----tx-save---");
		jdbcTemplate.execute("INSERT into t_test(name) VALUES('" + name + "');");
	}

	@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
	public void rollback(String name) {
		System.out.println("----tx-rollback---");
		jdbcTemplate.execute("INSERT into t_test(name) VALUES('" + name + "'');");
		int i = 1 / 0;
	}

	@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
	public void get(String name) {
		System.out.println("----tx-get---" + name);
		jdbcTemplate.query("SELECT * FROM t_test;", (rs, rowNum) -> {
			System.out.print("id: " + rs.getInt("id"));
			System.out.println("name: " + rs.getString("name"));
			return null;
		});
	}

	public void getNoTransactional(String name) {
		System.out.println("----tx-get---" + name);
		jdbcTemplate.query("SELECT * FROM t_test;", (rs, rowNum) -> {
			System.out.print("id: " + rs.getInt("id"));
			System.out.println("name: " + rs.getString("name"));
			return null;
		});
	}
}
