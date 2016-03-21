package onight.sm.redis.scala.persist

import java.util.concurrent.TimeUnit
import org.codehaus.jackson.map.util.LRUMap
import com.google.common.cache.CacheBuilder
import com.google.common.cache.LoadingCache
import onight.sm.redis.scala.persist.MysqlDAOs.KOLoginUser
import onight.tfw.outils.conf.PropHelper
import com.google.common.cache.Cache
import com.github.mauricio.async.db.RowData

object VMDaos {
  val pconfig: PropHelper = new PropHelper(null);
 
//  val dbCache = new LRUMap[String, KOLoginUser](10000, pconfig.get("ssm.vmcache.dbmax", 100000))
//  val redisCacheByLoginId = new LRUMap[String, LogSessionBySMID](100000, pconfig.get("ssm.vmcache.redismax", 100000))

  val dbCache: Cache[String, RowData] = CacheBuilder.newBuilder().maximumSize(pconfig.get("ssm.vmcache.dbmax", 100000)) //
    .expireAfterWrite(pconfig.get("ssm.vmcache.expire", 60), TimeUnit.SECONDS)
    .build();

    val pwdCache: Cache[String, Map[String, Any]] = CacheBuilder.newBuilder().maximumSize(pconfig.get("ssm.vmcache.dbmax.pwd", 1000000)) //
    .expireAfterWrite(pconfig.get("ssm.vmcache.expire", 60), TimeUnit.SECONDS)
    .build();
  //  LoadingCache[Key, Graph] graphs = CacheBuilder.newBuilder()
  //       .maximumSize(10000)
  //       .expireAfterWrite(10, TimeUnit.MINUTES)
  //       .removalListener(MY_LISTENER)
  //       .build(
  //           new CacheLoader<Key, Graph>() {
  //             public Graph load(Key key) throws AnyException {
  //               return createExpensiveGraph(key);
  //             }
  //           });

}