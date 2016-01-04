package onight.tfw.ojpa.ordb;

import java.util.List;

public interface DynamicTableDaoSupport<T,D,K> {
	
    int countByExample(D example,String table);

    int deleteByExample(D example,String table);

    int deleteByPrimaryKey(K key,String table);

    int insert(T record,String table);

    int insertSelective(T record,String table);
    
    int batchInsert(List<T> records,String table) ;
    
    int batchUpdate(List<T> records,String table) ;
    
    int batchDelete(List<T> records,String table) ;

    List<T> selectByExample(D example,String table) ;
    
    T selectOneByExample(D example,String table) ;

    T selectByPrimaryKey(K key,String table);
    
    List<T> findAll(List<T> records,String table) ;

    int updateByExampleSelective(T record, D example,String table);

    int updateByExample(T record, D example,String table);

    int updateByPrimaryKeySelective(T record,String table);

    int updateByPrimaryKey(T record,String table);

    int sumByExample(D example,String table);
    
    void deleteAll(String table);
    
	D getExample(T record);

}