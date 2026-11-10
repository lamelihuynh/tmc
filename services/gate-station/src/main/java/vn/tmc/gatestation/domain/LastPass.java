package vn.tmc.gatestation.domain;

import java.time.Instant;

/**
 * Direction and time of the last confirmed pass of an employee at this factory (anti-passback
 * state).
 */
public record LastPass(Direction direction, Instant at) {}
