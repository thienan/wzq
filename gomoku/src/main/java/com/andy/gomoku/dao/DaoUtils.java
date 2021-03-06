package com.andy.gomoku.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andy.gomoku.base.PageVO;
import com.andy.gomoku.dao.vo.GenTable;
import com.andy.gomoku.dao.vo.GenTableColumn;
import com.andy.gomoku.dao.vo.NameValue;
import com.andy.gomoku.dao.vo.Where;
import com.andy.gomoku.entity.BaseEntity;
import com.andy.gomoku.exception.GoSeviceException;
import com.andy.gomoku.utils.EntityMetadata;
import com.andy.gomoku.utils.EntityUtils;
import com.andy.gomoku.utils.ReflectUtil;
import com.andy.gomoku.utils.SpringContextHolder;
import com.google.common.collect.Lists;

public class DaoUtils {

	static Logger logger = LoggerFactory.getLogger(DaoUtils.class);
	
	static DataSource dataSource = SpringContextHolder.getBean(DataSource.class);
	
	static GmkBeanProcessor convert = new GmkBeanProcessor();
	static BasicRowProcessor rowProcessor = new BasicRowProcessor(convert);

	private static ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();
	
	private static final String keywordEscape = "`";

	/**
	 * 根据ID查询实体
	 * 
	 * @param id
	 * @param clasz
	 * @return
	 */
	public static <T> T get(Serializable id, Class<T> clasz) {
		QueryRunner run = new QueryRunner(dataSource);
		ResultSetHandler<T> h = new BeanHandler<T>(clasz,rowProcessor);
		try {
			T p = run.query("SELECT * FROM " + toTable(clasz.getSimpleName()) + " WHERE id=?", h, id);
			return p;
		} catch (SQLException e) {
			throw new GoSeviceException(e);
		}
	}

	public static String toTable(String entity) {
		if(entity.indexOf('_') > 0) return entity;
		String[] strs = StringUtils.splitByCharacterTypeCamelCase(entity);
		return StringUtils.join(strs, "_");	
	}

