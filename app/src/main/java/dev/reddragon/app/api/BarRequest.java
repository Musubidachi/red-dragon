package dev.reddragon.app.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarRequest {
    private LocalDate date;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
}
