package dev.reddragon.app.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * One daily bar inside a backtest frame request.
 */
@Data
public class BacktestBarRequest {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
}
