package io.github.valossa515.pricetracker.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

@Configuration
class CognitoClientConfig {

    @Bean
    CognitoIdentityProviderClient cognitoClient(
            @Value("${cognito.region:us-east-1}") String region) {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class CognitoUserDirectory {

    private final CognitoIdentityProviderClient cognito;

    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    /**
     * Deletes a Cognito user by their `sub` claim. Idempotent: if the user does
     * not exist (already deleted), the call is a no-op.
     */
    public void deleteUserBySub(String sub) {
        String username = resolveUsername(sub);
        if (username == null) {
            log.info("Cognito user sub {} not found; skipping delete", sub);
            return;
        }
        try {
            cognito.adminDeleteUser(AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build());
            log.info("Cognito user {} (sub {}) deleted", username, sub);
        } catch (UserNotFoundException e) {
            log.info("Cognito user {} vanished between resolve and delete", username);
        }
    }

    private String resolveUsername(String sub) {
        var response = cognito.listUsers(ListUsersRequest.builder()
                .userPoolId(userPoolId)
                .filter("sub = \"" + sub + "\"")
                .limit(1)
                .build());
        return response.users().stream()
                .findFirst()
                .map(UserType::username)
                .orElse(null);
    }
}
