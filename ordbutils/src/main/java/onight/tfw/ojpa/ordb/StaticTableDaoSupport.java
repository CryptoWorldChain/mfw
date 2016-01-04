package onight.tfw.ojpa.ordb;

import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;

public interface StaticTableDaoSupport<T,D,K> {
	
    int countByExample(D example);

    int deleteByExample(D example) throws Exception;

    int deleteByPrimaryKey(K key) throws Exception;

    int insert(T record) throws Exception;

    int insertSelective(T record) throws Exception;
    
    int batchInsert(List<T> records)  throws Exception;
    
    int batchUpdate(List<T> records)  throws Exception;
    
    int batchDelete(List<T> records)  throws Exception;

    List<T> selectByExample(D example) ;
    
	T selectOneByExample(D example) ;

    T selectByPrimaryKey(K key);
    
    List<T> findAll(List<T> records) ;

    int updateByExampleSelective(T record, D example) throws Exception;

    int updateByExample(T record, D example) throws Exception;

    int updateByPrimaryKeySelective(T record) throws Exception;

    int updateByPrimaryKey(T record) throws Exception;

    int sumByExample(D example);
    
    void deleteAll() throws Exception;
    
	D getExample(T record);
	
	SqlSessionFactory getSqlSessionFactory();

}