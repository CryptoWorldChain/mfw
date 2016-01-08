package onight.scala.test;


import onight.sm.Ssm.PBSession;
import onight.sm.Ssm.RetCode;
import onight.sm.Ssm.TokenOp;
import onight.tfw.outils.serialize.SerializerFactory;

import com.google.protobuf.ExtensionRegistry;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

public class TestPBJSON {

	public static void main(String[] args) {

		ActorSessionTest session = new ActorSessionTest();
//		session.setLoginMS(1000002);
		session.setAttribute("aaabb", new Token(1, "ccccc", "userid").tokenid);
		session.setAttribute("aaabb1", new Token(1, "cccccddd", "userid").tokenid);
		session.setAttribute("aaabb2",new Token(1, "eeeee", "userid").tokenid);
//		session.setSmid("ifsffda");
//		session.setStatus(RetCode.FAILED_VALUE);
//		session.setToken(new Token(1, "tokenid", "userid"));
//		session.getTokens().add(new Token(TokenOp.TOKEN_CHECK_VALUE, "tokenid", "userid"));
//		session.getTokens().add(new Token(TokenOp.TOKEN_NEW_VALUE, "tokenid", "userid"));
		session.mapkvs();
		String jsonTxt = new String((byte[]) SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_JSON).serialize(session));
		System.out.println("Json::" + jsonTxt);
		PBSession.Builder builder = PBSession.newBuilder();
		BeanPBUtil util=new BeanPBUtil();
		
		try {
			util.json2PB(jsonTxt, builder);
			PBSession pbsess = builder.build();
			System.out.println("pbss=="+pbsess);
			JsonPBFormat jf=new JsonPBFormat();
			String str=jf.printToString(pbsess);
			System.out.println(str);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// builder.getMutableKvs().put("hello1", "abc");
		// builder.getMutableKvs().put("hello2", "abc");
		// builder.addUsers("abc");
		// builder.setLastUpdateMS(System.currentTimeMillis());
		// builder.setLoginMS(1000);
		// // Token tk=new Token(1, "abc", "userid");
		// builder.setTk(PBToken.newBuilder().setOp(TokenOp.TOKEN_CHECK).setTokenid("tkid").setUserid("useriddff"));
		// builder.addTokenss(PBToken.newBuilder().setOp(TokenOp.TOKEN_CHECK).setTokenid("tkid0").setUserid("useriddff0"));
		// builder.addTokenss(PBToken.newBuilder().setOp(TokenOp.TOKEN_CHECK).setTokenid("tkid1").setUserid("useriddff1"));
		//
		// builder.setSmid("smid.smid.smid");
		// PBSession session = builder.build();
		// BeanPBUtil util = new BeanPBUtil();
		// ActorSession actsess = util.copyFromPB(session, new ActorSession());
		// System.out.println(actsess.getUsers().getClass());
		// System.out.println("lastupdate="+actsess.getLastUpdateMS());
		// System.out.println(actsess);
		// System.out.println(actsess.getTk());
		// System.out.println(actsess.getTokenss().get(0).getOp());

	}
}
