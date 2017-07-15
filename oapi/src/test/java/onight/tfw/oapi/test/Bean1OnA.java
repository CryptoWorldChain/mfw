package onight.tfw.oapi.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import onight.tfw.otransio.api.session.ModuleSession;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper=true)
public class Bean1OnA extends BasicA<Bean1> {
	String dd;

	
}