	/**
	 * 根据条件查询列表
	 * 
	 * @param clasz
	 * @param conds
	 * @return
	 */
	public static <T> List<T> getList(Class<T> clasz, Where... conds) {
		return getList(clasz, null, null, null, null, conds);
	}
	public static <T> T getOne(Class<T> clasz, Where... conds) {
		List<T> list = getList(clasz, null, null, null, null, conds);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	public static <T> List<T> getList(Class<T> clasz, String sort, String limit, Where... conds) {
		return getList(clasz, sort, limit, null, null, conds);
	}
	public static <T> T getOne(Class<T> clasz, String sort, String limit, Where... conds) {
		List<T> list = getList(clasz, sort, limit, null, null, conds);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	public static List<Map<String, Object>> getListMap(String table,String field,Integer limit, Where... conds) {
		QueryRunner run = new QueryRunner(dataSource);
		Object[] wh = buildWhere(conds);
		Object[] vas = (Object[]) wh[1];
		if(field == null){
			field = "*";
		}
		String limits = "";
		if(limit != null){
			limits = " LIMIT 0," + limit;
		}
		try {
			List<Map<String, Object>> list = run.query("SELECT " + field + " FROM " + table + wh[0] + limits, new MapListHandler(),vas);
			return list;
		} catch (SQLException e) {
			throw new GoSeviceException(e);
		}
	}
	
	public static PageVO getPageForMap(String table,Integer page,Integer epage, Where... conds) {
		return getPageForMap(table, null , null, page, epage, conds);
	}
	public static PageVO getPageForMap(String table,String field,Integer page,Integer epage, Where... conds) {
		return getPageForMap(table, field, null , page, epage, conds);
	}
	
	public static PageVO getPageForMap(String table,String field,String sort,Integer page,Integer epage, Where... conds) {
		QueryRunner run = new QueryRunner(dataSource);
		PageVO pageVO = new PageVO();
		if(page==null) {
			page = 1;
		}
		if(epage==null) {
			epage = 10;
		}
		pageVO.setPage(page);
		pageVO.setEpage(epage);
		Object[] wh = buildWhere(conds);
		Object[] vas = (Object[]) wh[1];
		if(field == null){
			field = "*";
		}
		String limits = " LIMIT "+(page-1) * epage+","+ epage;
		if(sort == null){
			sort = "";
		}else{
			sort = " order by "+sort;
		}
		try {
			List<Map<String, Object>> list = run.query("SELECT " + field + " FROM " + table + wh[0] + sort + limits, new MapListHandler(), vas);
			Map<String, Object> count = run.query("SELECT count(1) as count FROM " + table + wh[0], new MapHandler(), vas);
			pageVO.setItems(list);
			pageVO.setTotal_items(MapUtils.getInteger(count, "count"));
			return pageVO;
		} catch (SQLException e) {
			throw new GoSeviceException(e);
		}
	}
	
	private static Object[] buildWhere(Where... conds) {
		String where = "";
		StringBuilder sb = new StringBuilder();
		Object[] vas = null;
		if (conds.length > 0) {
			vas = new Object[conds.length];
			int c =0;
			for (int i = 0; i < conds.length; i++) {
				Where nv = conds[i];
				sb.append(" AND ").append(keywordEscape).append(nv.getName()).append(keywordEscape).append(" ").append(nv.getCondition());
				if(nv.getCondition().equals(Condition.IN) || nv.getCondition().equals(Condition.NOT_IN)) {
					sb.append(" (").append(nv.getValue()).append(") ");
				}else {
					c++;
					sb.append(" ? ");
					vas[i] = nv.getValue();
				}
				vas[i] = nv.getValue();
			}
			vas = Arrays.copyOf(vas, c);
			where = " WHERE 1=1" + sb.toString();
		}
		return new Object[]{where,vas};
	}
	
	public static Map getOne(String field, String group, Where... conds) {
		List<Map> list = getList(Map.class, null, null, field, group, conds);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	public static <T> List<T> getListSql(Class<T> clasz, String sql, Object... conds) {
		QueryRunner run = new QueryRunner(dataSource);
		ResultSetHandler<List<T>> h = new BeanListHandler<T>(clasz);
		try {
			List<T> list = run.query(sql, h, conds);
			return list;
		} catch (SQLException e) {
			throw new GoSeviceException(e);
		}
	}
	
	public static List<Map<String, Object>> getListSql(String sql, Object... conds) {
		QueryRunner run = new QueryRunner(dataSource);
		try {
			List<Map<String, Object>> list = run.query(sql, new MapListHandler(), conds);
			return list;
		} catch (SQLException e) {
			throw new GoSeviceException(e);
		}
	}

	public static <T> T getOneSql(Class<T> clasz, String sql, Object... conds) {
		List<T> list = getListSql(clasz, sql, conds);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	/**
	 * 根据条件查询排序列表
	 * 
	 * @param clasz
	 * @param group
	 * @param sort
	 * @param limit
	 * @param conds
	 * @return
	 */
	public static <T> List<T> getList(Class<T> clasz, String sort, String limit, String field, String group,
			Where... conds) {
		QueryRunner run = new QueryRunner(dataSource);
		
		ResultSetHandler<List<T>> h = new BeanListHandler<T>(clasz,rowProcessor);
		Object[] wh = buildWhere(conds);
		Object[] vas = (Object[]) wh[1];
		if(field == null){
			field = "*";
		}
		String limits = "";
		if(limit != null){
			if(limit.indexOf(",") > 0){
				limits = " LIMIT " + limit;
			}else{
				limits = " LIMIT 0," + limit;
			}
		}
		try {
			List<T> list= run.query(
						"SELECT " + field + " FROM " + toTable(clasz.getSimpleName()) + wh[0]
								+ (group == null ? "" : " GROUP BY " + group)
								+ (sort == null ? "" : " ORDER BY " + sort) + limits, h, vas);
			return list;
		} catch (SQLException e) {
			throw new GoSeviceException(e);
		}
	}

	/**
	 * 插入实体
	 * 
	 * @param entity
	 * @return
	 */
	public static int insert(Object entity) {
		QueryRunner run = new QueryRunner(dataSource);
		try {
			List<NameValue> fields = null;
			String table = "";
			if (entity instanceof BaseEntity) {
				((BaseEntity) entity).setCreateTime(System.currentTimeMillis()/1000);
				fields = EntityUtils.getNameValues((BaseEntity) entity, false,false);
				table = toTable(entity.getClass().getSimpleName());
			} else if (entity instanceof Map) {
				table = (String) ((Map) entity).remove("table_");
				((Map) entity).put("create_time", System.currentTimeMillis()/1000);
				fields = Lists.newArrayList();
				for (Entry<String, Object> fc : ((Map<String, Object>) entity).entrySet()) {
					Object value = fc.getValue();
					if (value != null) {
						String column = toTable(fc.getKey());
						fields.add(new NameValue(column, value));
					}
				}
			} else {
				return 0;
			}
			StringBuilder sb = new StringBuilder();
			Object[] vas = new Object[fields.size()];
			for (int i = 0; i < fields.size(); i++) {
				NameValue nv = fields.get(i);
				sb.append(",").append(keywordEscape).append(nv.getName()).append(keywordEscape);
				vas[i] = nv.getValue();
			}
			Object[] insert = run.insert("INSERT INTO " + table + " (" + sb.substring(1) + ") VALUES ("
					+ StringUtils.repeat("?", ",", fields.size()) + ")",new ArrayHandler(), vas);
			Long idd = ((Long) insert[0]);
			if (entity instanceof BaseEntity) {
				((BaseEntity) entity).setId(idd);
			}else{
				((Map<String, Object>) entity).put("id", idd);
			}
			return 1;
		} catch (SQLException sqle) {
			throw new GoSeviceException(sqle);
		}
	}
	
	/**
	 * 批量插入
	 * @param entitys
	 * @return 
	 */
	public static int[] batchSave(List<? extends BaseEntity> entitys) {
		if(entitys == null || entitys.isEmpty()) return null;
		QueryRunner run = new QueryRunner(dataSource);
		
		EntityMetadata metadata = EntityUtils.getEntityMetadata(entitys.get(0).getClass());
		String[] fields = metadata.getFieldSelect().split(",");
		
		Object[][] params = new Object[entitys.size()][fields.length-1];
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fields.length; i++) {
			if(!fields[i].equalsIgnoreCase("id")){
				sb.append(",").append(keywordEscape).append(fields[i]).append(keywordEscape);
			}
		}
		for(int i=0;i<entitys.size();i++){
			entitys.get(i).setCreateTime(System.currentTimeMillis()/1000);
			int j = 0;
			for (String fi:fields) {
				if(!fi.equalsIgnoreCase("id")){
					params[i][j] = ReflectUtil.getFieldValue(entitys.get(i), fi.replace("_", ""));
					j++;
				}
			}
		}
		
		try {
			int[] inserts = run.batch("INSERT INTO " + toTable(entitys.get(0).getClass().getSimpleName()) + 
					" (" + sb.substring(1) + ") VALUES (" + StringUtils.repeat("?", ",", fields.length-1) + ")", params);
//			for(int i=0;i<inserts.length;i++){
//				entitys.get(i).setId((long) inserts[i]);
//			}
			return inserts;
		} catch (SQLException sqle) {
			throw new GoSeviceException(sqle);
		}
	}


	/**
	 * 更新实体
	 * 
	 * @param entity
	 * @return
	 */
	public static int update(Object entity) {
		QueryRunner run = new QueryRunner(dataSource);
		List<NameValue> fields = null;
		Serializable id = null;
		if (entity instanceof BaseEntity) {
			((BaseEntity) entity).setUpdateTime(System.currentTimeMillis()/1000);
			fields = EntityUtils.getNameValues((BaseEntity) entity, false,false);
			id = ((BaseEntity) entity).getId();
		} else if (entity instanceof Map) {
			((Map) entity).put("update_time", System.currentTimeMillis()/1000);
			id = (Serializable) ((Map) entity).get("id");
			fields = Lists.newArrayList();
			for (Entry<String, Object> fc : ((Map<String, Object>) entity).entrySet()) {
				if ("id".equals(fc.getKey()))
					continue;
				Object value = fc.getValue();
				if (value != null) {
					String column = toTable(fc.getKey());
					fields.add(new NameValue(column, value));
				}
			}
		} else {
			return 0;
		}
		String table = toTable(entity.getClass().getSimpleName());
		StringBuilder sb = new StringBuilder();
		Object[] vas = new Object[fields.size() + 1];
		for (int i = 0; i < fields.size(); i++) {
			NameValue nv = fields.get(i);
			sb.append(",").append(keywordEscape).append(nv.getName()).append(keywordEscape).append("=?");
			vas[i] = nv.getValue();
		}
		vas[fields.size()] = id;
		try {
			int updates = run.update("UPDATE " + table + " SET " + sb.substring(1) + " WHERE id=?", vas);
			return updates;
		} catch (SQLException sqle) {
			throw new GoSeviceException(sqle);
		}
	}
	
	
	/**
	 * 批量更新
	 * @param entity
	 * @return
	 */
	public static int[] updateBatch(String field, List<? extends BaseEntity> entitys) {
		if(field == null || entitys == null || entitys.isEmpty()) return null;
		QueryRunner run = new QueryRunner(dataSource);
		String[] fields = StringUtils.split(field,",");
		Object[][] params = new Object[entitys.size()][fields.length+1];
		String table = toTable(entitys.get(0).getClass().getSimpleName());
		StringBuilder sb = new StringBuilder("");
		for(String fie:fields){
			sb.append(",").append(keywordEscape).append(toTable(fie)).append(keywordEscape).append("=?");
		}
		
		for(int i=0;i<entitys.size();i++){
			entitys.get(i).setUpdateTime(System.currentTimeMillis()/1000);
			for (int j = 0; j < fields.length; j++) {
				params[i][j] = ReflectUtil.getFieldValue(entitys.get(i), fields[j]);
			}
			params[i][fields.length] = entitys.get(i).getId();
		}
		
		try {
			int[] updates = run.batch("UPDATE " + table + " SET " + sb.substring(1) + " WHERE id=?", params);
			return updates;
		} catch (SQLException sqle) {
			throw new GoSeviceException(sqle);
		}
	}

	/**
	 * 更新数据
	 * 
	 * @param sql
	 * @param conds
	 * @return
	 */
	public static int update(String sql, Object... conds) {
		QueryRunner run = new QueryRunner(dataSource);
		try {
			int updates = run.update(sql, conds);
			return updates;
		} catch (SQLException sqle) {
			throw new GoSeviceException(sqle);
		}
	}
	
	/**
	 * 删除数据
	 * @param table
	 * @param i
	 */
	public static int delete(String table, long id) {
		QueryRunner run = new QueryRunner(dataSource);
		try {
			int delets = run.update("DELETE FROM " + table + " WHERE id=?", id);
			return delets;
		} catch (SQLException sqle) {
			throw new GoSeviceException(sqle);
		}
	}
	
	/**
	 * 删除数据
	 * @param table
	 * @param i
	 */
	public static int delete(String table, Where... conds) {
		QueryRunner run = new QueryRunner(dataSource);
		Object[] wh = buildWhere(conds);
		try {
			int delets = run.update("DELETE FROM " + table + wh[0], (Object[])wh[1]);
			return delets;
		} catch (SQLException sqle) {
			throw new GoSeviceException(sqle);
		}
	}
	
	public static GenTable getTables(final String table){
		GenTable tables = getOneSql(GenTable.class, "SELECT t.table_name AS NAME,t.TABLE_COMMENT AS comments FROM information_schema.`TABLES` t "
								+ "WHERE t.TABLE_SCHEMA = (SELECT DATABASE()) AND t.TABLE_NAME =(?) ORDER BY t.TABLE_NAME",table);
		List<GenTableColumn> cols = getListSql(GenTableColumn.class, "SELECT t.COLUMN_NAME AS NAME,(CASE WHEN t.IS_NULLABLE = 'YES' THEN '1' ELSE '0'	END	) AS isNull,"
				+ "	(t.ORDINAL_POSITION * 10) AS sort,	t.COLUMN_COMMENT AS comments, t.COLUMN_TYPE AS jdbcType FROM information_schema.`COLUMNS` t"
				+ " where t.TABLE_SCHEMA = (SELECT DATABASE()) AND t.TABLE_NAME = (?) ORDER BY	t.ORDINAL_POSITION",table);
		tables.setColumnList(cols);
		return tables;
	}

	/**
	 * @Method: startTransaction
	 * @Description: 开启事务
	 */
	public static void startTransaction() {
		try {
			Connection conn = threadLocal.get();
			if (conn == null) {
				conn = dataSource.getConnection();
				threadLocal.set(conn);
			}
			// 开启事务
			conn.setAutoCommit(false);
		} catch (Exception e) {
			throw new GoSeviceException(e);
		}
	}

	/**
	 * @Method: commit
	 * @Description:提交事务
	 */
	public static void commit() {
		try {
			// 从当前线程中获取Connection
			Connection conn = threadLocal.get();
			if (conn != null) {
				// 提交事务
				conn.commit();
			}
		} catch (Exception e) {
			throw new GoSeviceException(e);
		}
	}

	/**
	 * @Method: close
	 * @Description:关闭数据库连接(注意，并不是真的关闭，而是把连接还给数据库连接池)
	 * @Anthor:
	 *
	 */
	public static void close() {
		try {
			// 从当前线程中获取Connection
			Connection conn = threadLocal.get();
			if (conn != null) {
				conn.close();
				// 解除当前线程上绑定conn
				threadLocal.remove();
			}
		} catch (Exception e) {
			throw new GoSeviceException(e);
		}
	}


}
