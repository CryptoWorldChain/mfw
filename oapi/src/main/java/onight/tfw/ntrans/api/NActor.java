package onight.tfw.ntrans.api;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CallBack;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.bean.ClassUtils;
import onight.tfw.outils.serialize.SerializerFactory;

import com.google.protobuf.AbstractMessage.Builder;
import com.googlecode.protobuf.format.JsonFormat;

/**
 * 处理节点的
 * 
 * @author brew
 *
 */
@Slf4j
public abstract class NActor<T> extends ActWrapper implements NPacketProccsor, ActorService {

	public NActor() {
	}

	// @Override
	// final public FramePacket send(FramePacket fp, long timeoutMS) {
	// return sender.send(fp, timeoutMS);
	// }
	//
	// @Override
	// final public void asyncSend(FramePacket fp, CallBack<FramePacket> cb) {
	// sender.asyncSend(fp, cb);
	// }
	//
	// @Override
	// final public void post(FramePacket fp) {
	// sender.post(fp);
	// }

	@Override
	public void onPacket(FramePacket pack, CompleteHandler handler) {
		super.onPacket(pack, handler);
	}

	public T getPBBody(FramePacket pack) {
		if (pack.getBody().length > 0) {
			if (pack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_PROTOBUF) {
				try {
					if (getPBBuilder() != null) {
						return (T) getPBBuilder().mergeFrom(pack.getBody()).build();
					}
					return null;
				} catch (Exception e) {
					log.debug("cannot invoke pb builder for pack:" + pack.getFixHead(), e);
				}
			} else if (pack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_JSON) {
				try {
					String jsonTxt = new String(pack.getBody(), "UTF-8");
					Builder builder = getPBBuilder();
					if (builder != null) {
						JsonFormat.merge(jsonTxt, builder);
						return (T) builder.build();
					}
					return null;

				} catch (Exception e) {
					log.debug("cannot invoke pb builder for pack from JSON:" + pack.getFixHead(), e);
				}
			} else {
				log.debug("cannot invoke  builder for pack this ENC TYPE:" + pack.getFixHead().getEnctype() + ",head=" + pack.getFixHead());
			}
		}
		return null;
	}

	Method bm;

	public Builder getPBBuilder() {
		if (bm == null) {
			try {
				bm = getBeanType().getMethod("newBuilder", null);
			} catch (Exception e) {
				log.warn("cannot found pb builder for class" + getBeanType(), e);
			}
		}
		if(bm!=null)
		try {
			return (Builder) bm.invoke(null, null);
		} catch (Exception e) {
			log.warn("cannot found pb builder for class" + getBeanType(), e);
		}
		return null;
	}

	public Class getBeanType() {
		return ClassUtils.getFirstParameterizedClass(getClass());
//		Class ret = null;
//		try {
//			ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
//			ret = (Class) parameterizedType.getActualTypeArguments()[0];
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return ret;

	}

}
