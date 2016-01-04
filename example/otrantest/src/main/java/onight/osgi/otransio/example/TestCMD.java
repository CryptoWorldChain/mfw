package onight.osgi.otransio.example;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.pb.test.Test1;
import onight.pb.test.Test1.retloginpack;
import onight.tfw.otransio.api.CMDType;
import onight.tfw.otransio.api.OPacket;
import onight.tfw.otransio.api.CMDProcessor;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;
import onight.tfw.proxy.ActorProxy;
import onight.tfw.proxy.IActor;

import org.apache.felix.ipojo.annotations.Provides;

import com.google.protobuf.GeneratedMessage.Builder;
import com.google.protobuf.MessageLite;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

@iPojoBean
@Provides(specifications = { CMDProcessor.class ,IActor.class})
@Slf4j
public class TestCMD extends ActorProxy<Test1.loginpack> {

	public CMDType getCMDType() {
		return new CMDType("hello", 0, Test1.loginpack.class);
	}
	


	@Override
	public String getWebPath() {
		return "/hello.do";
	}


	@Override
	public OPacket onSocketObject(OPacket pack, Test1.loginpack pbo) {
		log.debug("获取到消息：" + pack);
		retloginpack ret = retloginpack.newBuilder().setUserid(pbo.getUserid()).setStatus("SUCCESS").setRetcode(0)
				.setDesc("last login time:2015-08-18").build();
		return pack.toPBReturnPack(ret);
	}

	public static void main(String[] args) {
		Test1.loginpack pack = Test1.loginpack.newBuilder().setUserid("abcdef").build();
		Object array = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_PROTOBUF).serialize(pack);

		Test1.loginpack pack2 = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_PROTOBUF).deserialize(
				array, Test1.loginpack.class);

		System.out.println(Test1.loginpack.class.isAssignableFrom(MessageLite.class));
		System.out.println(MessageLite.class.isAssignableFrom(Test1.loginpack.class));
		System.out.println(pack.getUserid());
		System.out.println(pack2.getUserid());
		Test1.loginpack.Builder packb = Test1.loginpack.newBuilder();
		Test1.loginpack pack3;
		try {
			JsonFormat.merge(JsonFormat.printToString(pack), packb);
			pack3 = packb.build();
			System.out.println("pack3=" + pack3.getUserid());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(JsonFormat.printToString(pack));
		System.out.println(JsonFormat.printToString(pack2));

	}
}
