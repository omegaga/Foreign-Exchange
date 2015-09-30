## Labeling Method:

In this program, data is aggregated into time slots. Length of time slot can be
configured. For each time slot, the following values are recorded: max, min,
open and close price of bid and ask. If there is no data in a time slot, take
the value of previous time slot for smoothing.

This program uses directionality as label. Given a time slot, the value
of label is 1 when close price of this time slot is higher than its precedent
time slot, and 0 if lower.

This program uses a sliding window fashion for features. It takes data of n
time slots prior to the given label time slot as features.
