package io.github.valossa515.pricetracker.account.cqrs;

import io.github.valossa515.pricetracker.account.dto.AccountExportResponse;
import io.github.valossa515.spring_courier.core.interfaces.IQuery;

public record ExportAccountQuery(String userId, String email) implements IQuery<AccountExportResponse> {
}
