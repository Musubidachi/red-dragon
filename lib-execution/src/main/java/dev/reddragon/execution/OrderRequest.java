package dev.reddragon.execution;

import java.math.BigDecimal;

public sealed interface OrderRequest permits EquityOrderRequest, SingleLegOptionOrderRequest {

    String symbol();

    AssetType assetType();

    BigDecimal quantity();

    String clientOrderId();

    String description();
}
