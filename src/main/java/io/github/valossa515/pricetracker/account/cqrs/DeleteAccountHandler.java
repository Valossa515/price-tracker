package io.github.valossa515.pricetracker.account.cqrs;

import io.github.valossa515.pricetracker.account.CognitoUserDirectory;
import io.github.valossa515.pricetracker.alert.AlertRepository;
import io.github.valossa515.pricetracker.consent.UserConsentRepository;
import io.github.valossa515.spring_courier.core.interfaces.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteAccountHandler implements CommandHandler<DeleteAccountCommand, Void> {

    private final AlertRepository alerts;
    private final UserConsentRepository consents;
    private final CognitoUserDirectory cognito;

    @Override
    @Transactional
    public Void handle(DeleteAccountCommand cmd) {
        String userId = cmd.userId();
        log.info("Deleting account for user {}", userId);

        int alertsDeleted = alerts.deleteByUserId(userId);
        int consentsDeleted = consents.deleteByUserId(userId);
        log.info("User {} - removed {} alerts and {} consents", userId, alertsDeleted, consentsDeleted);

        cognito.deleteUserBySub(userId);
        return null;
    }
}
