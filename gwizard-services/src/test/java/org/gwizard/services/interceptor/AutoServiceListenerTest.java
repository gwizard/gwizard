package org.gwizard.services.interceptor;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import org.gwizard.services.Services;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class AutoServiceListenerTest {

	@Mock
	TypeEncounter typeEncounter;

	@Mock
	Provider<Services> servicesProvider;

	AutoServiceListener autoServiceListener;

	@BeforeClass
	public void setUp() {
		initMocks(this);
	}

	@BeforeMethod
	public void resetMock() {
		reset(typeEncounter);
		autoServiceListener = new AutoServiceListener(servicesProvider);
	}

	@Test
	public void shouldMatchService() {
		// given
		TypeLiteral typeLiteral = TypeLiteral.get(AutoServiceTest.ExampleService.class);

		// when
		autoServiceListener.hear(typeLiteral, typeEncounter);

		// then
		verify(typeEncounter, times(1)).register(any(InjectionListener.class));
	}

	@Test
	public void shouldMatchServiceListener() {
		// given
		TypeLiteral typeLiteral = TypeLiteral.get(AutoServiceTest.ExampleServiceListener.class);

		// when
		autoServiceListener.hear(typeLiteral, typeEncounter);

		// then
		verify(typeEncounter, times(1)).register(any(InjectionListener.class));
	}

	@Test
	public void shouldMatchServiceManagerListener() {
		// given
		TypeLiteral typeLiteral = TypeLiteral.get(AutoServiceTest.ExampleServiceManagerListener.class);

		// when
		autoServiceListener.hear(typeLiteral, typeEncounter);

		// then
		verify(typeEncounter, times(1)).register(any(InjectionListener.class));
	}
}