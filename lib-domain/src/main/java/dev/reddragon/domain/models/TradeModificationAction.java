package dev.reddragon.domain.models;

/**
 * Advisory actions that can be requested when live context changes.
 *
 * These actions are not orders. They are safe recommendations for an execution
 * or portfolio layer to review before modifying a live trade.
 */
public enum TradeModificationAction {
    HOLD,
    REDUCE_EXPOSURE,
    TIGHTEN_STOP,
    MOVE_TO_PROBE_SIZE,
    EXIT_REVIEW,
    INCREASE_MONITORING,
    BLOCK_CONCENTRATION,
    ESCALATE_FOR_HUMAN_REVIEW
}
