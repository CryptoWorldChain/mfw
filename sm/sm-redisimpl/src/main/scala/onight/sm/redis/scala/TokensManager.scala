package onight.sm.redis.scala

import onight.tfw.outils.serialize.SessionIDGenerator
import java.util.Calendar
import onight.sm.redis.scala.persist.RedisDAOs
import onight.sm.redis.entity.TokenEncKeys
import onight.tfw.ojpa.api.TransactionExecutor
import onight.tfw.mservice.ThreadContext
import onight.tfw.ojpa.api.CASCriteria
import onight.tfw.ojpa.api.CASCriteria.Cause
import onight.tfw.ojpa.api.JpaContextConstants
import onight.sm.redis.scala.persist.TokenRedisLoCache
import org.apache.commons.lang3.StringUtils

object TokensManager {

  def getKeys(timeIdx: String = SMIDHelper.cureTimeIdx): String = {
    "tkofw20:" + timeIdx //1分钟产生一个key，
  }

  def ensureKeys: String = {
    val timeIdx = getKeys();
    val tokens = TokenEncKeys(timeIdx);
    val lotoken = TokenRedisLoCache.get(tokens)
    if (lotoken != null && (System.currentTimeMillis() - lotoken.gentime) / 1000 < 5 * 60) { //
      //1分钟之内还是可以使用的
      return lotoken.getEnckey(); //一分钟一次
    }
    timeIdx.intern().synchronized({//reentry,lock
      val lotoken = TokenRedisLoCache.get(tokens)
      if (lotoken != null && (System.currentTimeMillis() - lotoken.gentime) / 1000 < 5 * 60) { //
        //1分钟之内还是可以使用的
        return lotoken.getEnckey(); //一分钟一次
      }
      //内存里没有或者超过时间了，重新生成一个
      tokens.enckey = SMIDHelper.nextSMID("TMTK");
      ThreadContext.setContext(JpaContextConstants.Cache_Timeout_Second, 3600) //1小时之后取消
      tokens.gentime = System.currentTimeMillis()
      if (RedisDAOs.tokenDao.insertIfNoExist(tokens).asInstanceOf[Boolean]) { //插入成功
        TokenRedisLoCache.redisLocalCache.put(tokens.timeIdx, tokens)
        return tokens.enckey;
      } //插入失败了
      val redisToken = RedisDAOs.tokenDao.selectByPrimaryKey(tokens);
      TokenRedisLoCache.redisLocalCache.put(redisToken.timeIdx, redisToken)
      return redisToken.enckey;
    })
  }
  def genToken(userid: String): String = {
    SMIDHelper.nextToken(userid, ensureKeys);
  }

  def checkToken(token: String, userid: String = "*"): String = {
    val timeIdx = getKeys(token.substring(token.length() - 3, token.length() - 1));
    val tokens = TokenEncKeys(timeIdx);
    val lotoken = TokenRedisLoCache.get(tokens)
    if (lotoken != null && (System.currentTimeMillis() - lotoken.gentime) / 1000 < 10 * 60) { //
      //10分钟之内还是可以使用的
      return SMIDHelper.checkToken(token, lotoken.enckey)
    }
    return null;
  }

  def main(args: Array[String]): Unit = {
    println(TokensManager.getKeys())
  }
}