package org.apereo.cas.adaptors.gauth.repository.credentials;

import lombok.val;

import com.warrenstrange.googleauth.IGoogleAuthenticator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.CipherExecutor;
import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.configuration.model.support.mfa.GAuthMultifactorProperties;
import org.apereo.cas.otp.repository.credentials.BaseOneTimeTokenCredentialRepository;
import org.apereo.cas.util.CollectionUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is {@link RestGoogleAuthenticatorTokenCredentialRepository}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Slf4j
@Getter
public class RestGoogleAuthenticatorTokenCredentialRepository extends BaseOneTimeTokenCredentialRepository {

    private final IGoogleAuthenticator googleAuthenticator;
    private final transient RestTemplate restTemplate;
    private final GAuthMultifactorProperties gauth;

    public RestGoogleAuthenticatorTokenCredentialRepository(final IGoogleAuthenticator googleAuthenticator, final RestTemplate restTemplate,
                                                            final GAuthMultifactorProperties gauth, final CipherExecutor<String, String> tokenCredentialCipher) {
        super(tokenCredentialCipher);
        this.googleAuthenticator = googleAuthenticator;
        this.restTemplate = restTemplate;
        this.gauth = gauth;
    }

    @Override
    public OneTimeTokenAccount get(final String username) {
        val rest = gauth.getRest();
        val headers = new HttpHeaders();
        headers.setAccept(CollectionUtils.wrap(MediaType.APPLICATION_JSON));
        headers.put("username", CollectionUtils.wrap(username));

        val entity = new HttpEntity<>(headers);
        val result = restTemplate.exchange(rest.getEndpointUrl(), HttpMethod.GET, entity, OneTimeTokenAccount.class);
        if (result.getStatusCodeValue() == HttpStatus.OK.value()) {
            return decode(result.getBody());
        }
        return null;
    }

    @Override
    public void save(final String userName, final String secretKey, final int validationCode, final List<Integer> scratchCodes) {
        val account = new GoogleAuthenticatorAccount(userName, secretKey, validationCode, scratchCodes);
        update(account);
    }

    @Override
    public OneTimeTokenAccount create(final String username) {
        val key = this.googleAuthenticator.createCredentials();
        return new GoogleAuthenticatorAccount(username, key.getKey(), key.getVerificationCode(), key.getScratchCodes());
    }

    @Override
    public void deleteAll() {
        val rest = gauth.getRest();
        restTemplate.delete(rest.getEndpointUrl());
    }

    @Override
    public OneTimeTokenAccount update(final OneTimeTokenAccount accountToUpdate) {
        val rest = gauth.getRest();
        val account = encode(accountToUpdate);

        val headers = new HttpHeaders();
        headers.setAccept(CollectionUtils.wrap(MediaType.APPLICATION_JSON));
        headers.put("username", CollectionUtils.wrap(account.getUsername()));
        headers.put("validationCode", CollectionUtils.wrap(String.valueOf(account.getValidationCode())));
        headers.put("secretKey", CollectionUtils.wrap(account.getSecretKey()));
        headers.put("scratchCodes", account.getScratchCodes().stream().map(String::valueOf).collect(Collectors.toList()));

        val entity = new HttpEntity<>(headers);
        val result = restTemplate.exchange(rest.getEndpointUrl(), HttpMethod.POST, entity, Object.class);
        if (result.getStatusCodeValue() == HttpStatus.OK.value()) {
            LOGGER.debug("Posted google authenticator account successfully");
            return account;
        }
        LOGGER.warn("Failed to save google authenticator account successfully");
        return null;
    }
}
