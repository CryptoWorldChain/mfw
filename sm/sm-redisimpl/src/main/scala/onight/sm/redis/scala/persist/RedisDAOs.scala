package onight.sm.redis.scala.persist

import scala.beans.BeanProperty
import com.google.protobuf.Message
import onight.osgi.annotation.NActorProvider
import onight.tfw.ojpa.api.OJpaDAO
import onight.tfw.ojpa.api.annotations.StoreDAO
import onight.sm.redis.entity.SMIDSession
import onight.tfw.outils.serialize.UUIDGenerator
import com.google.common.hash.HashCode
import onight.sm.redis.scala.SessionModules
import onight.sm.redis.entity.TokenEncKeys
import onight.sm.redis.entity.LoginResIDSession

@NActorProvider
object RedisDAOs extends SessionModules[Message] {
  @BeanProperty
  @StoreDAO(domain = classOf[SMIDSession], key = "smid", target = "redis")
  var smiddao: OJpaDAO[SMIDSession] = null

  @StoreDAO(domain = classOf[LoginResIDSession], key = "loginId,resId", target = "redis")
  @BeanProperty
  var logiddao: OJpaDAO[LoginResIDSession] = null

  @StoreDAO(domain = classOf[TokenEncKeys], key = "timeIdx", target = "redis")
  @BeanProperty
  var tokenDao: OJpaDAO[TokenEncKeys] = null

  def newSMID(userId: String, loginId: String, password: String, resourceid: String, ext: String) {
    val smid = UUIDGenerator.generate() + "_" + loginId;
    HashCode.fromString(smid)
    val logsession = SMIDSession(smid, userId, loginId, password, resourceid, ext)
  }

}

