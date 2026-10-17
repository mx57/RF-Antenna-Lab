# Security Specification & Hardened Red Team Audit (RF Antenna Lab)

## 1. Data Invariants
1. **Multi-User Isolation**: Every document in `/antenna_profiles/{profileId}`, `/vna_measurements/{measurementId}`, and `/custom_plugins/{pluginId}` MUST belong to an authenticated user (`request.auth != null`) and have `userId == request.auth.uid`.
2. **Immutable Ownership & Temporal Integrity**: On `create`, `userId == request.auth.uid`, `createdAt is timestamp && createdAt <= request.time`, `updatedAt is timestamp && updatedAt <= request.time`. On `update`, `userId` and `createdAt` are strictly immutable (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
3. **Strict Key & Size Bounds**: Every string field enforces strict `.size()` bounds, numeric fields enforce valid physical ranges, and `hasOnly` / `hasAll` gates block ghost fields.
4. **Query Enforcer**: Every `allow list` rule explicitly evaluates `resource.data.userId == request.auth.uid`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/List**: `auth = null` attempting `get` or `list` on `/antenna_profiles`.
2. **Cross-User Read**: `alice` attempting `get` on `bob`'s `/antenna_profiles/bob_prof`.
3. **Identity Spoofing on Create**: `alice` creating a profile with `userId: "bob_456"`.
4. **Shadow Update (Ghost Field)**: `alice` updating `/antenna_profiles/alice_prof` with `isVerified: true`.
5. **Immutable Field Mutation**: `alice` updating `createdAt` or `userId` on an existing document.
6. **Resource Poisoning (Oversized ID)**: Creating a document with a 500-char ID or invalid regex characters.
7. **Value Poisoning (Oversized String)**: Setting `name` to a 10,000-char string (`> 120` limit).
8. **Type Confusion**: Passing a string `"145.5"` instead of `number` for `frequencyMhz`.
9. **Blanket List Query**: Authenticated user querying `/antenna_profiles` without `.where("userId", "==", uid)`.
10. **Future Timestamp Injection**: Passing a timestamp in the future (`> request.time`).
11. **Cross-User Delete**: `alice` attempting to `delete` `bob`'s VNA measurement.
12. **Plugin Code Injection Overflow**: Creating a custom plugin with `formulaExpression` exceeding 2000 characters.

## 3. Red Team Audit Conflict Report
- `/antenna_profiles/{profileId}`: Protected against Identity Spoofing, Shadow Updates, Value Poisoning, and Unfiltered Queries. `isValidAntennaProfile(incoming())` wraps all writes.
- `/vna_measurements/{measurementId}`: Protected against Identity Spoofing, Shadow Updates, Value Poisoning, and Unfiltered Queries. `isValidVnaMeasurement(incoming())` wraps all writes.
- `/custom_plugins/{pluginId}`: Protected against Identity Spoofing, Shadow Updates, Value Poisoning, and Unfiltered Queries. `isValidCustomPlugin(incoming())` wraps all writes.
