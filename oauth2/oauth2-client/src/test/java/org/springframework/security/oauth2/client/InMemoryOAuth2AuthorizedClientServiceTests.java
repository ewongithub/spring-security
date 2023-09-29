/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.oauth2.client;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InMemoryOAuth2AuthorizedClientService}.
 *
 * @author Joe Grandja
 * @author Vedran Pavic
 */
public class InMemoryOAuth2AuthorizedClientServiceTests {

	private String principalName1 = "principal-1";

	private String principalName2 = "principal-2";

	private ClientRegistration registration1 = TestClientRegistrations.clientRegistration().build();

	private ClientRegistration registration2 = TestClientRegistrations.clientRegistration2().build();

	private ClientRegistration registration3 = TestClientRegistrations.clientRegistration()
		.clientId("client-3")
		.registrationId("registration-3")
		.build();

	private ClientRegistrationRepository clientRegistrationRepository = new InMemoryClientRegistrationRepository(
			this.registration1, this.registration2, this.registration3);

	private InMemoryOAuth2AuthorizedClientService authorizedClientService = new InMemoryOAuth2AuthorizedClientService(
			this.clientRegistrationRepository);

	@Test
	public void constructorWhenClientRegistrationRepositoryIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new InMemoryOAuth2AuthorizedClientService(null));
	}

	@Test
	public void constructorWhenAuthorizedClientsIsNullThenThrowIllegalArgumentException() {
		// @formatter:off
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new InMemoryOAuth2AuthorizedClientService(this.clientRegistrationRepository, null))
				.withMessage("authorizedClients cannot be empty");
		// @formatter:on
	}

	@Test
	public void constructorWhenAuthorizedClientsProvidedThenUseProvidedAuthorizedClients() {
		String registrationId = this.registration3.getRegistrationId();
		Map<OAuth2AuthorizedClientId, OAuth2AuthorizedClient> authorizedClients = Collections.singletonMap(
				new OAuth2AuthorizedClientId(this.registration3.getRegistrationId(), this.principalName1),
				mock(OAuth2AuthorizedClient.class));
		ClientRegistrationRepository clientRegistrationRepository = mock(ClientRegistrationRepository.class);
		given(clientRegistrationRepository.findByRegistrationId(eq(registrationId))).willReturn(this.registration3);
		InMemoryOAuth2AuthorizedClientService authorizedClientService = new InMemoryOAuth2AuthorizedClientService(
				clientRegistrationRepository, authorizedClients);
		assertThatObject(authorizedClientService.loadAuthorizedClient(registrationId, this.principalName1)).isNotNull();
	}

	@Test
	public void loadAuthorizedClientWhenClientRegistrationIdIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.authorizedClientService.loadAuthorizedClient(null, this.principalName1));
	}

	@Test
	public void loadAuthorizedClientWhenPrincipalNameIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.authorizedClientService.loadAuthorizedClient(this.registration1.getRegistrationId(), null));
	}

	@Test
	public void loadAuthorizedClientWhenClientRegistrationNotFoundThenReturnNull() {
		OAuth2AuthorizedClient authorizedClient = this.authorizedClientService
			.loadAuthorizedClient("registration-not-found", this.principalName1);
		assertThat(authorizedClient).isNull();
	}

	@Test
	public void loadAuthorizedClientWhenClientRegistrationFoundButNotAssociatedToPrincipalThenReturnNull() {
		OAuth2AuthorizedClient authorizedClient = this.authorizedClientService
			.loadAuthorizedClient(this.registration1.getRegistrationId(), "principal-not-found");
		assertThat(authorizedClient).isNull();
	}

	@Test
	public void loadAuthorizedClientWhenClientRegistrationFoundAndAssociatedToPrincipalThenReturnAuthorizedClient() {
		Authentication authentication = mock(Authentication.class);
		given(authentication.getName()).willReturn(this.principalName1);
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(this.registration1, this.principalName1,
				mock(OAuth2AccessToken.class));
		this.authorizedClientService.saveAuthorizedClient(authorizedClient, authentication);
		OAuth2AuthorizedClient loadedAuthorizedClient = this.authorizedClientService
			.loadAuthorizedClient(this.registration1.getRegistrationId(), this.principalName1);
		assertThat(loadedAuthorizedClient).isEqualTo(authorizedClient);
	}

	@Test
	public void saveAuthorizedClientWhenAuthorizedClientIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.authorizedClientService.saveAuthorizedClient(null, mock(Authentication.class)));
	}

	@Test
	public void saveAuthorizedClientWhenPrincipalIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.authorizedClientService.saveAuthorizedClient(mock(OAuth2AuthorizedClient.class), null));
	}

	@Test
	public void saveAuthorizedClientWhenSavedThenCanLoad() {
		Authentication authentication = mock(Authentication.class);
		given(authentication.getName()).willReturn(this.principalName2);
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(this.registration3, this.principalName2,
				mock(OAuth2AccessToken.class));
		this.authorizedClientService.saveAuthorizedClient(authorizedClient, authentication);
		OAuth2AuthorizedClient loadedAuthorizedClient = this.authorizedClientService
			.loadAuthorizedClient(this.registration3.getRegistrationId(), this.principalName2);
		assertThat(loadedAuthorizedClient).isEqualTo(authorizedClient);
	}

	@Test
	public void removeAuthorizedClientWhenClientRegistrationIdIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.authorizedClientService.removeAuthorizedClient(null, this.principalName2));
	}

	@Test
	public void removeAuthorizedClientWhenPrincipalNameIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.authorizedClientService
			.removeAuthorizedClient(this.registration3.getRegistrationId(), null));
	}

	@Test
	public void removeAuthorizedClientWhenSavedThenRemoved() {
		Authentication authentication = mock(Authentication.class);
		given(authentication.getName()).willReturn(this.principalName2);
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(this.registration2, this.principalName2,
				mock(OAuth2AccessToken.class));
		this.authorizedClientService.saveAuthorizedClient(authorizedClient, authentication);
		OAuth2AuthorizedClient loadedAuthorizedClient = this.authorizedClientService
			.loadAuthorizedClient(this.registration2.getRegistrationId(), this.principalName2);
		assertThat(loadedAuthorizedClient).isNotNull();
		this.authorizedClientService.removeAuthorizedClient(this.registration2.getRegistrationId(),
				this.principalName2);
		loadedAuthorizedClient = this.authorizedClientService
			.loadAuthorizedClient(this.registration2.getRegistrationId(), this.principalName2);
		assertThat(loadedAuthorizedClient).isNull();
	}

}
